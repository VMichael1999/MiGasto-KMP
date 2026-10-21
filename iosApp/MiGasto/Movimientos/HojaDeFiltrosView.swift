import Shared
import SwiftUI

/// La hoja «Filtrar movimientos»: categoría, fuente y rango de monto. Las opciones y sus textos las arma `shared`.
struct HojaDeFiltrosView: View {
    let pantalla: PantallaDeMovimientos
    let alAplicar: (FiltrosDeMovimientos) -> Void
    @Environment(\.dismiss) private var cerrar

    @State private var categoria: String?
    @State private var fuente: PaymentSource?
    @State private var desde: Double
    @State private var hasta: Double
    @State private var rangoTocado: Bool

    init(pantalla: PantallaDeMovimientos, alAplicar: @escaping (FiltrosDeMovimientos) -> Void) {
        self.pantalla = pantalla
        self.alAplicar = alAplicar
        let f = pantalla.filtros
        _categoria = State(initialValue: f.categoria)
        _fuente = State(initialValue: f.fuente)
        _desde = State(initialValue: f.desdeDeLaHoja)
        _hasta = State(initialValue: f.hastaDeLaHoja)
        _rangoTocado = State(initialValue: f.hayRangoDeLaHoja)
    }

    var body: some View {
        let textos = TextosDeMovimientos.shared
        let hoja = pantalla.hoja
        ScrollView {
            VStack(alignment: .leading, spacing: 4) {
                Text(textos.FILTRAR).font(.outfit(20, .semibold, relativeTo: .title3)).foregroundStyle(Paleta.tinta)
                Seccion(texto: textos.CATEGORIA)
                FlowLayout {
                    ChipDeFiltro(texto: textos.TODAS, elegido: categoria == nil) { categoria = nil }
                    ForEach(hoja.categorias, id: \.clave) { opcion in
                        ChipDeFiltro(texto: opcion.etiqueta, elegido: categoria == opcion.clave) { categoria = opcion.clave }
                    }
                }
                Seccion(texto: textos.FUENTE)
                FlowLayout {
                    ChipDeFiltro(texto: textos.TODAS, elegido: fuente == nil) { fuente = nil }
                    ForEach(hoja.fuentes, id: \.etiqueta) { opcion in
                        ChipDeFiltro(texto: opcion.etiqueta, elegido: fuente == opcion.fuente) { fuente = opcion.fuente }
                    }
                }
                HStack {
                    Seccion(texto: textos.RANGO)
                    Spacer()
                    Text(textoDelRangoDeLaHoja(desde: desde, hasta: hasta))
                        .font(.outfit(12.5, relativeTo: .caption).monospacedDigit())
                        .foregroundStyle(Paleta.apagado)
                }
                Slider(value: bindingDelRango($desde, minimo: hoja.montoMinimo, maximo: { hasta - hoja.paso }), in: hoja.montoMinimo...hoja.montoMaximo, step: hoja.paso)
                    .accessibilityLabel("Monto mínimo")
                Slider(value: bindingDelRango($hasta, minimo: hoja.montoMinimo, maximo: { hoja.montoMaximo }, piso: { desde + hoja.paso }), in: hoja.montoMinimo...hoja.montoMaximo, step: hoja.paso)
                    .accessibilityLabel("Monto máximo")
                Button {
                    alAplicar(armarLosFiltros())
                    cerrar()
                } label: {
                    Text(textos.APLICAR)
                        .font(.outfit(16.5, .semibold))
                        .foregroundStyle(Paleta.tintaSobreVerde)
                        .frame(maxWidth: .infinity, minHeight: 54)
                        .background(Paleta.verdeNeon, in: RoundedRectangle(cornerRadius: 15))
                }
                .padding(.top, 12)
                Button(textos.LIMPIAR_FILTROS) {
                    alAplicar(pantalla.filtros.sinLoDeLaHoja())
                    cerrar()
                }
                .font(.outfit(14, .medium))
                .tint(Paleta.marcaTinta)
                .frame(maxWidth: .infinity)
                .padding(.top, 4)
            }
            .padding(.horizontal, 18)
            .padding(.top, 20)
            .padding(.bottom, 18)
        }
        .background(Paleta.fondo)
        .presentationDetents([.large])
    }

    private func armarLosFiltros() -> FiltrosDeMovimientos {
        pantalla.filtros.conLaHoja(
            categoria: categoria,
            fuente: fuente,
            desde: rangoTocado ? KotlinDouble(double: desde) : nil,
            hasta: rangoTocado ? KotlinDouble(double: hasta) : nil
        )
    }

    /// Un deslizador de los dos extremos: no deja que se crucen y marca el rango como tocado.
    private func bindingDelRango(_ valor: Binding<Double>, minimo: Double, maximo: @escaping () -> Double, piso: @escaping () -> Double = { 0 }) -> Binding<Double> {
        Binding(
            get: { valor.wrappedValue },
            set: { nuevo in
                valor.wrappedValue = min(max(nuevo, max(minimo, piso())), maximo())
                rangoTocado = true
            }
        )
    }
}

private struct Seccion: View {
    let texto: String

    var body: some View {
        Text(texto)
            .font(.outfit(12.5, .semibold, relativeTo: .caption))
            .foregroundStyle(Paleta.apagado)
            .padding(.top, 16)
            .padding(.bottom, 8)
    }
}

private struct ChipDeFiltro: View {
    let texto: String
    let elegido: Bool
    let alTocar: () -> Void

    var body: some View {
        Button(action: alTocar) {
            Text(texto)
                .font(.outfit(13.5, .medium))
                .foregroundStyle(elegido ? Paleta.fondo : Paleta.tinta)
                .padding(.horizontal, 12)
                .frame(height: 34)
                .background(elegido ? Paleta.tinta : Paleta.superficie, in: Capsule())
                .overlay(Capsule().stroke(elegido ? Paleta.tinta : Paleta.linea, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(elegido ? .isSelected : [])
    }
}
