import MapKit
import Shared
import SwiftUI

/// La pantalla Detalle (mg-04): la boleta del movimiento, con editar, cambiar nombre, cambiar categoría,
/// confirmar y eliminar con «Deshacer». Los textos y las reglas son de `shared`.
struct DetalleView: View {
    let aplicacion: Aplicacion
    @Environment(\.dismiss) private var cerrar
    @State private var modelo: DetalleModelo
    @State private var editando = false
    @State private var eligiendoCategoria = false
    @State private var cambiandoNombre = false
    @State private var nombreNuevo = ""
    @State private var confirmandoEliminar = false
    @State private var verTexto = false
    @State private var servicioDeUbicacion = ServicioDeUbicacion()
    @State private var capturando = false
    @State private var ubicacionBloqueada = false

    init(id: String, aplicacion: Aplicacion) {
        self.aplicacion = aplicacion
        _modelo = State(initialValue: DetalleModelo(base: aplicacion.base, registro: aplicacion.registro, id: id))
    }

    var body: some View {
        Group {
            if let estado = modelo.estado {
                switch onEnum(of: estado) {
                case .listo(let listo): contenido(listo)
                case .noExiste: Color.clear.onAppear { cerrar() }
                }
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

    @ViewBuilder
    private func contenido(_ listo: EstadoDelDetalleListo) -> some View {
        let d = listo.detalle
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Boleta(detalle: d, oculto: aplicacion.montosOcultos,
                       alCambiarNombre: { nombreNuevo = d.nombreParaEditar; cambiandoNombre = true },
                       alElegirCategoria: { eligiendoCategoria = true })
                if d.porConfirmar { BotonConfirmar(detalle: d) { confirmar(d) } }
                if d.tieneUbicacion {
                    Ubicacion(detalle: d) { modelo.seguimiento.quitarUbicacion() }
                } else {
                    BotonAgregarUbicacion(capturando: capturando) { agregarUbicacion() }
                }
                if let texto = d.textoOriginal { TextoOriginal(texto: texto, abierto: $verTexto) }
                BotonEliminar(detalle: d) { confirmandoEliminar = true }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 24)
        }
        .navigationTitle(d.tipoTexto)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(TextosDelDetalle.shared.EDITAR) { editando = true }
            }
        }
        .alert(TextosDelDetalle.shared.CAMBIAR_NOMBRE, isPresented: $cambiandoNombre) {
            TextField(TextosDelDetalle.shared.NOMBRE, text: $nombreNuevo)
                .autocorrectionDisabled()
            Button(TextosDelDetalle.shared.GUARDAR) { modelo.seguimiento.establecerAlias(comercio: d.nombreOriginal, alias: nombreNuevo) }
            if d.tieneNombrePropio {
                Button(TextosDelDetalle.shared.QUITAR_NOMBRE) { modelo.seguimiento.establecerAlias(comercio: d.nombreOriginal, alias: "") }
            }
            Button(TextosDelDetalle.shared.CANCELAR, role: .cancel) {}
        } message: {
            Text(TextosDelDetalle.shared.explicacionDelNombre(nombreOriginal: d.nombreOriginal))
        }
        .alert(d.textoDeEliminar, isPresented: $confirmandoEliminar) {
            Button(TextosDelDetalle.shared.ELIMINAR, role: .destructive) { eliminar(d) }
            Button(TextosDelDetalle.shared.CANCELAR, role: .cancel) {}
        } message: {
            Text(d.preguntaDeEliminar)
        }
        .alert(TextosDeUbicacion.shared.AGREGAR, isPresented: $ubicacionBloqueada) {
            Button(TextosDeUbicacion.shared.ABRIR_AJUSTES) {
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
            }
            Button(TextosDelDetalle.shared.CANCELAR, role: .cancel) {}
        } message: {
            Text(mensajeDeFallo(motivo: .denegadaParaSiempre))
        }
        .sheet(isPresented: $editando) { HojaDeEdicionView(detalle: d, seguimiento: modelo.seguimiento) }
        .sheet(isPresented: $eligiendoCategoria) { HojaDeCategoriaView(detalle: d, propias: listo.propias, tipo: listo.tipo, seguimiento: modelo.seguimiento) }
    }

    private func confirmar(_ d: DetalleDelMovimiento) {
        modelo.seguimiento.confirmar()
        aplicacion.avisar(d.avisoDeGuardado)
    }

    private func eliminar(_ d: DetalleDelMovimiento) {
        guard let original = modelo.seguimiento.eliminar() else { return }
        let registro = aplicacion.registro
        aplicacion.avisar(d.avisoDeEliminado, accion: "Deshacer", segundos: 8) { registro.restaurar(movimiento: original) }
        cerrar()
    }
}

