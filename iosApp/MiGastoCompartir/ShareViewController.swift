import UIKit
import UniformTypeIdentifiers
import Vision

/// La extensión «Compartir»: recibe una captura (por ejemplo, la constancia de Yape), lee su texto con Vision en el
/// teléfono y lo deja en la cola para que MiGasto lo registre. No sale nada del teléfono.
final class ShareViewController: UIViewController {
    private let mensaje = UILabel()
    private let rueda = UIActivityIndicatorView(style: .large)

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        mensaje.text = "Leyendo la captura…"
        mensaje.font = .preferredFont(forTextStyle: .headline)
        mensaje.textAlignment = .center
        mensaje.numberOfLines = 0
        rueda.startAnimating()
        let pila = UIStackView(arrangedSubviews: [rueda, mensaje])
        pila.axis = .vertical
        pila.spacing = 16
        pila.alignment = .center
        pila.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(pila)
        NSLayoutConstraint.activate([
            pila.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            pila.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            pila.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
        ])
        Task { await procesar() }
    }

    private func procesar() async {
        guard let imagen = await imagenCompartida() else { return terminar("No pudimos abrir la imagen.") }
        let texto = await leerTexto(de: imagen)
        guard !texto.isEmpty else { return terminar("No se leyó texto en la imagen.") }
        guard ColaDeCapturas.agregar(texto) else { return terminar("No se pudo guardar la captura. Abre MiGasto e inténtalo otra vez.") }
        terminar("Listo. Abre MiGasto para ver el pago.")
    }

    private func imagenCompartida() async -> CGImage? {
        let items = (extensionContext?.inputItems as? [NSExtensionItem]) ?? []
        for proveedor in items.flatMap({ $0.attachments ?? [] }) where proveedor.hasItemConformingToTypeIdentifier(UTType.image.identifier) {
            guard let elemento = try? await proveedor.loadItem(forTypeIdentifier: UTType.image.identifier) else { continue }
            if let url = elemento as? URL, let datos = try? Data(contentsOf: url), let imagen = UIImage(data: datos)?.cgImage { return imagen }
            if let imagen = elemento as? UIImage, let cg = imagen.cgImage { return cg }
            if let datos = elemento as? Data, let cg = UIImage(data: datos)?.cgImage { return cg }
        }
        return nil
    }

    /// Todo el texto de la imagen, en orden de lectura, en una sola línea (como lo espera el lector de pagos).
    private func leerTexto(de imagen: CGImage) async -> String {
        await withCheckedContinuation { continuacion in
            let pedido = VNRecognizeTextRequest { pedido, _ in
                let lineas = (pedido.results as? [VNRecognizedTextObservation])?.compactMap { $0.topCandidates(1).first?.string } ?? []
                continuacion.resume(returning: lineas.joined(separator: " "))
            }
            pedido.recognitionLevel = .accurate
            pedido.recognitionLanguages = ["es-ES", "en-US"]
            DispatchQueue.global(qos: .userInitiated).async {
                do {
                    try VNImageRequestHandler(cgImage: imagen).perform([pedido])
                } catch {
                    continuacion.resume(returning: "")
                }
            }
        }
    }

    private func terminar(_ texto: String) {
        rueda.stopAnimating()
        mensaje.text = texto
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { [weak self] in
            self?.extensionContext?.completeRequest(returningItems: nil)
        }
    }
}
