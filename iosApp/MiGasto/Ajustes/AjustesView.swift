import Shared
import SwiftUI
import UniformTypeIdentifiers

/// La pantalla Ajustes: registro automático, dónde pagaste y tus datos (presupuesto, listas, CSV y respaldo cifrado).
/// Los textos y las acciones los arma `shared` (los mismos que usa Android); aquí solo se dibuja.
struct AjustesView: View {
    let aplicacion: Aplicacion
    @State private var modelo: AjustesModelo
    @State private var hoja: Hoja?
    @State private var borrandoUbicaciones = false
    @State private var pidiendoContrasena: ContrasenaPara?
    @State private var eligiendoArchivo = false

    enum Hoja: Identifiable {
        case presupuesto, aprendidas, propias, alias
        var id: Self { self }
    }

    enum ContrasenaPara: Identifiable {
        case crear, abrir
        var id: Self { self }
    }

    init(aplicacion: Aplicacion) {
        self.aplicacion = aplicacion
        _modelo = State(initialValue: AjustesModelo(base: aplicacion.base, registro: aplicacion.registro))
    }

    /// Para activar el bloqueo hay que confirmar con Face ID (o el código): así se sabe que el usuario podrá desbloquear.
    private func alternarBloqueo(activado: Bool) {
        if activado {
            modelo.seguimiento.establecerBloqueo(activado: false)
            aplicacion.bloqueoActivado = false
            return
        }
        let autenticador = Autenticador()
        switch decidirAlActivar(telefonoSoportaBloqueo: autenticador.disponible()) {
        case .explicar:
            aplicacion.avisar(TextosDelBloqueo.shared.SIN_BLOQUEO_EN_EL_TELEFONO, segundos: 4)
        case .pedirConfirmacion:
            Task {
                let correcta = await autenticador.autenticar(motivo: TextosDelBloqueo.shared.MOTIVO_AL_ACTIVAR)
                if activarSiConfirmo(correcta: correcta) {
                    modelo.seguimiento.establecerBloqueo(activado: true)
                    aplicacion.bloqueoActivado = true
                }
            }
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                if let p = modelo.pantalla {
                    VStack(alignment: .leading, spacing: 0) {
                        Etiqueta(TextosDeLaPantallaDeAjustes.shared.SECCION_REGISTRO)
                        Caja {
                            FilaDeAjuste(
                                simbolo: "lock", titulo: "Monto en pantalla bloqueada", subtitulo: p.subtituloDelMontoEnPantallaBloqueada,
                                interruptor: p.montoEnPantallaBloqueada
                            ) { modelo.seguimiento.alternarMontoEnPantallaBloqueada() }
                            Separador()
                            FilaDeAjuste(
                                simbolo: "arrow.down.circle", titulo: "Guardar ingresos automáticamente",
                                subtitulo: p.subtituloDeLosIngresosAutomaticos, interruptor: p.ingresosAutomaticos
                            ) { modelo.seguimiento.alternarIngresosAutomaticos() }
                            ForEach(p.proveedores, id: \.clave) { proveedor in
                                Separador()
                                FilaDeProveedor(proveedor: proveedor) { modelo.seguimiento.alternarProveedor(clave: proveedor.clave) }
                            }
                        }

                        Etiqueta(TextosDeLaPantallaDeAjustes.shared.SECCION_UBICACION)
                        Caja {
                            FilaDeAjuste(
                                simbolo: "mappin.and.ellipse", titulo: "Borrar todas las ubicaciones", subtitulo: "Los movimientos se mantienen"
                            ) { borrandoUbicaciones = true }
                        }

                        Etiqueta(TextosDeLaPantallaDeAjustes.shared.SECCION_DATOS)
                        Caja {
                            FilaDeAjuste(simbolo: "wallet.pass", titulo: "Presupuesto de gastos", valor: p.textoDelPresupuesto) { hoja = .presupuesto }
                            Separador()
                            FilaDeAjuste(
                                simbolo: "tag", titulo: "Aprendidas de tus cambios", subtitulo: "Categorías que la app ya recuerda por comercio"
                            ) { hoja = .aprendidas }
                            Separador()
                            FilaDeAjuste(simbolo: "tag", titulo: "Mis categorías", subtitulo: "Las que creaste, con tu ícono") { hoja = .propias }
                            Separador()
                            FilaDeAjuste(simbolo: "tag", titulo: "Nombres guardados", subtitulo: "Cómo ves a cada persona o comercio") { hoja = .alias }
                            Separador()
                            FilaDeAjuste(simbolo: "doc.text", titulo: "Exportar a CSV", subtitulo: "Guarda tus movimientos, sin ubicaciones") {
                                modelo.prepararCsv()
                            }
                            Separador()
                            FilaDeAjuste(simbolo: "lock", titulo: "Respaldo cifrado", subtitulo: p.subtituloDelRespaldo) { pidiendoContrasena = .crear }
                            Separador()
                            FilaDeAjuste(
                                simbolo: "faceid", titulo: "Bloqueo con Face ID o código", subtitulo: "Usa el desbloqueo de tu iPhone",
                                interruptor: p.bloqueoActivado
                            ) { alternarBloqueo(activado: p.bloqueoActivado) }
                            Separador()
                            FilaDeAjuste(simbolo: "doc", titulo: "Restaurar un respaldo", subtitulo: "Desde un archivo .mgb, con tu contraseña") {
                                eligiendoArchivo = true
                            }
                        }
                        Text(TextosDeLaPantallaDeAjustes.shared.PIE)
                            .font(.outfit(12.5)).foregroundStyle(Paleta.apagado).padding(.leading, 4).padding(.top, 12)
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                }
            }
            .background(Paleta.fondo)
            .navigationTitle(TextosDeLaPantallaDeAjustes.shared.TITULO)
        }
        .task { await modelo.observar() }
        .sheet(item: $hoja) { hoja in
            if let p = modelo.pantalla {
                switch hoja {
                case .presupuesto:
                    HojaDePresupuesto(inicial: p.presupuestoParaEditar) { monto in
                        aplicacion.avisar(modelo.seguimiento.guardarPresupuesto(monto: monto))
                    }
                case .aprendidas:
                    HojaDeLista(
                        titulo: "Aprendidas de tus cambios", ayuda: "Cuando cambias la categoría de un pago, la app lo recuerda para ese comercio.",
                        vacio: "Todavía no cambiaste ninguna categoría.", hayFilas: !p.aprendidas.isEmpty,
                        pie: ("Olvidar todas", { modelo.seguimiento.olvidarTodasLasAprendidas() })
                    ) {
                        ForEach(p.aprendidas, id: \.comercio) { a in
                            FilaDeLista(titulo: a.comercio, subtitulo: a.categoria, quitar: "Olvidar \(a.comercio)") {
                                modelo.seguimiento.olvidarAprendida(comercio: a.comercio)
                            }
                        }
                    }
                case .propias:
                    HojaDeLista(
                        titulo: "Mis categorías",
                        ayuda: "Las creas al registrar un movimiento, con \"Nueva\". Si borras una, sus movimientos pasan a verse como \"Otros\".",
                        vacio: "Aún no creaste ninguna.", hayFilas: !p.propias.isEmpty
                    ) {
                        ForEach(p.propias, id: \.id) { c in
                            FilaDeLista(
                                titulo: c.nombre, subtitulo: c.tipo == .ingreso ? "Ingreso" : "Gasto",
                                simbolo: simboloDeIconoPropio(c.icono), quitar: "Borrar \(c.nombre)"
                            ) { modelo.seguimiento.quitarPropia(id: c.id) }
                        }
                    }
                case .alias:
                    HojaDeLista(
                        titulo: "Nombres guardados", ayuda: "Para ponerle nombre a alguien, abre un movimiento suyo y toca \"De\" o \"Para\".",
                        vacio: "Aún no has guardado ninguno.", hayFilas: !p.aliases.isEmpty
                    ) {
                        ForEach(p.aliases, id: \.clave) { a in
                            FilaDeLista(titulo: a.clave, subtitulo: a.nombre, quitar: "Quitar el nombre de \(a.nombre)") {
                                modelo.seguimiento.quitarAlias(clave: a.clave)
                            }
                        }
                    }
                }
            }
        }
        .sheet(item: $pidiendoContrasena) { para in
            HojaDeContrasena(creando: para == .crear) { contrasena in
                Task {
                    let fallo = switch para {
                    case .crear: await modelo.crearRespaldo(contrasena: contrasena)
                    case .abrir: await modelo.abrirRespaldo(contrasena: contrasena)
                    }
                    if let fallo { aplicacion.avisar(fallo, segundos: 4) }
                }
            }
        }
        .alert("Borrar todas las ubicaciones", isPresented: $borrandoUbicaciones) {
            Button("Cancelar", role: .cancel) {}
            Button("Borrar", role: .destructive) {
                modelo.seguimiento.borrarUbicaciones()
                aplicacion.avisar("Ubicaciones borradas")
            }
        } message: {
            Text("Se borra dónde hiciste cada pago. Tus gastos e ingresos se mantienen.")
        }
        .alert("Restaurar respaldo", isPresented: Binding(get: { modelo.respaldoAbierto != nil }, set: { if !$0 { modelo.respaldoAbierto = nil } })) {
            Button("Cancelar", role: .cancel) { modelo.respaldoAbierto = nil }
            Button("Restaurar") {
                if let contenido = modelo.respaldoAbierto?.contenido {
                    aplicacion.avisar(modelo.seguimiento.restaurar(contenido: contenido))
                }
                modelo.respaldoAbierto = nil
            }
        } message: {
            Text(modelo.respaldoAbierto?.textoParaConfirmar ?? "")
        }
        .fileExporter(
            isPresented: Binding(get: { modelo.csvParaGuardar != nil }, set: { if !$0 { modelo.csvParaGuardar = nil } }),
            document: modelo.csvParaGuardar, contentType: .commaSeparatedText, defaultFilename: modelo.csvParaGuardar?.nombre
        ) { resultado in
            if case .success = resultado { aplicacion.avisar("Movimientos guardados") }
        }
        .fileExporter(
            isPresented: Binding(get: { modelo.respaldoParaGuardar != nil }, set: { if !$0 { modelo.respaldoParaGuardar = nil } }),
            document: modelo.respaldoParaGuardar, contentType: .data, defaultFilename: modelo.respaldoParaGuardar?.nombre
        ) { resultado in
            switch resultado {
            case .success:
                modelo.seguimiento.respaldoGuardado()
                aplicacion.avisar("Respaldo creado. Guárdalo en un lugar seguro (iCloud, correo, tu computadora).", segundos: 4)
            case .failure:
                aplicacion.avisar("No se pudo crear el respaldo.")
            }
        }
        .fileImporter(isPresented: $eligiendoArchivo, allowedContentTypes: [.data]) { resultado in
            guard case .success(let url) = resultado else { return }
            let acceso = url.startAccessingSecurityScopedResource()
            defer { if acceso { url.stopAccessingSecurityScopedResource() } }
            guard let datos = try? Data(contentsOf: url) else {
                aplicacion.avisar("No se pudo leer el archivo.")
                return
            }
            modelo.archivoParaAbrir = datos.base64EncodedString()
            pidiendoContrasena = .abrir
        }
        .overlay {
            if let mensaje = modelo.ocupado {
                ZStack {
                    Color.black.opacity(0.35).ignoresSafeArea()
                    HStack(spacing: 14) {
                        ProgressView()
                        Text(mensaje).font(.outfit(15)).foregroundStyle(Paleta.tinta)
                    }
                    .padding(22)
                    .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 16))
                }
            }
        }
    }
}