private struct Boleta: View {
    let detalle: DetalleDelMovimiento
    let oculto: Bool
    let alCambiarNombre: () -> Void
    let alElegirCategoria: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(detalle.nombre)
                .font(.outfit(14))
                .foregroundStyle(Paleta.apagado)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                if !oculto {
                    Text("S/").font(.outfit(20, .medium, relativeTo: .title3)).foregroundStyle(Paleta.apagado)
                }
                Text(oculto ? "••••" : formatearMonto(monto: detalle.monto, decimales: 2))
                    .font(.outfit(46, .bold, relativeTo: .largeTitle))
                    .foregroundStyle(detalle.esIngreso ? Paleta.ingreso : Paleta.tinta)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
            }
            .padding(.top, 4)
            if detalle.porConfirmar {
                Text(TextosDelDetalle.shared.POR_CONFIRMAR)
                    .font(.outfit(11, .semibold, relativeTo: .caption2))
                    .foregroundStyle(Paleta.ingreso)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 2)
                    .background(Paleta.ingresoSuave, in: Capsule())
                    .padding(.top, 8)
            }
            LineaDeCorte().padding(.vertical, 10)
            Fila(etiqueta: TextosDelDetalle.shared.FECHA) { Valor(detalle.fecha) }
            Fila(etiqueta: detalle.etiquetaDelNombre, alTocar: alCambiarNombre) { Valor(detalle.nombre); Chevron() }
            Fila(etiqueta: detalle.etiquetaDeLaFuente) {
                Circle().fill(colorDeLaFuente(detalle.fuente)).frame(width: 8, height: 8)
                Valor(detalle.fuenteTexto)
            }
            Fila(etiqueta: TextosDelDetalle.shared.CATEGORIA, alTocar: alElegirCategoria) {
                Image(systemName: iconoDeCategoria(detalle.categoria, propio: detalle.iconoPropio)).font(.system(size: 14)).foregroundStyle(Paleta.tinta)
                Valor(detalle.categoriaEtiqueta)
                Chevron()
            }
            Fila(etiqueta: TextosDelDetalle.shared.REGISTRADO) { Valor(detalle.registrado) }
            if let nota = detalle.nota { Fila(etiqueta: TextosDelDetalle.shared.NOTA) { Valor(nota) } }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct LineaDeCorte: View {
    var body: some View {
        GeometryReader { geometria in
            Path { camino in
                camino.move(to: .zero)
                camino.addLine(to: CGPoint(x: geometria.size.width, y: 0))
            }
            .stroke(Paleta.linea, style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
        }
        .frame(height: 1)
    }
}

private struct Fila<Contenido: View>: View {
    let etiqueta: String
    var alTocar: (() -> Void)?
    @ViewBuilder let contenido: () -> Contenido

    var body: some View {
        let fila = HStack(spacing: 6) {
            Text(etiqueta).font(.outfit(14)).foregroundStyle(Paleta.apagado)
            Spacer(minLength: 12)
            contenido()
        }
        .padding(.vertical, 10)
        .contentShape(Rectangle())
        if let alTocar {
            Button(action: alTocar) { fila }.buttonStyle(.plain)
        } else {
            fila
        }
    }
}

private struct Valor: View {
    let texto: String
    init(_ texto: String) { self.texto = texto }

    var body: some View {
        Text(texto)
            .font(.outfit(13.5, .semibold))
            .foregroundStyle(Paleta.tinta)
            .multilineTextAlignment(.trailing)
    }
}

private struct Chevron: View {
    var body: some View {
        Image(systemName: "chevron.right").font(.system(size: 12, weight: .semibold)).foregroundStyle(Paleta.apagado)
    }
}

private struct BotonConfirmar: View {
    let detalle: DetalleDelMovimiento
    let alConfirmar: () -> Void

    var body: some View {
        Button(action: alConfirmar) {
            Text(detalle.textoDeConfirmar)
                .font(.outfit(16.5, .semibold))
                .foregroundStyle(detalle.esIngreso ? Paleta.fondo : Paleta.tintaSobreVerde)
                .frame(maxWidth: .infinity, minHeight: 54)
                .background(detalle.esIngreso ? Paleta.ingreso : Paleta.verdeNeon, in: RoundedRectangle(cornerRadius: 15))
        }
        .buttonStyle(.plain)
    }
}

private struct Ubicacion: View {
    let detalle: DetalleDelMovimiento
    let alQuitar: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let latitud = detalle.latitud?.doubleValue, let longitud = detalle.longitud?.doubleValue {
                let centro = CLLocationCoordinate2D(latitude: latitud, longitude: longitud)
                Map(initialPosition: .region(MKCoordinateRegion(center: centro, latitudinalMeters: 400, longitudinalMeters: 400)), interactionModes: []) {
                    Marker(detalle.direccion ?? "", coordinate: centro).tint(Paleta.presupuestoPasado)
                }
                .frame(height: 120)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .accessibilityHidden(true)
            }
            HStack(spacing: 12) {
                Image(systemName: "mappin.and.ellipse").foregroundStyle(Paleta.apagado)
                VStack(alignment: .leading, spacing: 2) {
                    Text(detalle.direccion ?? "Ubicación guardada").font(.outfit(14.5, .semibold)).foregroundStyle(Paleta.tinta)
                    if let texto = detalle.textoDeLaUbicacion {
                        Text(texto).font(.outfit(12.5, relativeTo: .caption)).foregroundStyle(Paleta.apagado)
                    }
                }
                Spacer()
            }
            .padding(14)
            .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 14))
            Button(TextosDelDetalle.shared.quitarUbicacion(tipoTexto: detalle.tipoTexto), action: alQuitar)
                .font(.outfit(13.5, .semibold))
                .tint(Paleta.marcaTinta)
                .padding(.top, 4)
        }
    }
}

