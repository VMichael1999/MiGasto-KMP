import Shared
import SwiftUI

/// Cubre la app con la pantalla de bloqueo cuando el usuario activó el bloqueo. Cuándo bloquear y cuándo pedir el
/// desbloqueo lo decide `ControlDeBloqueo` (de `shared`, el mismo de Android); aquí solo se pide Face ID y se dibuja.
struct PuertaDeBloqueoView<Contenido: View>: View {
    let aplicacion: Aplicacion
    @ViewBuilder let contenido: Contenido
    @Environment(\.scenePhase) private var fase
    @State private var control: ControlDeBloqueo
    @State private var version = 0
    private let autenticador = Autenticador()

    init(aplicacion: Aplicacion, @ViewBuilder contenido: () -> Contenido) {
        self.aplicacion = aplicacion
        self.contenido = contenido()
        _control = State(initialValue: ControlDeBloqueo(activadoAlAbrir: aplicacion.bloqueoActivado))
    }

    private var bloqueada: Bool {
        _ = version
        return control.visible(activado: aplicacion.bloqueoActivado)
    }

    var body: some View {
        ZStack {
            contenido
                .accessibilityHidden(bloqueada)
            if bloqueada {
                PantallaDeBloqueo(alDesbloquear: desbloquear)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: bloqueada)
        .task { if control.alAbrir() { desbloquear() } }
        .onChange(of: fase) { _, nueva in
            switch nueva {
            case .background, .inactive:
                control.alPasarAlFondo()
            case .active:
                if control.alVolver(activado: aplicacion.bloqueoActivado) {
                    version += 1
                    desbloquear()
                }
            @unknown default:
                break
            }
        }
    }

    private func desbloquear() {
        guard control.alEmpezarAutenticacion() else { return }
        Task {
            let correcta = await autenticador.autenticar(motivo: TextosDelBloqueo.shared.MOTIVO_AL_ABRIR)
            control.alTerminarAutenticacion(correcta: correcta)
            version += 1
        }
    }
}

private struct PantallaDeBloqueo: View {
    let alDesbloquear: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "lock").font(.system(size: 40)).foregroundStyle(Paleta.apagado)
            Text(TextosDelBloqueo.shared.TITULO)
                .font(.outfit(24, .semibold)).foregroundStyle(Paleta.tinta).multilineTextAlignment(.center).padding(.top, 16)
            Text(TextosDelBloqueo.shared.EXPLICACION)
                .font(.outfit(15)).foregroundStyle(Paleta.apagado).multilineTextAlignment(.center)
            Button(action: alDesbloquear) {
                Text(TextosDelBloqueo.shared.BOTON)
                    .font(.outfit(17, .semibold)).foregroundStyle(Paleta.tintaSobreVerde)
                    .frame(maxWidth: .infinity).frame(height: 54)
                    .background(Paleta.verdeNeon, in: RoundedRectangle(cornerRadius: 18))
            }
            .padding(.top, 24)
        }
        .padding(.horizontal, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Paleta.fondo)
        .contentShape(Rectangle())
    }
}
