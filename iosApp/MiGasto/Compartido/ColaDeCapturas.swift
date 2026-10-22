import Foundation

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
    static func agregar(_ texto: String) -> Bool {
        guard let archivo else { return false }
        var textos = leerArchivo(archivo)
        textos.append(String(texto.prefix(largoMaximo)))
        guard let datos = try? JSONEncoder().encode(textos) else { return false }
        return (try? datos.write(to: archivo, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])) != nil
    }

    /// Los textos pendientes, en orden; el archivo se borra apenas se leen.
    static func tomarPendientes() -> [String] {
        guard let archivo else { return [] }
        let textos = leerArchivo(archivo)
        try? FileManager.default.removeItem(at: archivo)
        return textos
    }

    private static func leerArchivo(_ archivo: URL) -> [String] {
        guard let datos = try? Data(contentsOf: archivo) else { return [] }
        return (try? JSONDecoder().decode([String].self, from: datos)) ?? []
    }
}
