import Observation
import Shared

/// Estado de la búsqueda de la barra: es la pantalla de Movimientos (la arma `shared`) con su propio texto de búsqueda y sus
/// chips, aparte de los filtros de la pestaña Movimientos.
@MainActor
@Observable
final class BusquedaModelo {
    private(set) var pantalla: PantallaDeMovimientos?
    var filtros: FiltrosDeMovimientos = filtrosVacios()

    var claveDeLosFiltros: String { String(describing: filtros) }

    var texto: String {
        get { filtros.busqueda }
        set { filtros = filtros.conBusqueda(texto: newValue) }
    }

    func observar(_ base: BaseMiGasto) async {
        for await nueva in observarPantallaDeMovimientos(base: base, filtros: filtros) {
            pantalla = nueva
        }
    }
}
