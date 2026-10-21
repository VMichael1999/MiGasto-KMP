import Observation
import Shared

/// Estado de Reportes: la pantalla ya armada por `shared` (período elegido y reporte, siempre al día).
@MainActor
@Observable
final class ReportesModelo {
    private(set) var pantalla: PantallaDeReportes?
    let seguimiento: SeguimientoDeReportes

    init(base: BaseMiGasto) {
        seguimiento = SeguimientoDeReportes(base: base)
    }

    func observar() async {
        for await nueva in seguimiento.estado {
            pantalla = nueva
        }
    }
}
