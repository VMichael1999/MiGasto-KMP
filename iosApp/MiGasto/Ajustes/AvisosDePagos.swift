import Foundation
import UserNotifications

/// El estado del permiso de avisos de MiGasto, para la fila «Avisos de pagos» de Ajustes.
enum EstadoDeAvisos {
    case activos, porActivar, bloqueados
}

/// Los avisos locales de un pago registrado (por ejemplo, con la acción de Atajos de Apple Pay, que corre sin abrir la app).
enum AvisosDePagos {
    private static var centro: UNUserNotificationCenter { UNUserNotificationCenter.current() }

    static func estado() async -> EstadoDeAvisos {
        switch await centro.notificationSettings().authorizationStatus {
        case .authorized, .provisional, .ephemeral: .activos
        case .denied: .bloqueados
        default: .porActivar
        }
    }

    /// Pide el permiso (iOS solo pregunta una vez). `true` si quedó activo.
    static func pedirPermiso() async -> Bool {
        (try? await centro.requestAuthorization(options: [.alert, .sound])) ?? false
    }

    /// Avisa si el permiso está dado; si no, no hace nada.
    static func avisar(titulo: String, detalle: String) async {
        guard await estado() == .activos else { return }
        let contenido = UNMutableNotificationContent()
        contenido.title = titulo
        contenido.body = detalle
        try? await centro.add(UNNotificationRequest(identifier: UUID().uuidString, content: contenido, trigger: nil))
    }
}
