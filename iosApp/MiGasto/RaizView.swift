import SwiftUI

/// La barra inferior de iOS (con Liquid Glass en iOS 26). Las pestañas que todavía no están hechas lo dicen.
struct RaizView: View {
    var body: some View {
        TabView {
            Tab("Resumen", systemImage: "house") { ResumenView() }
            Tab("Movimientos", systemImage: "list.bullet") { EnConstruccion(titulo: "Movimientos") }
            Tab("Reportes", systemImage: "chart.bar") { EnConstruccion(titulo: "Reportes") }
            Tab("Ajustes", systemImage: "gearshape") { EnConstruccion(titulo: "Ajustes") }
        }
    }
}

private struct EnConstruccion: View {
    let titulo: String

    var body: some View {
        Text("\(titulo): todavía no está hecha")
            .font(.outfit(16))
            .foregroundStyle(Paleta.apagado)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Paleta.fondo)
    }
}
