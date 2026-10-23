import Shared
import SwiftUI

/// La barra inferior de iOS (con Liquid Glass en iOS 26) y el botón «+» que abre Nuevo movimiento.
/// Las pestañas que todavía no están hechas lo dicen.
struct RaizView: View {
    @Environment(Aplicacion.self) private var aplicacion
    @State private var nuevoAbierto = false
    @Environment(\.scenePhase) private var fase

    var body: some View {
        @Bindable var aplicacion = aplicacion
        TabView(selection: $aplicacion.pestana) {
            Tab("Resumen", systemImage: "house", value: Aplicacion.Pestana.resumen) { ResumenView() }
            Tab("Movimientos", systemImage: "list.bullet", value: Aplicacion.Pestana.movimientos) { MovimientosView() }
            Tab("Reportes", systemImage: "chart.bar", value: Aplicacion.Pestana.reportes) { ReportesView(aplicacion: aplicacion) }
            Tab("Ajustes", systemImage: "gearshape", value: Aplicacion.Pestana.ajustes) { AjustesView(aplicacion: aplicacion) }
            // La lupa: el sistema la dibuja aparte de las otras cuatro, a la derecha de la barra.
            Tab(value: Aplicacion.Pestana.busqueda, role: .search) { BusquedaView(aplicacion: aplicacion) }
        }
        .overlay(alignment: .bottomTrailing) {
            if aplicacion.pantallasEncima == 0 && aplicacion.pestana != .busqueda {
                BotonAgregar { nuevoAbierto = true }
                    .padding(.trailing, 21)
                    .padding(.bottom, 55)
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

/// El «+» de vidrio verde que flota sobre la barra (Liquid Glass, como en el diseño).
private struct BotonAgregar: View {
    let alTocar: () -> Void

    var body: some View {
        Button(action: alTocar) {
            Image(systemName: "plus")
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(Color(hex: 0x0F3D1C))
                .frame(width: 56, height: 56)
        }
        .buttonStyle(.plain)
        .glassEffect(.regular.tint(Paleta.verdeNeon.opacity(0.78)).interactive(), in: .circle)
        .accessibilityLabel("Agregar movimiento")
    }
}
