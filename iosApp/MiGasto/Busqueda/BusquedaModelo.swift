import Observation
import Shared

/// Estado de la búsqueda de la barra: el texto escrito y los resultados que arma `shared`, siempre al día.
@MainActor
@Observable
final class BusquedaModelo {
    private(set) var resultados: ResultadosDeBusqueda?
    var texto = ""

    func observar(_ base: BaseMiGasto) async {
        for await nuevos in observarBusqueda(base: base, texto: texto) {
            resultados = nuevos
        }
    }
}
