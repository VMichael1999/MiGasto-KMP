import Foundation
import Observation
import Shared

/// Lo que vive mientras la app está abierta: la base cifrada y los casos de uso de `shared`.
@Observable
final class Aplicacion {
    let base: BaseMiGasto
    let registro: RegistroDeMovimientos

    /// Si el usuario ocultó los montos (el ojo del Resumen). Vale para todas las pantallas.
    var montosOcultos = false

    /// Un aviso breve que se ve sobre todo («Gasto guardado»), con una acción opcional («Deshacer»).
    struct Aviso: Equatable {
        let id = UUID()
        let mensaje: String
        let accion: String?
    }

    private(set) var aviso: Aviso?
    private var alAccionar: (() -> Void)?
    private var cierreDelAviso: Task<Void, Never>?

    /// Cuántas pantallas (el detalle) están encima de las pestañas: mientras haya una, el botón «+» se esconde.
    var pantallasEncima = 0

    func avisar(_ mensaje: String, accion: String? = nil, segundos: Double = 2.5, alAccionar: (() -> Void)? = nil) {
        aviso = Aviso(mensaje: mensaje, accion: accion)
        self.alAccionar = alAccionar
        cierreDelAviso?.cancel()
        cierreDelAviso = Task { @MainActor in
            try? await Task.sleep(for: .seconds(segundos))
            if !Task.isCancelled { aviso = nil }
        }
    }

    func accionarAviso() {
        alAccionar?()
        alAccionar = nil
        cierreDelAviso?.cancel()
        aviso = nil
    }

    init() {
        base = abrirLaBaseDeLaApp()
        registro = crearRegistroDeMovimientos(base: base)
    }
}
