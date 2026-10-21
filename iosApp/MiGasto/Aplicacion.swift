import Observation
import Shared

/// Lo que vive mientras la app está abierta: la base cifrada y los casos de uso de `shared`.
@Observable
final class Aplicacion {
    let base: BaseMiGasto
    let registro: RegistroDeMovimientos

    /// Si el usuario ocultó los montos (el ojo del Resumen). Vale para todas las pantallas.
    var montosOcultos = false

    /// Un aviso breve que se ve sobre todo («Gasto guardado»).
    private(set) var aviso: String?
    private var cierreDelAviso: Task<Void, Never>?

    func avisar(_ mensaje: String) {
        aviso = mensaje
        cierreDelAviso?.cancel()
        cierreDelAviso = Task { @MainActor in
            try? await Task.sleep(for: .seconds(2.5))
            if !Task.isCancelled { aviso = nil }
        }
    }

    init() {
        base = abrirLaBaseDeLaApp()
        registro = crearRegistroDeMovimientos(base: base)
    }
}
