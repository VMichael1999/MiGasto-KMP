import Shared
import SwiftUI

/// «Editar gasto» / «Editar ingreso»: monto, comercio y nota. `shared` valida; si no sirve, no guarda y avisa.
struct HojaDeEdicionView: View {
    let detalle: DetalleDelMovimiento
    let seguimiento: SeguimientoDelDetalle
    @Environment(\.dismiss) private var cerrar
    @State private var monto: String
    @State private var comercio: String
    @State private var nota: String
    @State private var error = false

    init(detalle: DetalleDelMovimiento, seguimiento: SeguimientoDelDetalle) {
        self.detalle = detalle
        self.seguimiento = seguimiento
        _monto = State(initialValue: detalle.montoParaEditar)
        _comercio = State(initialValue: detalle.nombreOriginal)
        _nota = State(initialValue: detalle.nota ?? "")
    }

    var body: some View {
        let textos = TextosDelDetalle.shared
        VStack(alignment: .leading, spacing: 10) {
            Text(textos.tituloDeEdicion(tipoTexto: detalle.tipoTexto))
                .font(.outfit(18, .semibold, relativeTo: .title3))
                .foregroundStyle(Paleta.tinta)
            Campo(titulo: textos.MONTO, prefijo: "S/", texto: $monto, error: error, teclado: .decimalPad) { error = false }
            Campo(titulo: textos.etiquetaDelComercio(esIngreso: detalle.esIngreso), texto: $comercio, error: false) { error = false }
            Campo(titulo: textos.NOTA_OPCIONAL, texto: $nota, error: false) {}
            if error {
                Text(textos.ERROR_DE_EDICION).font(.outfit(12.5, relativeTo: .caption)).foregroundStyle(Paleta.presupuestoPasado)
            }
            Button {
                if seguimiento.editar(monto: monto, comercio: comercio, notas: nota) { cerrar() } else { error = true }
            } label: {
                Text(textos.GUARDAR_CAMBIOS)
                    .font(.outfit(16.5, .semibold))
                    .foregroundStyle(Paleta.tintaSobreVerde)
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(Paleta.verdeNeon, in: RoundedRectangle(cornerRadius: 15))
            }
            .buttonStyle(.plain)
            .padding(.top, 6)
            Spacer()
        }
        .padding(18)
        .background(Paleta.fondo)
        .presentationDetents([.medium, .large])
    }
}

private struct Campo: View {
    let titulo: String
    var prefijo: String?
    @Binding var texto: String
    let error: Bool
    var teclado: UIKeyboardType = .default
    let alCambiar: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(titulo).font(.outfit(12.5, .semibold, relativeTo: .caption)).foregroundStyle(Paleta.apagado)
            HStack(spacing: 6) {
                if let prefijo { Text(prefijo).font(.outfit(16)).foregroundStyle(Paleta.apagado) }
                TextField(titulo, text: $texto)
                    .font(.outfit(16))
                    .keyboardType(teclado)
                    .autocorrectionDisabled()
                    .onChange(of: texto) { alCambiar() }
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 48)
            .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 13))
            .overlay(RoundedRectangle(cornerRadius: 13).stroke(error ? Paleta.presupuestoPasado : Color.clear, lineWidth: 1.5))
        }
    }
}
