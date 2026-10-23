import Shared
import SwiftUI

/// La hoja «Nuevo movimiento» (mg-03), como las de Apple: Cancelar y Guardar arriba, monto con teclado propio, nota y
/// fuente en una tarjeta, y las categorías en cuadrícula. Las reglas (monto, categorías, textos) las pone `shared`.
struct NuevoMovimientoView: View {
    @Environment(Aplicacion.self) private var aplicacion
    @Environment(\.dismiss) private var cerrar
    @State private var modelo = NuevoMovimientoModelo()
    @State private var eligiendoFuente = false
    @State private var creandoCategoria = false

    private let columnas = Array(repeating: GridItem(.flexible(), spacing: 8), count: 3)

    var body: some View {
        let f = modelo.formulario
        NavigationStack {
            VStack(spacing: 0) {
                ScrollView {
                    VStack(spacing: 12) {
                        SelectorDeTipo(tipo: f.tipo) { modelo.formulario = modelo.formulario.conTipo(nuevo: $0) }
                        Monto(formulario: f)
                        NotaYFuente(formulario: f, alEscribir: { modelo.formulario = modelo.formulario.conConcepto(texto: $0) }) {
                            eligiendoFuente = true
                        }
                        LazyVGrid(columns: columnas, spacing: 8) {
                            ForEach(Array(f.chipsDeCategoria(propias: modelo.propias).enumerated()), id: \.offset) { _, opcion in
                                CeldaDeCategoria(texto: opcion.etiqueta, icono: iconoDeCategoria(opcion.categoria, propio: opcion.propia?.icono), elegido: opcion.elegido, esIngreso: f.esIngreso) {
                                    modelo.formulario = modelo.formulario.conChip(chip: opcion)
                                }
                            }
                            CeldaDeCategoria(texto: TextosDeNuevoMovimiento.shared.NUEVA, icono: "plus", elegido: false, esIngreso: f.esIngreso) { creandoCategoria = true }
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.vertical, 6)
                }
                Teclado { modelo.formulario = modelo.formulario.presionando(tecla: $0) }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 8)
            }
            .background(Paleta.fondo)
            .navigationTitle(TextosDeNuevoMovimiento.shared.TITULO)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(TextosDeNuevoMovimiento.shared.CANCELAR) { cerrar() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(TextosDeNuevoMovimiento.shared.GUARDAR) { guardar() }
                        .disabled(!f.puedeGuardar)
                        .accessibilityLabel(f.textoDelBoton)
                }
            }
        }
        .task { modelo.cargarPropias(aplicacion.base) }
        .confirmationDialog(f.prefijoDeLaFuente, isPresented: $eligiendoFuente, titleVisibility: .visible) {
            ForEach(opcionesDeFuente(), id: \.etiqueta) { opcion in
                Button(opcion.etiqueta) { modelo.formulario = modelo.formulario.conFuente(nueva: opcion.fuente) }
            }
        }
        .sheet(isPresented: $creandoCategoria) {
            HojaDeCategoriaNuevaView { nombre, icono in
                modelo.crearCategoria(nombre: nombre, icono: icono, base: aplicacion.base)
            }
        }
    }

    private func guardar() {
        guard let mensaje = modelo.guardar(aplicacion.registro) else { return }
        aplicacion.avisar(mensaje)
        cerrar()
    }
}

private struct SelectorDeTipo: View {
    let tipo: TipoMovimiento
    let alElegir: (TipoMovimiento) -> Void

    var body: some View {
        HStack(spacing: 2) {
            segmento("Gasto", .gasto)
            segmento("Ingreso", .ingreso)
        }
        .padding(3)
        .background(Paleta.linea.opacity(0.7), in: Capsule())
    }

