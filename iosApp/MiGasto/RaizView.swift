import Shared
import SwiftUI

struct RaizView: View {
    @Environment(Aplicacion.self) private var aplicacion

    var body: some View {
        VStack(spacing: 8) {
            Text("MiGasto")
                .font(.custom("Outfit", size: 34, relativeTo: .largeTitle).weight(.bold))
            Text("Base cifrada abierta: \(aplicacion.base.movimientos.todos().count) movimientos")
                .font(.custom("Outfit", size: 16, relativeTo: .body))
                .foregroundStyle(.secondary)
        }
    }
}
