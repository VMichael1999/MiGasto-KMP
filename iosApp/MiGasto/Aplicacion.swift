import Observation
import Shared

/// Lo que vive mientras la app está abierta: la base cifrada y los casos de uso de `shared`.
/// La clave de la base se genera la primera vez y se guarda en el Keychain.
@Observable
final class Aplicacion {
    let base: BaseMiGasto

    init() {
        let clave = ClaveDeLaBase(almacen: AlmacenSeguroIos(servicio: "com.migasto"), nombre: "migasto_db_key").obtenerOCrear()
        base = BaseMiGasto.companion.abrir(fabrica: FabricaDeDriverIos(directorio: nil), nombre: "migasto.db", clave: clave)
    }
}