    private func segmento(_ texto: String, _ valor: TipoMovimiento) -> some View {
        Button { alElegir(valor) } label: {
            Text(texto)
                .font(.outfit(14.5, .semibold))
                .foregroundStyle(Paleta.tinta)
                .frame(maxWidth: .infinity, minHeight: 36)
                .background(tipo == valor ? Paleta.superficie : Color.clear, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(tipo == valor ? .isSelected : [])
    }
}

private struct Monto: View {
    let formulario: FormularioNuevoMovimiento

    var body: some View {
        VStack(spacing: 2) {
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(formulario.prefijoDelMonto)
                    .font(.outfit(24, .medium, relativeTo: .title2))
                    .foregroundStyle(Paleta.apagado)
                Text(formulario.entrada.texto)
                    .font(.outfit(52, .bold, relativeTo: .largeTitle))
                    .foregroundStyle(formulario.esIngreso ? Paleta.ingreso : Paleta.tinta)
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
            }
            Text(TextosDeNuevoMovimiento.shared.hoy())
                .font(.outfit(12.5, relativeTo: .caption))
                .foregroundStyle(Paleta.apagado)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .accessibilityElement(children: .combine)
    }
}

/// «Nota» y «Pagado con» juntas en una tarjeta, como una lista de Ajustes de iOS.
private struct NotaYFuente: View {
    let formulario: FormularioNuevoMovimiento
    let alEscribir: (String) -> Void
    let alCambiarFuente: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Image(systemName: "textformat").foregroundStyle(Paleta.marcaTinta).frame(width: 22)
                Text(TextosDeNuevoMovimiento.shared.NOTA).font(.outfit(16)).foregroundStyle(Paleta.tinta)
                TextField(formulario.pistaDelConcepto, text: Binding(get: { formulario.concepto }, set: alEscribir))
                    .font(.outfit(16))
                    .foregroundStyle(Paleta.tinta)
                    .multilineTextAlignment(.trailing)
                    .submitLabel(.done)
                    // Son nombres de comercios y personas: la autocorrección los cambia (lo vimos: «Segundo gasto» pasó a otra frase).
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.sentences)
            }
            .padding(.horizontal, 16)
            .frame(minHeight: 48)
            Divider().overlay(Paleta.linea).padding(.leading, 50)
            Button(action: alCambiarFuente) {
                HStack(spacing: 12) {
                    Circle().fill(colorDeLaFuente(formulario.fuente)).frame(width: 8, height: 8).frame(width: 22)
                    Text(formulario.prefijoDeLaFuente).font(.outfit(16)).foregroundStyle(Paleta.tinta)
                    Spacer()
                    Text(FormularioNuevoMovimiento.companion.etiquetaDeLaFuente(fuente: formulario.fuente))
                        .font(.outfit(16)).foregroundStyle(Paleta.marcaTinta)
                    Image(systemName: "chevron.right").font(.system(size: 12, weight: .semibold)).foregroundStyle(Paleta.apagado)
                }
                .padding(.horizontal, 16)
                .frame(minHeight: 48)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("\(formulario.prefijoDeLaFuente) \(FormularioNuevoMovimiento.companion.etiquetaDeLaFuente(fuente: formulario.fuente)). \(TextosDeNuevoMovimiento.shared.CAMBIAR)")
        }
        .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 14))
    }
}

/// Una categoría en la cuadrícula: el ícono arriba y el nombre abajo.
private struct CeldaDeCategoria: View {
    let texto: String
    let icono: String
    let elegido: Bool
    let esIngreso: Bool
    let alTocar: () -> Void

    var body: some View {
        let acento = esIngreso ? Paleta.ingreso : Paleta.marcaTinta
        Button(action: alTocar) {
            VStack(spacing: 4) {
                Image(systemName: icono).font(.system(size: 18)).frame(height: 22)
                Text(texto).font(.outfit(12.5, .medium, relativeTo: .caption)).lineLimit(1).minimumScaleFactor(0.8)
            }
            .foregroundStyle(elegido ? acento : Paleta.tinta)
            .frame(maxWidth: .infinity, minHeight: 58)
            .background(elegido ? acento.opacity(0.14) : Paleta.superficie, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(elegido ? acento : Paleta.linea, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(elegido ? .isSelected : [])
    }
}

/// El teclado numérico, con las teclas sin fondo (como en el diseño).
private struct Teclado: View {
    let alPresionar: (String) -> Void
    private let columnas = Array(repeating: GridItem(.flexible(), spacing: 0), count: 3)

    var body: some View {
        LazyVGrid(columns: columnas, spacing: 0) {
            ForEach(TextosDeNuevoMovimiento.shared.TECLAS, id: \.self) { tecla in
                Button { alPresionar(tecla) } label: {
                    Group {
                        if tecla == "<" {
                            Image(systemName: "delete.left").font(.system(size: 20, weight: .medium))
                        } else {
                            Text(tecla).font(.outfit(24, .medium, relativeTo: .title2))
                        }
                    }
                    .foregroundStyle(Paleta.tinta)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tecla == "<" ? "Borrar" : tecla == "." ? "Punto" : tecla)
            }
        }
    }
}
