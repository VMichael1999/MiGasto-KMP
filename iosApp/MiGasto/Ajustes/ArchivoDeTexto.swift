import SwiftUI
import UniformTypeIdentifiers

/// Un archivo que se guarda con «Guardar en Archivos»: el CSV (texto) o el respaldo cifrado (que `shared` entrega en base64).
struct ArchivoDeTexto: FileDocument {
    static var readableContentTypes: [UTType] { [.data] }
    static var writableContentTypes: [UTType] { [.data, .commaSeparatedText] }

    let texto: String
    let nombre: String
    var esBase64 = false

    init(texto: String, nombre: String, esBase64: Bool = false) {
        self.texto = texto
        self.nombre = nombre
        self.esBase64 = esBase64
    }

    init(configuration: ReadConfiguration) throws {
        texto = String(data: configuration.file.regularFileContents ?? Data(), encoding: .utf8) ?? ""
        nombre = ""
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        let datos = esBase64 ? (Data(base64Encoded: texto) ?? Data()) : Data(texto.utf8)
        return FileWrapper(regularFileWithContents: datos)
    }
}
