import Shared
import SwiftUI

/// «Categoría»: cambia la del movimiento y, en un gasto, puede aplicarla a todos los pagos al mismo comercio.
struct HojaDeCategoriaView: View {
    let detalle: DetalleDelMovimiento
    let propias: [CategoriaPropia]
    let tipo: TipoMovimiento
    let seguimiento: SeguimientoDelDetalle
    @Environment(\.dismiss) private var cerrar
    @State private var recordar: Bool
    @State private var creando = false

    init(detalle: DetalleDelMovimiento, propias: [CategoriaPropia], tipo: TipoMovimiento, seguimiento: SeguimientoDelDetalle) {
        self.detalle = detalle
        self.propias = propias
        self.tipo = tipo
        self.seguimiento = seguimiento
        _recordar = State(initialValue: !detalle.esIngreso)
    }

    private func elegida(_ opcion: OpcionDeCategoria) -> Bool {
        if let propia = opcion.propia { return detalle.categoriaPropiaId == propia.id }
        return detalle.categoriaPropiaId == nil && detalle.categoria == opcion.categoria
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text(TextosDelDetalle.shared.CATEGORIA)
                    .font(.outfit(18, .semibold, relativeTo: .title3))
                    .foregroundStyle(Paleta.tinta)
                    .padding(.bottom, 8)
                if !detalle.esIngreso {
                    Toggle(TextosDelDetalle.shared.aplicarATodos(nombre: detalle.nombre), isOn: $recordar)
                        .font(.outfit(14))
                        .tint(Paleta.marcaTinta)
                        .padding(.vertical, 6)
                }
                ForEach(Array(formularioVacio().conTipo(nuevo: tipo).chipsDeCategoria(propias: propias).enumerated()), id: \.offset) { _, opcion in
                    Opcion(icono: iconoDeCategoria(opcion.categoria, propio: opcion.propia?.icono), texto: opcion.etiqueta, elegida: elegida(opcion)) {
                        seguimiento.cambiarCategoria(categoria: opcion.categoria, recordar: recordar, propia: opcion.propia?.id)
                        cerrar()
                    }
                }
                Opcion(icono: "plus", texto: TextosDeNuevoMovimiento.shared.NUEVA_CATEGORIA, elegida: false) { creando = true }
            }
            .padding(18)
        }
        .background(Paleta.fondo)
        .presentationDetents([.medium, .large])
        .sheet(isPresented: $creando) {
            HojaDeCategoriaNuevaView { nombre, icono in
                seguimiento.crearCategoriaYElegirla(nombre: nombre, icono: icono, tipo: tipo, recordar: recordar)
                cerrar()
            }
        }
    }
}

private struct Opcion: View {
    let icono: String
    let texto: String
    let elegida: Bool
    let alTocar: () -> Void

    var body: some View {
        Button(action: alTocar) {
            HStack(spacing: 12) {
                Image(systemName: icono).font(.system(size: 16)).foregroundStyle(elegida ? Paleta.marcaTinta : Paleta.tinta).frame(width: 24)
                Text(texto).font(.outfit(15.5, elegida ? .semibold : .regular)).foregroundStyle(Paleta.tinta)
                Spacer()
                if elegida { Image(systemName: "checkmark").foregroundStyle(Paleta.marcaTinta) }
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(elegida ? .isSelected : [])
    }
}
