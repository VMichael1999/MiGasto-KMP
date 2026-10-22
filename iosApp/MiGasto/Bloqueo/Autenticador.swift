import LocalAuthentication

/// La autenticación del teléfono: Face ID (o Touch ID) y, como alternativa, el código del iPhone.
struct Autenticador {
    /// `true` si el iPhone tiene un código (o Face ID) configurado.
    func disponible() -> Bool {
        var error: NSError?
        return LAContext().canEvaluatePolicy(.deviceOwnerAuthentication, error: &error)
    }

    /// Pide desbloquear con [motivo]; `true` si salió bien, `false` si falló o se canceló.
    func autenticar(motivo: String) async -> Bool {
        let contexto = LAContext()
        return (try? await contexto.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: motivo)) ?? false
    }
}
