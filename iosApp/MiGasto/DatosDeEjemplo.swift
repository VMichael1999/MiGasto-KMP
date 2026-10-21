#if DEBUG
import Shared

/// Datos de ejemplo del diseño (`docs/diseno/migasto-kmp.html`), SOLO en depuración y solo con la base vacía.
/// Sirven para comparar la pantalla con el diseño: quedan S/ 2,655.30, por confirmar S/ 230.00 y presupuesto
/// S/ 1,500 con el 10 % usado. Se cargan con los mismos casos de uso que usa la app; la versión de producción
/// no compila este archivo.
enum DatosDeEjemplo {
    static func aplicar(_ app: Aplicacion) {
        guard app.base.movimientos.cantidad() == 0 else { return }
        app.base.ajustes.guardarPresupuesto(monto: 1500)
        let barberia = app.base.categoriasPropias.agregar(nombre: "Barbería", icono: "scissors", tipo: .gasto)
        let registro = app.registro
        func gasto(_ monto: Double, _ comercio: String, _ categoria: Categoria, _ fuente: PaymentSource, propia: String? = nil) {
            _ = registro.agregarManual(tipo: .gasto, monto: monto, comercio: comercio, categoria: categoria, categoriaPropia: propia, fuente: fuente)
        }
        _ = registro.agregarManual(tipo: .ingreso, monto: 2800, comercio: "Sueldo octubre", categoria: .sueldo, categoriaPropia: nil, fuente: .efectivo)
        gasto(18.5, "Café Central", .alimentacion, .yape)
        gasto(12, "Taxi", .transporte, .yape)
        gasto(89.2, "Minimarket La Esquina", .compras, .googlePay)
        gasto(25, "Barbería El Patrón", .otros, .efectivo, propia: barberia.id)
        // Los dos ingresos por confirmar entran como los detecta una captura de pago: quedan pendientes.
        _ = registro.registrarPagoDetectado(
            monto: 150, comercio: "Ana", proveedor: "yape", textoOriginal: "Ana te yapeó S/ 150.00", tipo: .ingreso, canal: .captura, pendienteActual: nil
        )
        _ = registro.registrarPagoDetectado(
            monto: 80, comercio: "Carlos", proveedor: "plin", textoOriginal: "Carlos te plineó S/ 80.00", tipo: .ingreso, canal: .captura, pendienteActual: nil
        )
    }
}
#endif
