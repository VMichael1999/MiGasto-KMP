import Observation
import Shared

/// Estado del Resumen: la pantalla ya armada por `shared` (se vuelve a emitir cuando cambian los movimientos),
/// y el mes que se mira.
@MainActor
@Observable
final class ResumenModelo {
    private(set) var pantalla: PantallaDelResumen?
    /// El mes elegido; `nil` es el mes en curso.
    var mes: MesElegido?

    var claveDelMes: String { mes.map { "\($0.anio)-\($0.mes)" } ?? "actual" }

    func observar(_ base: BaseMiGasto) async {
        for await nueva in observarPantallaDelResumen(base: base, mes: mes) {
            pantalla = nueva
        }
    }
}
