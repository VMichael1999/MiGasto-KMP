import Foundation
import Observation
import Shared

/// Lo que vive mientras la app está abierta: la base cifrada y los casos de uso de `shared`.
@Observable
final class Aplicacion {
    let base: BaseMiGasto
    let registro: RegistroDeMovimientos

    /// Si el bloqueo con Face ID o código está activado (lo cambia Ajustes; la puerta de bloqueo lo ve al instante).
    var bloqueoActivado: Bool

    /// Las pestañas de la barra. La lupa (`busqueda`) es la pestaña de búsqueda del sistema, aparte de las otras cuatro.
    enum Pestana: Hashable { case resumen, movimientos, reportes, ajustes, busqueda }

    /// La pestaña abierta; el Resumen la cambia con la lupa y con «Ver todos».
    var pestana: Pestana = .resumen

    /// Si el usuario ocultó los montos (el ojo del Resumen). Vale para todas las pantallas.
    var montosOcultos = false

    /// Un aviso breve que se ve sobre todo («Gasto guardado»), con una acción opcional («Deshacer»).
    struct Aviso: Equatable {
        let id = UUID()
        let mensaje: String
        let accion: String?
    }

    private(set) var aviso: Aviso?
    private var alAccionar: (() -> Void)?
    private var cierreDelAviso: Task<Void, Never>?

    /// Cuántas pantallas (el detalle) están encima de las pestañas: mientras haya una, el botón «+» se esconde.
    var pantallasEncima = 0

    func avisar(_ mensaje: String, accion: String? = nil, segundos: Double = 2.5, alAccionar: (() -> Void)? = nil) {
        aviso = Aviso(mensaje: mensaje, accion: accion)
        self.alAccionar = alAccionar
        cierreDelAviso?.cancel()
        cierreDelAviso = Task { @MainActor in
            try? await Task.sleep(for: .seconds(segundos))
            if !Task.isCancelled { aviso = nil }
        }
    }

    func accionarAviso() {
        alAccionar?()
        alAccionar = nil
        cierreDelAviso?.cancel()
        aviso = nil
    }

    private let recibidorDeCapturas: RecibidorDeCapturas
    private let registradorDeWallet: RegistradorDeWallet

    /// La misma instancia para la interfaz y para las acciones de Atajos: así comparten la base abierta.
    static let compartida = Aplicacion()

    /// Guarda un gasto de Apple Pay que pasó la acción de Atajos «Registrar movimiento».
    func registrarPagoDeWallet(monto: String, comercio: String, tarjeta: String?, latitud: Double?, longitud: Double?) -> ResultadoDeWallet {
        registradorDeWallet.registrar(
            monto: monto, comercio: comercio, tarjeta: tarjeta,
            latitud: latitud.map { KotlinDouble(value: $0) }, longitud: longitud.map { KotlinDouble(value: $0) }
        )
    }

    /// Registra las capturas que el usuario compartió con la extensión «Compartir» y dice qué pasó con cada una.
    func registrarCapturasPendientes() {
        let textos = ColaDeCapturas.tomarPendientes()
        guard !textos.isEmpty else { return }
        var resultados: [ResultadoDeLaCaptura] = []
        for texto in textos { resultados.append(recibidorDeCapturas.recibir(texto: texto)) }
        guard let ultimo = resultados.last else { return }
        let mensaje = resultados.count == 1 ? ultimo.titulo : "\(resultados.count) capturas: \(ultimo.titulo)"
        avisar(mensaje, segundos: 5)
    }

    init() {
        base = abrirLaBaseDeLaApp()
        registro = crearRegistroDeMovimientos(base: base)
        bloqueoActivado = base.ajustes.bloqueoActivado()
        recibidorDeCapturas = RecibidorDeCapturas(base: base, registro: registro)
        registradorDeWallet = RegistradorDeWallet(registro: registro)
    }
}
