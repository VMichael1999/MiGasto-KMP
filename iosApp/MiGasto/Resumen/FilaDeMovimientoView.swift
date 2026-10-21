import Shared
import SwiftUI

/// Una fila de movimiento (en el Resumen y, más adelante, en Movimientos). Los textos ya vienen armados.
struct FilaDeMovimientoView: View {
    let fila: FilaDeMovimiento
    let oculto: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconoDeCategoria(fila.categoria, propio: fila.iconoPropio))
                .font(.system(size: 17, weight: .regular))
                .foregroundStyle(fila.esIngreso ? Paleta.ingreso : Paleta.tinta)
                .frame(width: 36, height: 36)
                .background(fila.esIngreso ? Paleta.ingresoSuave : Paleta.fondoDeIcono, in: RoundedRectangle(cornerRadius: 11))
            VStack(alignment: .leading, spacing: 2) {
                Text(fila.nombre)
                    .font(.outfit(15, .semibold))
                    .foregroundStyle(Paleta.tinta)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Circle().fill(colorDeLaFuente(fila.fuente)).frame(width: 7, height: 7)
                    Text(fila.fuenteYHora)
                        .font(.outfit(12.5, relativeTo: .caption))
                        .foregroundStyle(Paleta.apagado)
                }
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 3) {
                Text(oculto ? "••••" : fila.monto)
                    .font(.outfit(15, .semibold))
                    .foregroundStyle(fila.esIngreso ? Paleta.ingreso : Paleta.tinta)
                if fila.porConfirmar {
                    Text("Por confirmar")
                        .font(.outfit(11, .semibold, relativeTo: .caption2))
                        .foregroundStyle(Paleta.ingreso)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 2)
                        .background(Paleta.ingresoSuave, in: Capsule())
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .accessibilityElement(children: .combine)
    }
}

func colorDeLaFuente(_ fuente: PaymentSource) -> Color {
    switch fuente {
    case .yape: return Paleta.yape
    case .plin: return Paleta.plin
    case .googlePay: return Paleta.googlePay
    case .tarjeta: return Paleta.tarjeta
    case .efectivo, .manual, .otro: return Paleta.manual
    }
}
