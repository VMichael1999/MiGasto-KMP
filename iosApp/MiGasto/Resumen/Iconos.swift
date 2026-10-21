import Shared

/// Íconos de SF Symbols para cada categoría y para las categorías propias del usuario.
func iconoDeCategoria(_ categoria: Categoria, propio: String? = nil) -> String {
    if let propio, let simbolo = iconosPropios[propio] { return simbolo }
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

private let iconosPropios: [String: String] = [
    "restaurante": "fork.knife", "cafe": "cup.and.saucer", "pizza": "fork.knife", "hamburguesa": "fork.knife",
    "pastel": "birthday.cake", "bar": "wineglass", "carrito": "cart", "bolsa": "bag", "tienda": "storefront",
    "ropa": "tshirt", "bus": "bus", "auto": "car", "gasolina": "fuelpump", "moto": "bicycle", "avion": "airplane",
    "hotel": "bed.double", "casa": "house", "llave": "key", "luz": "bolt", "agua": "drop", "wifi": "wifi",
    "celular": "iphone", "tele": "tv", "cine": "film", "musica": "music.note", "juegos": "gamecontroller",
    "futbol": "soccerball", "gimnasio": "dumbbell", "scissors": "scissors",
]
