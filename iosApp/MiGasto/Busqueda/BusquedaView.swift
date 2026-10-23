import Shared
import SwiftUI

/// La búsqueda de la barra (la lupa aparte): busca entre todos los movimientos. Lo que coincide, los grupos por día y los
/// textos los arma `shared`; aquí solo se dibuja.
struct BusquedaView: View {
    let aplicacion: Aplicacion
    @State private var modelo = BusquedaModelo()
    @State private var buscando = false

    var body: some View {
        NavigationStack {
            ScrollView {
                if let r = modelo.resultados {
                    VStack(alignment: .leading, spacing: 16) {
                        if let resumen = r.resumen {
                            Text(resumen).font(.outfit(13, .semibold)).foregroundStyle(Paleta.apagado).padding(.horizontal, 4)
                        }
                        ForEach(r.grupos, id: \.etiqueta) { grupo in
                            GrupoView(grupo: grupo, oculto: aplicacion.montosOcultos)
                        }
                        if let mensaje = r.mensaje {
                            Text(mensaje)
                                .font(.outfit(15))
                                .foregroundStyle(Paleta.apagado)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity)
                                .padding(.top, 60)
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                }
            }
            .background(Paleta.fondo)
            .navigationTitle(TextosDeLaBusqueda.shared.TITULO)
            .searchable(text: $modelo.texto, isPresented: $buscando, prompt: TextosDeLaBusqueda.shared.PLACEHOLDER)
            // Al tocar la lupa la búsqueda se abre sola, con el teclado listo para escribir.
            .task(id: aplicacion.pestana) {
                guard aplicacion.pestana == .busqueda else { return }
                // La pestaña tiene que estar a la vista antes de poder abrir la búsqueda.
                try? await Task.sleep(for: .milliseconds(450))
                buscando = true
            }
            .navigationDestination(for: String.self) { DetalleView(id: $0, aplicacion: aplicacion) }
        }
        .task(id: modelo.texto) { await modelo.observar(aplicacion.base) }
    }
}
