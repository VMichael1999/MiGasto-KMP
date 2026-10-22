import MapKit
import Shared
import SwiftUI

/// «Dónde pagaste»: los pagos con ubicación en un mapa y, debajo, agrupados por zona. Las zonas, sus textos y el período
/// los arma `shared` (los mismos que usa Android); aquí solo se dibuja el mapa de Apple y la lista.
struct DondePagasteView: View {
    let aplicacion: Aplicacion
    @State private var modelo: DondePagasteModelo
    @Environment(\.dismiss) private var cerrar

    init(aplicacion: Aplicacion) {
        self.aplicacion = aplicacion
        _modelo = State(initialValue: DondePagasteModelo(base: aplicacion.base))
    }

    var body: some View {
        Group {
            if let p = modelo.pantalla {
                // El mapa ocupa cinco novenos y la lista cuatro, como en Android.
                GeometryReader { medida in
                    VStack(spacing: 0) {
                        Mapa(pantalla: p)
                            .frame(height: medida.size.height * 5 / 9)
                        Lista(pantalla: p, oculto: aplicacion.montosOcultos) { modelo.seguimiento.elegirPeriodo(nuevo: $0) }
                            .frame(height: medida.size.height * 4 / 9)
                    }
                }
                .ignoresSafeArea(edges: .top)
            } else {
                Color.clear
            }
        }
        .background(Paleta.fondo)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .task { await modelo.observar() }
        .onAppear { aplicacion.pantallasEncima += 1 }
        .onDisappear { aplicacion.pantallasEncima -= 1 }
    }
}

private struct Mapa: View {
    let pantalla: PantallaDeDondePagaste
    @State private var camara: MapCameraPosition = .automatic

    var body: some View {
        Map(position: $camara) {
            ForEach(pantalla.filas, id: \.nombre) { fila in
                let centro = CLLocationCoordinate2D(latitude: fila.latitudDelCentro, longitude: fila.longitudDelCentro)
                Annotation(fila.nombre, coordinate: centro) {
                    Text("\(fila.cantidad)")
                        .font(.outfit(14, .bold)).foregroundStyle(Paleta.superficie)
                        .frame(width: 32, height: 32).background(Paleta.tinta, in: Circle())
                        .overlay(Circle().stroke(Paleta.superficie, lineWidth: 2))
                }
            }
        }
        .onChange(of: pantalla.limites?.puntos) { encuadrar() }
        .onChange(of: pantalla.periodo) { encuadrar() }
        .onAppear { encuadrar() }
        .accessibilityHidden(true)
    }

    /// Encuadra todos los pagos (con margen); con un solo punto se acerca a esa zona.
    private func encuadrar() {
        guard let l = pantalla.limites else { return }
        let centro = CLLocationCoordinate2D(latitude: (l.sur + l.norte) / 2, longitude: (l.oeste + l.este) / 2)
        let alto = max((l.norte - l.sur) * 1.5, 0.004)
        let ancho = max((l.este - l.oeste) * 1.5, 0.004)
        camara = .region(MKCoordinateRegion(center: centro, span: MKCoordinateSpan(latitudeDelta: alto, longitudeDelta: ancho)))
    }
}

private struct Lista: View {
    let pantalla: PantallaDeDondePagaste
    let oculto: Bool
    let alElegir: (PeriodoDeUbicaciones) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(pantalla.titulo).font(.outfit(24, .semibold)).foregroundStyle(Paleta.tinta)
            Picker("", selection: Binding(get: { pantalla.periodo }, set: alElegir)) {
                ForEach(pantalla.opcionesDePeriodo, id: \.periodo) { Text($0.texto).tag($0.periodo) }
            }
            .pickerStyle(.segmented)
            if pantalla.filas.isEmpty {
                Text(pantalla.textoVacio).font(.outfit(13.5)).foregroundStyle(Paleta.apagado)
                    .multilineTextAlignment(.center).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(Array(pantalla.filas.enumerated()), id: \.offset) { i, fila in
                            if i > 0 { Rectangle().fill(Paleta.linea).frame(height: 1) }
                            Fila(fila: fila, oculto: oculto)
                        }
                        Text(pantalla.pie).font(.outfit(12.5)).foregroundStyle(Paleta.apagado)
                            .frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 10)
                    }
                }
            }
        }
        .padding(.horizontal, 18).padding(.top, 14)
        .background { UnevenRoundedRectangle(topLeadingRadius: 24, topTrailingRadius: 24).fill(Paleta.superficie).ignoresSafeArea(edges: .bottom) }
    }
}

private struct Fila: View {
    let fila: FilaDeZona
    let oculto: Bool

    var body: some View {
        HStack(spacing: 12) {
            Text("\(fila.cantidad)").font(.outfit(14, .bold)).foregroundStyle(Paleta.superficie)
                .frame(width: 36, height: 36).background(Paleta.tinta, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(fila.nombre).font(.outfit(15, .semibold)).foregroundStyle(Paleta.tinta)
                Text(fila.subtitulo).font(.outfit(13)).foregroundStyle(Paleta.apagado)
            }
            Spacer()
            Text(oculto ? "••••" : fila.totalTexto).font(.outfit(15, .semibold)).foregroundStyle(Paleta.tinta)
        }
        .padding(.vertical, 10)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(fila.descripcion(ocultarMontos: oculto))
    }
}