// MARK: - Piezas

private struct Etiqueta: View {
    let texto: String
    init(_ texto: String) { self.texto = texto }
    var body: some View {
        Text(texto).font(.outfit(13, .medium)).foregroundStyle(Paleta.apagado).padding(.leading, 4).padding(.top, 16).padding(.bottom, 8)
    }
}

private struct Caja<Contenido: View>: View {
    @ViewBuilder let contenido: Contenido
    var body: some View {
        VStack(spacing: 0) { contenido }
            .frame(maxWidth: .infinity)
            .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct Separador: View {
    var body: some View { Rectangle().fill(Paleta.linea).frame(height: 1) }
}

/// Una fila: ícono, título y subtítulo, y a la derecha un interruptor, un valor o una flecha.
private struct FilaDeAjuste: View {
    let simbolo: String
    let titulo: String
    var subtitulo: String?
    var valor: String?
    var interruptor: Bool?
    let alTocar: () -> Void

    init(simbolo: String, titulo: String, subtitulo: String? = nil, valor: String? = nil, interruptor: Bool? = nil, alTocar: @escaping () -> Void) {
        self.simbolo = simbolo
        self.titulo = titulo
        self.subtitulo = subtitulo
        self.valor = valor
        self.interruptor = interruptor
        self.alTocar = alTocar
    }

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: simbolo).font(.system(size: 17)).foregroundStyle(Paleta.apagado).frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(titulo).font(.outfit(16, .medium)).foregroundStyle(Paleta.tinta)
                if let subtitulo {
                    Text(subtitulo).font(.outfit(13)).foregroundStyle(Paleta.apagado).fixedSize(horizontal: false, vertical: true)
                }
            }
            Spacer(minLength: 8)
            if let interruptor {
                Toggle("", isOn: Binding(get: { interruptor }, set: { _ in alTocar() })).labelsHidden().tint(Paleta.presupuestoBien)
            } else if let valor {
                Text(valor).font(.outfit(15, .semibold)).foregroundStyle(Paleta.marcaTinta)
            } else {
                Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundStyle(Paleta.apagado)
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 13)
        .contentShape(Rectangle())
        .onTapGesture { if interruptor == nil { alTocar() } }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(interruptor == nil ? .isButton : [])
    }
}

