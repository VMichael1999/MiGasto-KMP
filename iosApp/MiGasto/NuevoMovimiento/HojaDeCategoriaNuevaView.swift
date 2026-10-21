import Shared
import SwiftUI

/// «Nueva categoría»: un nombre (hasta 20 letras) y un ícono. La lista de íconos y el límite del nombre vienen de `shared`.
struct HojaDeCategoriaNuevaView: View {
    let alCrear: (String, String) -> Void
    @Environment(\.dismiss) private var cerrar
    @State private var nombre = ""
    @State private var icono = clavesDeIconosPropios.first ?? ""
    private let columnas = Array(repeating: GridItem(.flexible(), spacing: 8), count: 6)

    private var puedeCrear: Bool { !nombre.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(TextosDeNuevoMovimiento.shared.NUEVA_CATEGORIA)
                .font(.outfit(18, .semibold, relativeTo: .title3))
                .foregroundStyle(Paleta.tinta)
            TextField(TextosDeNuevoMovimiento.shared.NOMBRE, text: Binding(get: { nombre }, set: { nombre = limitarNombreDeCategoria(nombre: $0) }))
                .font(.outfit(16))
                .autocorrectionDisabled()
                .textInputAutocapitalization(.sentences)
                .padding(.horizontal, 14)
                .frame(minHeight: 50)
                .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 13))
            ScrollView {
                LazyVGrid(columns: columnas, spacing: 8) {
                    ForEach(clavesDeIconosPropios, id: \.self) { clave in
                        Button { icono = clave } label: {
                            Image(systemName: simboloDeIconoPropio(clave))
                                .font(.system(size: 20))
                                .foregroundStyle(icono == clave ? Paleta.marcaTinta : Paleta.tinta)
                                .frame(maxWidth: .infinity, minHeight: 46)
                                .background(icono == clave ? Paleta.marcaTinta.opacity(0.14) : Paleta.superficie, in: RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(icono == clave ? Paleta.marcaTinta : Color.clear, lineWidth: 1.5))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(clave)
                    }
                }
            }
            Button {
                alCrear(nombre, icono)
                cerrar()
            } label: {
                Text(TextosDeNuevoMovimiento.shared.CREAR_CATEGORIA)
                    .font(.outfit(16.5, .semibold))
                    .foregroundStyle(puedeCrear ? Paleta.tintaSobreVerde : Paleta.apagado)
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(puedeCrear ? Paleta.verdeNeon : Paleta.linea, in: RoundedRectangle(cornerRadius: 15))
            }
            .buttonStyle(.plain)
            .disabled(!puedeCrear)
        }
        .padding(18)
        .background(Paleta.fondo)
        .presentationDetents([.large])
    }
}
