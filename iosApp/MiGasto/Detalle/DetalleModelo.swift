import Observation
import Shared

/// Estado del detalle: lo que observa y hace `shared` (`SeguimientoDelDetalle`) y lo que el usuario está haciendo
/// (qué diálogo o qué hoja tiene abierta).
@MainActor
@Observable
final class DetalleModelo {
    private(set) var estado: EstadoDelDetalle?
    let seguimiento: SeguimientoDelDetalle

    init(base: BaseMiGasto, registro: RegistroDeMovimientos, id: String) {
        seguimiento = SeguimientoDelDetalle(base: base, registro: registro, id: id)
    }

    func observar() async {
        for await nuevo in seguimiento.estado {
            estado = nuevo
        }
    }
}
