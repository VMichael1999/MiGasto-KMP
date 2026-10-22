import Observation
import Shared

/// Estado de «Dónde pagaste»: la pantalla ya armada por `shared` (período elegido y zonas), siempre al día.
@MainActor
@Observable
final class DondePagasteModelo {
    private(set) var pantalla: PantallaDeDondePagaste?
    let seguimiento: SeguimientoDeDondePagaste

    init(base: BaseMiGasto) {
        seguimiento = SeguimientoDeDondePagaste(base: base)
    }

    func observar() async {
        for await nueva in seguimiento.estado {
            pantalla = nueva
        }
    }
}
