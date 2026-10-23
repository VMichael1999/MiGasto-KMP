import Shared
import SwiftUI

/// La hoja de la extensión «Compartir»: lo que se leyó de la captura, para confirmarlo antes de guardar. La lectura, el
/// monto, la categoría sugerida y los textos los arma `shared`; aquí solo se dibuja.
struct HojaDeLaCapturaView: View {
    @State var vista: VistaPreviaDeCaptura
    let alCancelar: () -> Void
    let alGuardar: (String) -> Void

    private let verde = Color(red: 0.12, green: 0.48, blue: 0.21)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    Label(TextosDeLaCaptura.shared.LEIDO_EN_EL_TELEFONO, systemImage: "checkmark.circle")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(verde)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(verde.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
                    VStack(spacing: 4) {
                        Text(vista.montoTexto)
                            .font(.system(size: 46, weight: .bold, design: .rounded))
                            .foregroundStyle(vista.esIngreso ? Color.blue : Color.primary)
                            .minimumScaleFactor(0.6)
                        Text(vista.detalle).font(.footnote).foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
                    VStack(spacing: 0) {
                        Menu {
                            ForEach(vista.opcionesDeCategoria, id: \.clave) { opcion in
                                Button { vista = vista.conCategoria(clave: opcion.clave) } label: {
                                    if opcion.elegida { Label(opcion.etiqueta, systemImage: "checkmark") } else { Text(opcion.etiqueta) }
                                }
                            }
                        } label: {
                            fila(TextosDeLaCaptura.shared.CATEGORIA, valor: vista.categoriaEtiqueta, flecha: true)
                        }
                        .buttonStyle(.plain)
                        Divider().padding(.leading, 16)
                        fila(TextosDeLaCaptura.shared.FUENTE, valor: vista.fuenteEtiqueta, flecha: false)
                    }
                    .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 14))
                }
                .padding(18)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle(TextosDeLaCaptura.shared.TITULO)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(TextosDeNuevoMovimiento.shared.CANCELAR, action: alCancelar)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(TextosDeNuevoMovimiento.shared.GUARDAR) { alGuardar(vista.categoriaSugerida.clave) }
                        .fontWeight(.semibold)
                }
            }
        }
        .tint(verde)
    }

    private func fila(_ titulo: String, valor: String, flecha: Bool) -> some View {
        HStack {
            Text(titulo).foregroundStyle(.primary)
            Spacer()
            Text(valor).foregroundStyle(.secondary)
            if flecha { Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary) }
        }
        .padding(.horizontal, 16)
        .frame(minHeight: 48)
        .contentShape(Rectangle())
    }
}

/// Cuando la imagen no tiene la constancia de un pago: se dice y no se guarda nada.
struct SinPagoView: View {
    let alCerrar: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 10) {
                Image(systemName: "questionmark.circle").font(.system(size: 40)).foregroundStyle(.secondary)
                Text(TextosDeLaCaptura.shared.SIN_PAGO).font(.headline).multilineTextAlignment(.center)
                Text(TextosDeLaCaptura.shared.SIN_PAGO_AYUDA).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            .padding(28)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle(TextosDeLaCaptura.shared.TITULO)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(TextosDeNuevoMovimiento.shared.CANCELAR, action: alCerrar) } }
        }
    }
}
