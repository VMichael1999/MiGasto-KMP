import Shared
import SwiftUI

/// La barra inferior de iOS (con Liquid Glass en iOS 26) y el botón «+» que abre Nuevo movimiento.
/// Las pestañas que todavía no están hechas lo dicen.
struct RaizView: View {
    @Environment(Aplicacion.self) private var aplicacion
    @State private var nuevoAbierto = false
    @Environment(\.scenePhase) private var fase

    var body: some View {
        TabView {
            Tab("Resumen", systemImage: "house") { ResumenView() }
            Tab("Movimientos", systemImage: "list.bullet") { MovimientosView() }
            Tab("Reportes", systemImage: "chart.bar") { ReportesView(aplicacion: aplicacion) }
            Tab("Ajustes", systemImage: "gearshape") { AjustesView(aplicacion: aplicacion) }
        }
        .overlay(alignment: .bottomTrailing) {
            if aplicacion.pantallasEncima == 0 {
            Button { nuevoAbierto = true } label: {
                Image(systemName: "plus")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(Paleta.tintaSobreVerde)
                    .frame(width: 58, height: 58)
                    .background(Paleta.verdeNeon, in: Circle())
                    .shadow(color: .black.opacity(0.18), radius: 10, y: 4)
            }
            .padding(.trailing, 20)
            .padding(.bottom, 96)
            .accessibilityLabel("Agregar movimiento")
            }
        }
        .overlay(alignment: .bottom) {
            if let aviso = aplicacion.aviso {
                HStack(spacing: 14) {
                    Text(aviso.mensaje)
                        .font(.outfit(14.5, .medium))
                        .foregroundStyle(Paleta.fondo)
                    if let accion = aviso.accion {
                        Button(accion) { aplicacion.accionarAviso() }
                            .font(.outfit(14.5, .bold))
                            .foregroundStyle(Paleta.verdeNeon)
                    }
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
                .background(Paleta.tinta, in: Capsule())
                .padding(.bottom, 100)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .accessibilityAddTraits(.updatesFrequently)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: aplicacion.aviso)
        .onOpenURL { enlace in
            if EnlacesDeLaApp.shared.esNuevoMovimiento(enlace: enlace.absoluteString) { nuevoAbierto = true }
        }
        .task { aplicacion.registrarCapturasPendientes() }
        .onChange(of: fase) { _, nueva in if nueva == .active { aplicacion.registrarCapturasPendientes() } }
        .fullScreenCover(isPresented: $nuevoAbierto) { NuevoMovimientoView() }
    }
}
