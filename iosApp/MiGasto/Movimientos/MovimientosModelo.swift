import Observation
import Shared

/// Estado de Movimientos: la pantalla ya armada por `shared` (se vuelve a emitir cuando cambian los movimientos)
/// y los filtros puestos.
@MainActor
@Observable
final class MovimientosModelo {
    private(set) var pantalla: PantallaDeMovimientos?
    var filtros: FiltrosDeMovimientos = filtrosVacios()

    /// Cambia cada vez que cambia un filtro, para volver a observar con los nuevos.
    var claveDeLosFiltros: String { String(describing: filtros) }

    func observar(_ base: BaseMiGasto) async {
        for await nueva in observarPantallaDeMovimientos(base: base, filtros: filtros) {
            pantalla = nueva
        }
    }
}
