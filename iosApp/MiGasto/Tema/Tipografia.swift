import SwiftUI

extension Font {
    /// Outfit (la fuente de MiGasto), que crece con el tamaño de texto del sistema.
    static func outfit(_ tamano: CGFloat, _ peso: Font.Weight = .regular, relativeTo estilo: Font.TextStyle = .body) -> Font {
        .custom("Outfit", size: tamano, relativeTo: estilo).weight(peso)
    }
}
