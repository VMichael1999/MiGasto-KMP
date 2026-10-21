import Observation
import Shared

/// Lo que vive mientras la app está abierta: la base cifrada y los casos de uso de `shared`.
@Observable
final class Aplicacion {
    let base: BaseMiGasto
    let registro: RegistroDeMovimientos

    /// Si el usuario ocultó los montos (el ojo del Resumen). Vale para todas las pantallas.
    var montosOcultos = false

    init() {
        base = abrirLaBaseDeLaApp()
        registro = crearRegistroDeMovimientos(base: base)
    }
}
