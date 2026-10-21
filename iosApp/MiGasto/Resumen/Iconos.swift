import Shared
import UIKit

/// Íconos de SF Symbols para cada categoría y para las categorías propias del usuario.
func iconoDeCategoria(_ categoria: Categoria, propio: String? = nil) -> String {
    if let propio { return simboloDeIconoPropio(propio) }
    switch categoria {
    case .alimentacion: return "fork.knife"
    case .transporte: return "car"
    case .compras: return "bag"
    case .servicios: return "creditcard"
    case .entretenimiento: return "film"
    case .otros, .otrosIngresos: return "square.grid.2x2"
    case .sueldo: return "briefcase"
    case .transferenciaRecibida: return "arrow.left.arrow.right"
    case .venta: return "tag"
    }
}

/// El símbolo de SF Symbols que corresponde a la clave de una categoría propia (genérico si no hay).
func simboloDeIconoPropio(_ clave: String) -> String {
    simbolo(iconosPropios[clave] ?? "tag")
}

/// Un símbolo que no exista en esta versión de iOS se ve como una etiqueta, nunca vacío.
private func simbolo(_ nombre: String) -> String {
    UIImage(systemName: nombre) != nil ? nombre : "tag"
}

private let iconosPropios: [String: String] = [
    "restaurante": "fork.knife", "cafe": "cup.and.saucer", "pizza": "fork.knife", "hamburguesa": "fork.knife",
    "pastel": "birthday.cake", "bar": "wineglass", "carrito": "cart", "bolsa": "bag", "tienda": "storefront",
    "ropa": "tshirt", "bus": "bus", "auto": "car", "gasolina": "fuelpump", "moto": "bicycle", "avion": "airplane",
    "hotel": "bed.double", "casa": "house", "llave": "key", "luz": "bolt", "agua": "drop", "wifi": "wifi",
    "celular": "iphone", "tele": "tv", "cine": "film", "musica": "music.note", "juegos": "gamecontroller",
    "futbol": "soccerball", "gimnasio": "dumbbell", "spa": "leaf", "salud": "cross.case", "medicina": "pills",
    "escuela": "graduationcap", "libro": "book", "bebe": "figure.and.child.holdinghands", "mascota": "pawprint",
    "regalo": "gift", "fiesta": "party.popper", "ahorro": "banknote", "banco": "building.columns",
    "tarjeta": "creditcard", "pagos": "dollarsign.circle", "trabajo": "briefcase", "herramientas": "wrench.and.screwdriver",
    "arreglo": "hammer", "computadora": "laptopcomputer", "camara": "camera", "arte": "paintpalette",
    "lavanderia": "washer", "donacion": "heart.circle", "corazon": "heart", "estrella": "star", "planta": "leaf",
    "parque": "tree", "playa": "beach.umbrella", "maleta": "suitcase", "recibo": "receipt",
    "inversion": "chart.line.uptrend.xyaxis", "flores": "camera.macro", "scissors": "scissors",
]
