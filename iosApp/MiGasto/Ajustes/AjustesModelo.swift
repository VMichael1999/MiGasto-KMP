import Foundation
import Observation
import Shared

/// Estado de Ajustes: la pantalla ya armada por `shared`, siempre al día, y los pasos del respaldo cifrado.
@MainActor
@Observable
final class AjustesModelo {
    private(set) var pantalla: PantallaDeAjustes?
    let seguimiento: SeguimientoDeAjustes

    /// Mientras se cifra o se abre un respaldo (tarda unos segundos), el mensaje que se muestra.
    private(set) var ocupado: String?

    /// Un respaldo ya cifrado, a la espera de que el usuario elija dónde guardarlo.
    var respaldoParaGuardar: ArchivoDeTexto?
    /// Un respaldo abierto, a la espera de que el usuario confirme restaurarlo.
    var respaldoAbierto: AperturaDeRespaldo?
    /// El CSV listo para guardar.
    var csvParaGuardar: ArchivoDeTexto?
    /// Un archivo elegido para restaurar, a la espera de su contraseña.
    var archivoParaAbrir: String?

    init(base: BaseMiGasto, registro: RegistroDeMovimientos) {
        seguimiento = SeguimientoDeAjustes(base: base, registro: registro)
    }

    func observar() async {
        for await nueva in seguimiento.estado {
            pantalla = nueva
        }
    }

    func prepararCsv() {
        csvParaGuardar = ArchivoDeTexto(texto: seguimiento.csv(), nombre: "MiGasto-movimientos")
    }

    /// Cifra el respaldo y lo deja listo para guardar. Devuelve el error (para avisar) o `nil`.
    func crearRespaldo(contrasena: String) async -> String? {
        ocupado = "Cifrando tu respaldo…"
        defer { ocupado = nil }
        let resultado = try? await seguimiento.crearRespaldo(contrasena: contrasena)
        guard let datos = resultado?.datosEnBase64 else { return resultado?.error ?? "No se pudo crear el respaldo." }
        respaldoParaGuardar = ArchivoDeTexto(texto: datos, nombre: seguimiento.nombreDelArchivoDeRespaldo(), esBase64: true)
        return nil
    }

    /// Abre el respaldo elegido con su contraseña. Devuelve el error o `nil` (y deja `respaldoAbierto`).
    func abrirRespaldo(contrasena: String) async -> String? {
        guard let datos = archivoParaAbrir else { return "No se pudo leer el archivo." }
        ocupado = "Abriendo tu respaldo…"
        defer { ocupado = nil }
        guard let apertura = try? await seguimiento.abrirRespaldo(datosEnBase64: datos, contrasena: contrasena) else {
            return "No se pudo abrir el respaldo."
        }
        if apertura.contenido == nil { return apertura.error ?? "No se pudo abrir el respaldo." }
        archivoParaAbrir = nil
        respaldoAbierto = apertura
        return nil
    }
}
