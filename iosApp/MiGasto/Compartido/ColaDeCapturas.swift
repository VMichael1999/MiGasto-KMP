import Foundation

/// Una captura compartida, a la espera de que la app la registre: el texto que Vision leyó y, si el usuario la cambió en la
/// hoja de la extensión, la clave de la categoría elegida.
struct CapturaPendiente: Codable {
    let texto: String
    let categoria: String?
}

/// La cola de capturas compartidas: la extensión «Compartir» deja aquí el texto que Vision leyó, y la app lo registra
/// cuando se abre. Así la extensión nunca toca la base cifrada ni su clave (que viven solo en la app).
/// Este archivo está en la app y en la extensión.
enum ColaDeCapturas {
    static let grupo = "group.com.migasto.ios"
    private static let nombreDelArchivo = "capturas-pendientes.json"
    /// Lo que se guarda de cada captura: alcanza para la constancia y limita los datos personales en el archivo.
    private static let largoMaximo = 1500

    private static var archivo: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: grupo)?.appendingPathComponent(nombreDelArchivo)
    }

    /// `false` si no hay carpeta compartida (la app no tiene el permiso de grupo).
    @discardableResult
    static func agregar(_ texto: String, categoria: String? = nil) -> Bool {
        guard let archivo else { return false }
        var capturas = leerArchivo(archivo)
        capturas.append(CapturaPendiente(texto: String(texto.prefix(largoMaximo)), categoria: categoria))
        guard let datos = try? JSONEncoder().encode(capturas) else { return false }
        return (try? datos.write(to: archivo, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])) != nil
    }

    /// Las capturas pendientes, en orden; el archivo se borra apenas se leen.
    static func tomarPendientes() -> [CapturaPendiente] {
        guard let archivo else { return [] }
        let capturas = leerArchivo(archivo)
        try? FileManager.default.removeItem(at: archivo)
        return capturas
    }

    private static func leerArchivo(_ archivo: URL) -> [CapturaPendiente] {
        guard let datos = try? Data(contentsOf: archivo) else { return [] }
        if let capturas = try? JSONDecoder().decode([CapturaPendiente].self, from: datos) { return capturas }
        // Una cola vieja era una lista de textos sueltos.
        return ((try? JSONDecoder().decode([String].self, from: datos)) ?? []).map { CapturaPendiente(texto: $0, categoria: nil) }
    }
}
