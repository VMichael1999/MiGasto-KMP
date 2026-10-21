import Observation
import Shared

/// Lo que vive mientras la app está abierta: la base cifrada y los casos de uso de `shared`.
@Observable
final class Aplicacion {
    let base: BaseMiGasto
    let registro: RegistroDeMovimientos

    init() {
        base = abrirLaBaseDeLaApp()
        registro = crearRegistroDeMovimientos(base: base)
    }
}
