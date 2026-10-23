import Shared
import SwiftUI

/// Un monto que, al volver a mostrarse (después de ocultarlo con el ojo), sube desde 0 hasta su valor, como en Flutter. La curva
/// y la duración las pone `shared` (`CuentaAnimada`); aquí solo se avanza el tiempo. Mientras está oculto muestra «••••».
struct MontoQueCuenta: View {
    let valor: Double
    /// `true` para las tarjetas de Ingresos y Gastos, que llevan `S/ ` delante; el saldo lo dibuja aparte.
    let conSoles: Bool
    let oculto: Bool

    @State private var inicio: Date?
    @State private var yaSeMostro = false

    var body: some View {
        Group {
            if oculto {
                Text("••••")
            } else if let inicio {
                TimelineView(.animation(paused: false)) { contexto in
                    let t = contexto.date.timeIntervalSince(inicio) * 1000 / Double(CuentaAnimada.shared.DURACION_EN_MILISEGUNDOS)
                    Text(texto(en: t))
                }
            } else {
                Text(texto(en: 1))
            }
        }
        .onChange(of: oculto) { _, ahoraOculto in
            // Solo al volver a mostrar cuenta; al ocultar cambia de una vez a «••••».
            inicio = ahoraOculto ? nil : Date()
        }
        .task(id: inicio) {
            guard let inicio else { return }
            let duracion = Double(CuentaAnimada.shared.DURACION_EN_MILISEGUNDOS) / 1000
            try? await Task.sleep(for: .seconds(duracion + 0.05))
            // Terminó: se deja el valor final y se quita el reloj.
            if self.inicio == inicio { self.inicio = nil }
        }
    }

    private func texto(en t: Double) -> String {
        conSoles ? CuentaAnimada.shared.textoConSoles(valor: valor, t: t) : CuentaAnimada.shared.texto(valor: valor, t: t)
    }
}
