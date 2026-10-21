import Observation
import Shared

/// Estado de «Nuevo movimiento»: el formulario (que vive en `shared`, con todas sus reglas) y las categorías propias.
@MainActor
@Observable
final class NuevoMovimientoModelo {
    var formulario = formularioVacio()
    private(set) var propias: [CategoriaPropia] = []

    func cargarPropias(_ base: BaseMiGasto) {
        propias = base.categoriasPropias.todas()
    }

    /// Crea una categoría propia (o devuelve la que ya tenía ese nombre) y la deja elegida.
    func crearCategoria(nombre: String, icono: String, base: BaseMiGasto) {
        let creada = base.categoriasPropias.agregar(nombre: nombre.trimmingCharacters(in: .whitespaces), icono: icono, tipo: formulario.tipo)
        propias = base.categoriasPropias.todas()
        formulario = formulario.conPropia(propia: creada)
    }

    /// Guarda el movimiento y devuelve el mensaje («Gasto guardado»); `nil` si faltan el monto o el detalle.
    func guardar(_ registro: RegistroDeMovimientos) -> String? {
        guardarNuevoMovimiento(registro: registro, f: formulario)
    }
}
