import AppIntents
import SwiftUI
import WidgetKit

/// Abre MiGasto en «Nuevo movimiento». Es `EnlacesDeLaApp.NUEVO_MOVIMIENTO` de `shared`: el widget no enlaza `shared`
/// (no necesita la base) y por eso el texto se repite aquí.
private let enlaceDeNuevoMovimiento = URL(string: "migasto://new")!

// MARK: - Widget de la pantalla de inicio y de la pantalla bloqueada

struct EntradaDelWidget: TimelineEntry {
    let date: Date
}

struct ProveedorDelWidget: TimelineProvider {
    func placeholder(in context: Context) -> EntradaDelWidget { EntradaDelWidget(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (EntradaDelWidget) -> Void) {
        completion(EntradaDelWidget(date: Date()))
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<EntradaDelWidget>) -> Void) {
        completion(Timeline(entries: [EntradaDelWidget(date: Date())], policy: .never))
    }
}

struct VistaDelWidget: View {
    @Environment(\.widgetFamily) private var familia

    private let tinta = Color(red: 0.06, green: 0.055, blue: 0.075)
    private let verde = Color(red: 0.55, green: 0.91, blue: 0.52)

    var body: some View {
        switch familia {
        case .accessoryCircular:
            ZStack {
                AccessoryWidgetBackground()
                Image(systemName: "plus").font(.title2.weight(.semibold))
            }
            .accessibilityLabel("Registrar movimiento")
        case .accessoryRectangular:
            HStack(spacing: 8) {
                Image(systemName: "plus.circle.fill").font(.title2)
                VStack(alignment: .leading) {
                    Text("MiGasto").font(.headline)
                    Text("Registrar movimiento").font(.caption)
                }
            }
        default:
            VStack(alignment: .leading, spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14).fill(tinta)
                    Image(systemName: "plus").font(.title2.weight(.bold)).foregroundStyle(verde)
                }
                .frame(width: 44, height: 44)
                Spacer()
                Text("Registrar").font(.headline).foregroundStyle(tinta)
                Text("Gasto o ingreso").font(.caption).foregroundStyle(tinta.opacity(0.7))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }
}

struct WidgetDeRegistro: Widget {
    let kind = "MiGastoQuickAdd"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ProveedorDelWidget()) { _ in
            VistaDelWidget()
                .widgetURL(enlaceDeNuevoMovimiento)
                .containerBackground(Color(red: 0.55, green: 0.91, blue: 0.52), for: .widget)
        }
        .configurationDisplayName("Registrar movimiento")
        .description("Abre MiGasto para registrar un gasto o un ingreso en segundos.")
        .supportedFamilies([.systemSmall, .accessoryCircular, .accessoryRectangular])
    }
}

// MARK: - Control del Centro de control

struct AbrirNuevoMovimientoIntent: AppIntent {
    static let title: LocalizedStringResource = "Registrar movimiento"
    static let openAppWhenRun = true

    func perform() async throws -> some IntentResult & OpensIntent {
        .result(opensIntent: OpenURLIntent(enlaceDeNuevoMovimiento))
    }
}

struct ControlDeRegistro: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "MiGastoQuickAddControl") {
            ControlWidgetButton(action: AbrirNuevoMovimientoIntent()) {
                Label("Registrar", systemImage: "plus.circle.fill")
            }
        }
        .displayName("Registrar movimiento")
        .description("Abre MiGasto en la pantalla de registro.")
    }
}

@main
struct MiGastoWidgets: WidgetBundle {
    var body: some Widget {
        WidgetDeRegistro()
        ControlDeRegistro()
    }
}
