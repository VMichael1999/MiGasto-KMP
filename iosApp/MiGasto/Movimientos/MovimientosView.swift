import Shared
import SwiftUI

/// La pantalla Movimientos (mg-02): filtro por tipo, búsqueda, días agrupados y la hoja de filtros.
/// Solo dibuja: los grupos, los totales y los textos los arma `shared`.
struct MovimientosView: View {
    @Environment(Aplicacion.self) private var aplicacion
    @State private var modelo = MovimientosModelo()
    @State private var hojaAbierta = false

    var body: some View {
        NavigationStack {
            ScrollView {
                if let p = modelo.pantalla {
                    VStack(alignment: .leading, spacing: 16) {
                        TipoDeMovimientos(pantalla: p) { modelo.filtros = modelo.filtros.conTipo(nuevo: $0) }
                        if p.grupos.isEmpty {
                            SinMovimientos(pantalla: p) { modelo.filtros = filtrosVacios() }
                        } else {
                            ForEach(p.grupos, id: \.etiqueta) { grupo in
                                GrupoView(grupo: grupo, oculto: aplicacion.montosOcultos)
                            }
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                }
            }
            .background(Paleta.fondo)
            .navigationTitle(TextosDeMovimientos.shared.TITULO)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { hojaAbierta = true } label: {
                        Image(systemName: "line.3.horizontal.decrease")
                    }
                    .overlay(alignment: .topTrailing) { Insignia(cantidad: Int(modelo.filtros.cantidadDeFiltrosActivos)) }
                    .accessibilityLabel(TextosDeMovimientos.shared.FILTRAR)
                }
            }
            .navigationDestination(for: String.self) { DetalleView(id: $0, aplicacion: aplicacion) }
            .sheet(isPresented: $hojaAbierta) {
                if let p = modelo.pantalla {
                    HojaDeFiltrosView(pantalla: p) { modelo.filtros = $0 }
                }
            }
        }
        .task(id: modelo.claveDeLosFiltros) { await modelo.observar(aplicacion.base) }
    }
}

private struct Insignia: View {
    let cantidad: Int

    var body: some View {
        if cantidad > 0 {
            Text("\(cantidad)")
                .font(.outfit(10, .bold, relativeTo: .caption2))
                .foregroundStyle(Paleta.tintaSobreVerde)
                .frame(width: 16, height: 16)
                .background(Paleta.verdeNeon, in: Circle())
                .offset(x: 4, y: -4)
                .accessibilityLabel(TextosDeMovimientos.shared.filtrosPuestos(cantidad: Int32(cantidad)))
        }
    }
}

/// El filtro por tipo: un control segmentado propio, porque «Por confirmar · 2» no cabe sin achicarse.
struct TipoDeMovimientos: View {
    let pantalla: PantallaDeMovimientos
    let alElegir: (FiltroDeTipo) -> Void

    var body: some View {
        HStack(spacing: 2) {
            ForEach(pantalla.opcionesDeTipo, id: \.tipo) { opcion in
                let elegido = pantalla.tipo == opcion.tipo
                Button { alElegir(opcion.tipo) } label: {
                    Text(opcion.texto)
                        .font(.outfit(13.5, .medium))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                        .foregroundStyle(opcion.esPorConfirmar && !elegido ? Paleta.ingreso : Paleta.tinta)
                        .padding(.horizontal, 4)
                        .frame(maxWidth: .infinity, minHeight: 34)
                        .background(elegido ? Paleta.superficie : Color.clear, in: Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(elegido ? .isSelected : [])
            }
        }
        .padding(3)
        .background(Paleta.linea.opacity(0.7), in: Capsule())
    }
}

struct GrupoView: View {
    let grupo: GrupoDeMovimientos
    let oculto: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(grupo.etiqueta.uppercased())
                    .font(.outfit(12.5, .semibold, relativeTo: .caption))
                    .foregroundStyle(Paleta.apagado)
                Spacer()
                if let total = grupo.totalDeGastos {
                    Text(oculto ? "••••" : total)
                        .font(.outfit(12.5, relativeTo: .caption))
                        .foregroundStyle(Paleta.apagado)
                }
            }
            .padding(.horizontal, 4)
            VStack(spacing: 0) {
                ForEach(Array(grupo.filas.enumerated()), id: \.element.id) { indice, fila in
                    if indice > 0 { Divider().overlay(Paleta.linea) }
                    NavigationLink(value: fila.id) { FilaDeMovimientoView(fila: fila, oculto: oculto) }
                        .buttonStyle(.plain)
                }
            }
            .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 16))
        }
    }
}

private struct SinMovimientos: View {
    let pantalla: PantallaDeMovimientos
    let alQuitarFiltros: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Text(pantalla.textoSinMovimientos)
                .font(.outfit(16))
                .foregroundStyle(Paleta.apagado)
                .multilineTextAlignment(.center)
            if pantalla.hayFiltro {
                Button(TextosDeMovimientos.shared.QUITAR_FILTROS, action: alQuitarFiltros)
                    .font(.outfit(13.5, .semibold))
                    .tint(Paleta.marcaTinta)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }
}
