import AppIntents
import Foundation
import Shared

/// La acción de Atajos «Registrar movimiento». Se usa en la automatización «Transacción» de Wallet: cada vez que pagas
/// con Apple Pay, Atajos pasa el monto, el comercio y la tarjeta, y el gasto se guarda en MiGasto sin abrir la app.
/// Qué se guarda y qué se contesta lo decide `RegistradorDeWallet` de `shared` (el mismo código que usa Android).
struct RegistrarMovimientoIntent: AppIntent {
    static let title: LocalizedStringResource = "Registrar movimiento"
    static let description = IntentDescription(
        "Guarda un gasto en MiGasto con el monto, el comercio y la tarjeta que le pases.",
        categoryName: "Movimientos"
    )

    /// Se registra aunque la app esté cerrada: no abre MiGasto.
    static let openAppWhenRun = false

    @Parameter(title: "Monto", description: "Por ejemplo 25.50 o S/ 25.50")
    var monto: String

    @Parameter(title: "Comercio")
    var comercio: String

    @Parameter(title: "Tarjeta", description: "Nombre de la tarjeta de Wallet")
    var tarjeta: String?

    @Parameter(title: "Latitud", description: "Opcional, de la acción Obtener ubicación actual")
    var latitud: Double?

    @Parameter(title: "Longitud", description: "Opcional, de la acción Obtener ubicación actual")
    var longitud: Double?

    static var parameterSummary: some ParameterSummary {
        Summary("Registrar \(\.$monto) en \(\.$comercio) con \(\.$tarjeta)") {
            \.$latitud
            \.$longitud
        }
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let resultado = Aplicacion.compartida.registrarPagoDeWallet(
            monto: monto, comercio: comercio, tarjeta: tarjeta, latitud: latitud, longitud: longitud
        )
        if resultado.desenlace == .montoInvalido { throw ErrorDeRegistro.montoInvalido(resultado.titulo) }
        let texto = resultado.detalle.isEmpty ? resultado.titulo : "\(resultado.titulo). \(resultado.detalle)"
        return .result(dialog: IntentDialog(stringLiteral: texto))
    }
}

enum ErrorDeRegistro: Error, CustomLocalizedStringResourceConvertible {
    case montoInvalido(String)

    var localizedStringResource: LocalizedStringResource {
        switch self {
        case .montoInvalido(let mensaje): return LocalizedStringResource(stringLiteral: mensaje)
        }
    }
}

/// Hace que la acción aparezca en Atajos y en Spotlight.
struct AtajosDeMiGasto: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: RegistrarMovimientoIntent(),
            phrases: ["Registrar un movimiento en \(.applicationName)"],
            shortTitle: "Registrar movimiento",
            systemImageName: "plus.circle"
        )
    }
}