private struct FilaDeProveedor: View {
    let proveedor: ProveedorDeAjuste
    let alCambiar: () -> Void

    private var color: Color {
        switch proveedor.clave {
        case "yape": Paleta.yape
        case "plin": Paleta.plin
        default: Paleta.googlePay
        }
    }

    var body: some View {
        HStack(spacing: 14) {
            Circle().fill(color).frame(width: 12, height: 12).frame(width: 24)
            Text(proveedor.nombre).font(.outfit(16, .medium)).foregroundStyle(Paleta.tinta)
            Spacer()
            Toggle("", isOn: Binding(get: { proveedor.activo }, set: { _ in alCambiar() })).labelsHidden().tint(Paleta.presupuestoBien)
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(proveedor.nombre)
    }
}

// MARK: - Hojas

private struct HojaDePresupuesto: View {
    let inicial: String
    let alGuardar: (Double) -> Void
    @State private var texto: String
    @Environment(\.dismiss) private var cerrar

    init(inicial: String, alGuardar: @escaping (Double) -> Void) {
        self.inicial = inicial
        self.alGuardar = alGuardar
        _texto = State(initialValue: inicial)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("S/").font(.outfit(20, .semibold)).foregroundStyle(Paleta.apagado)
                    TextField("Monto del mes", text: $texto).keyboardType(.decimalPad).font(.outfit(20, .semibold))
                }
                .padding(14).background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 14))
                if !inicial.isEmpty {
                    Button("Quitar presupuesto", role: .destructive) { alGuardar(0); cerrar() }.font(.outfit(15, .medium))
                }
                Spacer()
            }
            .padding(18)
            .background(Paleta.fondo)
            .navigationTitle("Presupuesto")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { cerrar() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") {
                        if let monto = parsearPresupuesto(texto: texto)?.doubleValue { alGuardar(monto); cerrar() }
                    }
                }
            }
        }
        .presentationDetents([.height(300)])
    }
}

