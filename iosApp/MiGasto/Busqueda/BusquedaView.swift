import Shared
import SwiftUI

/// La búsqueda de la barra (la lupa aparte): igual que en Android, con los chips (Todo, Gastos, Ingresos, Por confirmar) y los
/// movimientos por día con su total de gastos. Con el campo vacío se ven todos. Los grupos y los textos los arma `shared`.
struct BusquedaView: View {
    let aplicacion: Aplicacion
    @State private var modelo = BusquedaModelo()
    @State private var buscando = false

    var body: some View {
        NavigationStack {
            ScrollView {
                if let p = modelo.pantalla {
                    VStack(alignment: .leading, spacing: 16) {
                        TipoDeMovimientos(pantalla: p) { modelo.filtros = modelo.filtros.conTipo(nuevo: $0) }
                        if p.grupos.isEmpty {
                            Text(p.textoSinMovimientos)
                                .font(.outfit(15))
                                .foregroundStyle(Paleta.apagado)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity)
                                .padding(.top, 60)
                        } else {
                            ForEach(p.grupos, id: \.etiqueta) { grupo in
                                GrupoView(grupo: grupo, oculto: aplicacion.montosOcultos)
                            }
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                }
            }
            .background(Paleta.fondo)
            .navigationTitle(TextosDeLaBusqueda.shared.TITULO)
            .searchable(text: textoBuscado, isPresented: $buscando, prompt: TextosDeLaBusqueda.shared.PLACEHOLDER)
            .task(id: aplicacion.pestana) {
                guard aplicacion.pestana == .busqueda else { return }
                // La pestaña tiene que estar a la vista antes de poder abrir la búsqueda.
                try? await Task.sleep(for: .milliseconds(450))
                buscando = true
            }
            .navigationDestination(for: String.self) { DetalleView(id: $0, aplicacion: aplicacion) }
        }
        .task(id: modelo.claveDeLosFiltros) { await modelo.observar(aplicacion.base) }
    }

    private var textoBuscado: Binding<String> {
        Binding(get: { modelo.texto }, set: { modelo.texto = $0 })
    }
}
