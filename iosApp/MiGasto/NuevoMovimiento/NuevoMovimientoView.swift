import Shared
import SwiftUI

/// La pantalla «Nuevo movimiento» (mg-03): teclado numérico, categorías, fuente y Guardar.
/// Las reglas (monto, categorías, textos) las pone `shared`; aquí solo se dibuja.
struct NuevoMovimientoView: View {
    @Environment(Aplicacion.self) private var aplicacion
    @Environment(\.dismiss) private var cerrar
    @State private var modelo = NuevoMovimientoModelo()
    @State private var eligiendoFuente = false
    @State private var creandoCategoria = false

    var body: some View {
        let f = modelo.formulario
        VStack(spacing: 0) {
            Cabecera(alCerrar: { cerrar() })
            ScrollView {
                VStack(spacing: 12) {
                    SelectorDeTipo(tipo: f.tipo) { modelo.formulario = modelo.formulario.conTipo(nuevo: $0) }
                    Monto(formulario: f)
                    ConceptoField(formulario: f) { modelo.formulario = modelo.formulario.conConcepto(texto: $0) }
                    FuenteFila(formulario: f) { eligiendoFuente = true }
                    FlowLayout {
                        ForEach(Array(f.chipsDeCategoria(propias: modelo.propias).enumerated()), id: \.offset) { _, opcion in
                            ChipDeCategoria(texto: opcion.etiqueta, icono: iconoDeCategoria(opcion.categoria, propio: opcion.propia?.icono), elegido: opcion.elegido, esIngreso: f.esIngreso) {
                                modelo.formulario = modelo.formulario.conChip(chip: opcion)
                            }
                        }
                        ChipDeCategoria(texto: TextosDeNuevoMovimiento.shared.NUEVA, icono: "plus", elegido: false, esIngreso: f.esIngreso) { creandoCategoria = true }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 6)
            }
            Teclado { modelo.formulario = modelo.formulario.presionando(tecla: $0) }
                .padding(.horizontal, 18)
                .padding(.top, 8)
            BotonGuardar(formulario: f) { guardar() }
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
        }
        .background(Paleta.fondo)
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

private struct Cabecera: View {
    let alCerrar: () -> Void

    var body: some View {
        ZStack {
            Text(TextosDeNuevoMovimiento.shared.TITULO)
                .font(.outfit(16.5, .semibold, relativeTo: .headline))
                .foregroundStyle(Paleta.tinta)
            HStack {
                Button(action: alCerrar) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Paleta.tinta)
                        .frame(width: 44, height: 44)
                        .background(Paleta.superficie, in: Circle())
                }
                .accessibilityLabel("Cerrar")
                Spacer()
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 14)
        .padding(.bottom, 8)
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

private struct ConceptoField: View {
    let formulario: FormularioNuevoMovimiento
    let alEscribir: (String) -> Void

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "tag").foregroundStyle(Paleta.apagado)
            TextField(formulario.pistaDelConcepto, text: Binding(get: { formulario.concepto }, set: alEscribir))
                .font(.outfit(16))
                .foregroundStyle(Paleta.tinta)
                .submitLabel(.done)
                // Son nombres de comercios y personas: la autocorrección los cambia (lo vimos: «Segundo gasto» pasó a otra frase).
                .autocorrectionDisabled()
                .textInputAutocapitalization(.sentences)
        }
        .padding(.horizontal, 14)
        .frame(minHeight: 50)
        .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 13))
    }
}

private struct FuenteFila: View {
    let formulario: FormularioNuevoMovimiento
    let alCambiar: () -> Void

    var body: some View {
        Button(action: alCambiar) {
            HStack(spacing: 10) {
                Circle().fill(colorDeLaFuente(formulario.fuente)).frame(width: 8, height: 8)
                Text("\(formulario.prefijoDeLaFuente) \(FormularioNuevoMovimiento.companion.etiquetaDeLaFuente(fuente: formulario.fuente))")
                    .font(.outfit(16))
                    .foregroundStyle(Paleta.tinta)
                Spacer()
                Text(TextosDeNuevoMovimiento.shared.CAMBIAR)
                    .font(.outfit(13, .semibold))
                    .foregroundStyle(Paleta.apagado)
                Image(systemName: "chevron.right").font(.system(size: 12, weight: .semibold)).foregroundStyle(Paleta.apagado)
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 50)
            .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 13))
        }
        .buttonStyle(.plain)
    }
}

private struct ChipDeCategoria: View {
    let texto: String
    let icono: String
    let elegido: Bool
    let esIngreso: Bool
    let alTocar: () -> Void

    var body: some View {
        let acento = esIngreso ? Paleta.ingreso : Paleta.marcaTinta
        Button(action: alTocar) {
            HStack(spacing: 6) {
                Image(systemName: icono).font(.system(size: 14))
                Text(texto).font(.outfit(13.5, .medium)).lineLimit(1)
            }
            .foregroundStyle(elegido ? acento : Paleta.tinta)
            .padding(.horizontal, 12)
            .frame(height: 38)
            .background(elegido ? acento.opacity(0.14) : Paleta.superficie, in: Capsule())
            .overlay(Capsule().stroke(elegido ? acento : Paleta.linea, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(elegido ? .isSelected : [])
    }
}

private struct Teclado: View {
    let alPresionar: (String) -> Void
    private let columnas = Array(repeating: GridItem(.flexible(), spacing: 8), count: 3)

    var body: some View {
        LazyVGrid(columns: columnas, spacing: 8) {
            ForEach(TextosDeNuevoMovimiento.shared.TECLAS, id: \.self) { tecla in
                Button { alPresionar(tecla) } label: {
                    Group {
                        if tecla == "<" {
                            Image(systemName: "delete.left").font(.system(size: 20, weight: .medium))
                        } else {
                            Text(tecla).font(.outfit(22, .medium, relativeTo: .title2))
                        }
                    }
                    .foregroundStyle(Paleta.tinta)
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 13))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tecla == "<" ? "Borrar" : tecla == "." ? "Punto" : tecla)
            }
        }
    }
}

private struct BotonGuardar: View {
    let formulario: FormularioNuevoMovimiento
    let alGuardar: () -> Void

    var body: some View {
        Button(action: alGuardar) {
            Text(formulario.textoDelBoton)
                .font(.outfit(16.5, .semibold))
                .foregroundStyle(formulario.puedeGuardar ? Paleta.tintaSobreVerde : Paleta.apagado)
                .frame(maxWidth: .infinity, minHeight: 54)
                .background(formulario.puedeGuardar ? Paleta.verdeNeon : Paleta.linea, in: RoundedRectangle(cornerRadius: 15))
        }
        .buttonStyle(.plain)
        .disabled(!formulario.puedeGuardar)
    }
}
