import SwiftUI
import UIKit

/// Colores de MiGasto (claro y oscuro): los mismos de `lib/core/theme` de Flutter y de la app de Android.
enum Paleta {
    static let fondo = dinamico(claro: 0xF4F3F6, oscuro: 0x0F0E13)
    static let superficie = dinamico(claro: 0xFFFFFF, oscuro: 0x1B1922)
    static let tinta = dinamico(claro: 0x16141B, oscuro: 0xF2F0F5)
    static let apagado = dinamico(claro: 0x5B5866, oscuro: 0xA5A1B0)
    static let linea = dinamico(claro: 0xE1DFE6, oscuro: 0x2A2733)
    /// Verde de marca como texto o ícono (en claro es más oscuro).
    static let marcaTinta = dinamico(claro: 0x1E7A36, oscuro: 0x8CE885)
    static let fondoDeIcono = dinamico(claro: 0xF1F0F4, oscuro: 0x23212B)
    static let presupuestoBien = dinamico(claro: 0x1C8A34, oscuro: 0x3FCB5B)
    static let presupuestoCerca = dinamico(claro: 0x9A6400, oscuro: 0xFAB219)
    static let presupuestoPasado = dinamico(claro: 0xC23434, oscuro: 0xF07070)
    static let ingreso = dinamico(claro: 0x2458C6, oscuro: 0x7FB6FF)
    static let ingresoSuave = dinamico(claro: 0xE3EBFA, oscuro: 0x18233A)
    static let barra = dinamico(claro: 0xCFCCD7, oscuro: 0x4A4656)
    static let yape = dinamico(claro: 0x742284, oscuro: 0x9B4FD6)
    static let plin = dinamico(claro: 0x00857A, oscuro: 0x10BFAF)
    static let tarjeta = dinamico(claro: 0xA56A00, oscuro: 0xE8B04A)
    static let googlePay = dinamico(claro: 0x1A73E8, oscuro: 0x5B9BF8)
    static let manual = dinamico(claro: 0x6B6876, oscuro: 0x8A8794)
    /// El verde neón de los botones principales.
    static let verdeNeon = Color(hex: 0x8CE885)
    static let tintaSobreVerde = Color(hex: 0x0F0E13)

    private static func dinamico(claro: UInt32, oscuro: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: oscuro) : UIColor(hex: claro) })
    }
}

extension Color {
    init(hex: UInt32) { self.init(UIColor(hex: hex)) }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: 1
        )
    }
}
