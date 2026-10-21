import SwiftUI

@main
struct MiGastoApp: App {
    @State private var aplicacion = Aplicacion()

    var body: some Scene {
        WindowGroup {
            RaizView()
                .environment(aplicacion)
                .tint(Paleta.marcaTinta)
                .task {
                    #if DEBUG
                    DatosDeEjemplo.aplicar(aplicacion)
                    #endif
                }
        }
    }
}