private struct HojaDeLista<Filas: View>: View {
    let titulo: String
    let ayuda: String
    let vacio: String
    let hayFilas: Bool
    var pie: (String, () -> Void)?
    @ViewBuilder let filas: Filas
    @Environment(\.dismiss) private var cerrar

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(ayuda).font(.outfit(14)).foregroundStyle(Paleta.apagado)
                    if hayFilas {
                        Caja { filas }
                        if let pie {
                            Button(pie.0, role: .destructive) { pie.1() }.font(.outfit(15, .medium)).padding(.top, 4)
                        }
                    } else {
                        Text(vacio).font(.outfit(15)).foregroundStyle(Paleta.tinta).padding(.top, 8)
                    }
                }
                .padding(18)
            }
            .background(Paleta.fondo)
            .navigationTitle(titulo)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Listo") { cerrar() } } }
        }
    }
}

private struct FilaDeLista: View {
    let titulo: String
    let subtitulo: String
    var simbolo: String?
    let quitar: String
    let alQuitar: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            if let simbolo { Image(systemName: simbolo).foregroundStyle(Paleta.apagado).frame(width: 24) }
            VStack(alignment: .leading, spacing: 2) {
                Text(titulo).font(.outfit(16, .medium)).foregroundStyle(Paleta.tinta)
                Text(subtitulo).font(.outfit(13)).foregroundStyle(Paleta.apagado)
            }
            Spacer()
            Button(action: alQuitar) { Image(systemName: "trash").foregroundStyle(Paleta.presupuestoPasado) }
                .accessibilityLabel(quitar)
        }
        .padding(.horizontal, 16).padding(.vertical, 12)
    }
}

/// Pide la contraseña del respaldo (dos veces al crearlo). La hoja se cierra al aceptar: el trabajo largo lo muestra Ajustes.
private struct HojaDeContrasena: View {
    let creando: Bool
    let alAceptar: (String) -> Void
    @State private var primera = ""
    @State private var segunda = ""
    @State private var error: String?
    @Environment(\.dismiss) private var cerrar

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                if creando {
                    Text("Elige una contraseña que recuerdes. MiGasto no la guarda: si la olvidas, nadie podrá abrir el respaldo.")
                        .font(.outfit(14)).foregroundStyle(Paleta.apagado)
                }
                SecureField("Contraseña", text: $primera)
                    .padding(14).background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 14))
                if creando {
                    SecureField("Repite la contraseña", text: $segunda)
                        .padding(14).background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 14))
                }
                if let error { Text(error).font(.outfit(13)).foregroundStyle(Paleta.presupuestoPasado) }
                Spacer()
            }
            .font(.outfit(16))
            .padding(18)
            .background(Paleta.fondo)
            .navigationTitle("Contraseña")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { cerrar() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(creando ? "Crear respaldo" : "Abrir") { aceptar() }
                }
            }
            .onChange(of: primera) { error = nil }
            .onChange(of: segunda) { error = nil }
        }
        .presentationDetents([.height(creando ? 360 : 260)])
    }

    private func aceptar() {
        if let problema = validarContrasenaDeRespaldo(contrasena: primera, repetida: segunda, creando: creando) {
            error = problema
            return
        }
        alAceptar(primera)
        cerrar()
    }
}
