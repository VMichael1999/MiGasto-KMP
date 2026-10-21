import SwiftUI

/// Pone los elementos en fila y salta a la siguiente cuando no caben (los chips de la hoja de filtros).
struct FlowLayout: Layout {
    var espacio: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        medir(ancho: proposal.width ?? .infinity, subviews: subviews).tamano
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let posiciones = medir(ancho: bounds.width, subviews: subviews).posiciones
        for (subvista, posicion) in zip(subviews, posiciones) {
            subvista.place(at: CGPoint(x: bounds.minX + posicion.x, y: bounds.minY + posicion.y), proposal: .unspecified)
        }
    }

    private func medir(ancho: CGFloat, subviews: Subviews) -> (tamano: CGSize, posiciones: [CGPoint]) {
        var posiciones: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var altoDeLaFila: CGFloat = 0
        var anchoMaximo: CGFloat = 0
        for subvista in subviews {
            let tamano = subvista.sizeThatFits(.unspecified)
            if x > 0, x + tamano.width > ancho {
                x = 0
                y += altoDeLaFila + espacio
                altoDeLaFila = 0
            }
            posiciones.append(CGPoint(x: x, y: y))
            x += tamano.width + espacio
            altoDeLaFila = max(altoDeLaFila, tamano.height)
            anchoMaximo = max(anchoMaximo, x - espacio)
        }
        return (CGSize(width: anchoMaximo, height: y + altoDeLaFila), posiciones)
    }
}
