import CoreLocation
import Shared

/// Lo que devuelve «Agregar ubicación»: la ubicación tomada, o por qué no se pudo.
enum ResultadoDeCaptura {
    case ok(UbicacionCapturada)
    case fallo(MotivoDeFallo)
}

/// Pide el permiso si falta y toma la ubicación una sola vez (solo ahora, nunca en segundo plano). La dirección se arma
/// con `armarDireccion` de `shared`, igual que en Android. Qué se dice cuando falla también viene de `shared`.
@MainActor
final class ServicioDeUbicacion: NSObject, CLLocationManagerDelegate {
    private let gestor = CLLocationManager()
    private var esperandoPermiso: CheckedContinuation<Void, Never>?
    private var esperandoUbicacion: CheckedContinuation<CLLocation?, Never>?

    override init() {
        super.init()
        gestor.delegate = self
        gestor.desiredAccuracy = kCLLocationAccuracyBest
    }

    func capturar() async -> ResultadoDeCaptura {
        if gestor.authorizationStatus == .notDetermined {
            await pedirPermiso()
        }
        switch gestor.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            break
        case .notDetermined:
            return .fallo(.denegada)
        default:
            // iOS no vuelve a preguntar después de un «No permitir»: solo se puede cambiar en los ajustes.
            return .fallo(.denegadaParaSiempre)
        }
        guard let ubicacion = await tomarUbicacion() else { return .fallo(.noDisponible) }
        let lugar = await direccion(de: ubicacion)
        return .ok(UbicacionCapturada(
            latitud: ubicacion.coordinate.latitude, longitud: ubicacion.coordinate.longitude,
            precision: ubicacion.horizontalAccuracy, lugar: lugar
        ))
    }

    private func pedirPermiso() async {
        await withCheckedContinuation { continuacion in
            esperandoPermiso = continuacion
            gestor.requestWhenInUseAuthorization()
        }
    }

    private func tomarUbicacion() async -> CLLocation? {
        await withCheckedContinuation { continuacion in
            esperandoUbicacion = continuacion
            gestor.requestLocation()
        }
    }

    private func direccion(de ubicacion: CLLocation) async -> String? {
        guard let marca = try? await CLGeocoder().reverseGeocodeLocation(ubicacion).first else { return nil }
        return armarDireccion(calle: marca.thoroughfare, numero: marca.subThoroughfare, barrio: marca.subLocality, ciudad: marca.locality)
    }

    nonisolated func locationManagerDidChangeAuthorization(_ gestor: CLLocationManager) {
        Task { @MainActor in
            guard gestor.authorizationStatus != .notDetermined else { return }
            esperandoPermiso?.resume()
            esperandoPermiso = nil
        }
    }

    nonisolated func locationManager(_ gestor: CLLocationManager, didUpdateLocations ubicaciones: [CLLocation]) {
        let ultima = ubicaciones.last
        Task { @MainActor in
            esperandoUbicacion?.resume(returning: ultima)
            esperandoUbicacion = nil
        }
    }

    nonisolated func locationManager(_ gestor: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in
            esperandoUbicacion?.resume(returning: nil)
            esperandoUbicacion = nil
        }
    }
}