private struct TextoOriginal: View {
    let texto: String
    @Binding var abierto: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button { withAnimation { abierto.toggle() } } label: {
                HStack {
                    Text(TextosDelDetalle.shared.TEXTO_ORIGINAL).font(.outfit(14.5, .semibold)).foregroundStyle(Paleta.tinta)
                    Spacer()
                    Image(systemName: abierto ? "chevron.up" : "chevron.down").foregroundStyle(Paleta.apagado)
                }
                .padding(.vertical, 12)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if abierto {
                Text(texto).font(.outfit(12.5, relativeTo: .caption)).foregroundStyle(Paleta.apagado)
            }
        }
    }
}

private struct BotonEliminar: View {
    let detalle: DetalleDelMovimiento
    let alEliminar: () -> Void

    var body: some View {
        Button(action: alEliminar) {
            Label(detalle.textoDeEliminar, systemImage: "trash")
                .font(.outfit(14.5, .semibold))
                .foregroundStyle(Paleta.presupuestoPasado)
                .frame(maxWidth: .infinity, minHeight: 48)
        }
        .buttonStyle(.plain)
    }
}

extension DetalleView {
    /// Pide el permiso si falta, toma la ubicación una vez y la guarda; si falla, dice por qué (o ofrece abrir los ajustes).
    fileprivate func agregarUbicacion() {
        guard !capturando else { return }
        capturando = true
        Task {
            let resultado = await servicioDeUbicacion.capturar()
            capturando = false
            switch resultado {
            case .ok(let ubicacion):
                modelo.seguimiento.ponerUbicacion(ubicacion: ubicacion)
            case .fallo(let motivo):
                if motivo.ofreceAbrirAjustes { ubicacionBloqueada = true } else { aplicacion.avisar(mensajeDeFallo(motivo: motivo), segundos: 4) }
            }
        }
    }
}

private struct BotonAgregarUbicacion: View {
    let capturando: Bool
    let alTocar: () -> Void

    var body: some View {
        Button(action: alTocar) {
            HStack(spacing: 12) {
                Image(systemName: "mappin.and.ellipse").foregroundStyle(Paleta.marcaTinta)
                VStack(alignment: .leading, spacing: 2) {
                    Text(TextosDeUbicacion.shared.AGREGAR).font(.outfit(15, .semibold)).foregroundStyle(Paleta.tinta)
                    Text(TextosDeUbicacion.shared.AGREGAR_DETALLE).font(.outfit(12.5, relativeTo: .caption)).foregroundStyle(Paleta.apagado)
                }
                Spacer()
                if capturando { ProgressView() }
            }
            .padding(14)
            .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 14))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(capturando)
        .accessibilityLabel("\(TextosDeUbicacion.shared.AGREGAR). \(TextosDeUbicacion.shared.AGREGAR_DETALLE)")
    }
}
