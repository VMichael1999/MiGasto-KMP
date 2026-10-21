import Charts
import Shared
import SwiftUI

/// La pantalla Reportes (mg-05): mes, año o rango, comparación con el período anterior, gráfico de barras y tablas
/// por categoría. El reporte, sus textos y la comparación los arma `shared`; aquí solo se dibuja.
struct ReportesView: View {
    let aplicacion: Aplicacion
    @State private var modelo: ReportesModelo
    @State private var eligiendoRango = false

    init(aplicacion: Aplicacion) {
        self.aplicacion = aplicacion
        _modelo = State(initialValue: ReportesModelo(base: aplicacion.base))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                if let p = modelo.pantalla {
                    VStack(alignment: .leading, spacing: 16) {
                        SelectorDeModo(pantalla: p) { modelo.seguimiento.elegirModo(modo: $0) }
                        NavegadorDePeriodo(
                            pantalla: p,
                            alDesplazar: { modelo.seguimiento.desplazar(delta: $0) },
                            alCambiarRango: { eligiendoRango = true }
                        )
                        Saldo(pantalla: p, oculto: aplicacion.montosOcultos)
                        Grafico(reporte: p.reporte)
                        Tabla(titulo: p.reporte.tituloDeIngresos, vacio: p.reporte.textoSinIngresos, filas: p.reporte.ingresosPorCategoria, oculto: aplicacion.montosOcultos)
                        Tabla(titulo: p.reporte.tituloDeGastos, vacio: p.reporte.textoSinGastos, filas: p.reporte.gastosPorCategoria, oculto: aplicacion.montosOcultos)
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                }
            }
            .background(Paleta.fondo)
            .navigationTitle(TextosDeReportes.shared.TITULO)
            .sheet(isPresented: $eligiendoRango) {
                if let p = modelo.pantalla {
                    HojaDeRango(desde: p.desde, hasta: p.hasta) { modelo.seguimiento.elegirRango(desde: $0, hasta: $1) }
                }
            }
        }
        .task { await modelo.observar() }
    }
}

private struct SelectorDeModo: View {
    let pantalla: PantallaDeReportes
    let alElegir: (ModoDeReporte) -> Void

    var body: some View {
        HStack(spacing: 2) {
            ForEach(pantalla.opcionesDeModo, id: \.modo) { opcion in
                let elegido = pantalla.modo == opcion.modo
                Button { alElegir(opcion.modo) } label: {
                    Text(opcion.texto)
                        .font(.outfit(14.5, .semibold))
                        .foregroundStyle(Paleta.tinta)
                        .frame(maxWidth: .infinity, minHeight: 36)
                        .background(elegido ? Paleta.superficie : Color.clear, in: Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(elegido ? .isSelected : [])
            }
        }
        .padding(3)
        .background(Paleta.linea.opacity(0.7), in: Capsule())
    }
}

private struct NavegadorDePeriodo: View {
    let pantalla: PantallaDeReportes
    let alDesplazar: (Int32) -> Void
    let alCambiarRango: () -> Void

    var body: some View {
        HStack {
            Flecha(icono: "chevron.left", etiqueta: TextosDeReportes.shared.PERIODO_ANTERIOR, activa: pantalla.puedeRetroceder) { alDesplazar(-1) }
            Spacer()
            if pantalla.modo == .rango {
                Button(action: alCambiarRango) {
                    HStack(spacing: 6) {
                        Text(pantalla.reporte.etiquetaDelPeriodo).font(.outfit(16, .semibold)).foregroundStyle(Paleta.tinta)
                        Text(TextosDeReportes.shared.CAMBIAR).font(.outfit(13, .semibold)).foregroundStyle(Paleta.apagado)
                    }
                }
                .buttonStyle(.plain)
            } else {
                Text(pantalla.reporte.etiquetaDelPeriodo).font(.outfit(16, .semibold)).foregroundStyle(Paleta.tinta)
            }
            Spacer()
            Flecha(icono: "chevron.right", etiqueta: TextosDeReportes.shared.PERIODO_SIGUIENTE, activa: pantalla.puedeAvanzar) { alDesplazar(1) }
        }
    }
}

private struct Flecha: View {
    let icono: String
    let etiqueta: String
    let activa: Bool
    let alTocar: () -> Void

    var body: some View {
        Button(action: alTocar) {
            Image(systemName: icono)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(activa ? Paleta.tinta : Paleta.apagado.opacity(0.4))
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!activa)
        .accessibilityLabel(etiqueta)
    }
}

private struct Saldo: View {
    let pantalla: PantallaDeReportes
    let oculto: Bool

    var body: some View {
        let r = pantalla.reporte
        let negativo = r.saldoNegativo && !oculto
        VStack(alignment: .leading, spacing: 4) {
            Text(r.textoDelSaldo).font(.outfit(13.5, .medium)).foregroundStyle(Paleta.apagado)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                if oculto {
                    Text("••••").font(.outfit(38, .bold, relativeTo: .largeTitle)).foregroundStyle(Paleta.tinta)
                } else {
                    Text(negativo ? "− S/" : "S/")
                        .font(.outfit(18, .medium, relativeTo: .title3))
                        .foregroundStyle(negativo ? Paleta.presupuestoPasado : Paleta.apagado)
                    Text(formatearMonto(monto: abs(r.saldo), decimales: 2))
                        .font(.outfit(38, .bold, relativeTo: .largeTitle).monospacedDigit())
                        .foregroundStyle(negativo ? Paleta.presupuestoPasado : Paleta.tinta)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                }
            }
            if let texto = oculto ? pantalla.textoDeLaComparacionOculta : pantalla.textoDeLaComparacion {
                Text(texto).font(.outfit(12.5, relativeTo: .caption)).foregroundStyle(Paleta.apagado)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct Grafico: View {
    let reporte: Reporte

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 16) {
                Leyenda(color: Paleta.ingreso, texto: TextosDeReportes.shared.INGRESOS)
                Leyenda(color: Paleta.barraDeGastos, texto: TextosDeReportes.shared.GASTOS)
            }
            Chart {
                ForEach(reporte.cubos, id: \.mes) { cubo in
                    BarMark(x: .value("Mes", cubo.etiqueta), y: .value("Monto", cubo.ingresos))
                        .position(by: .value("Tipo", TextosDeReportes.shared.INGRESOS))
                        .foregroundStyle(Paleta.ingreso)
                    BarMark(x: .value("Mes", cubo.etiqueta), y: .value("Monto", cubo.gastos))
                        .position(by: .value("Tipo", TextosDeReportes.shared.GASTOS))
                        .foregroundStyle(Paleta.barraDeGastos)
                }
            }
            .chartYScale(domain: 0...reporte.topDelEje)
            .chartYAxis {
                AxisMarks(position: .leading) { _ in
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 1, dash: [4, 4])).foregroundStyle(Paleta.linea)
                    AxisValueLabel().font(.outfit(11, relativeTo: .caption2)).foregroundStyle(Paleta.apagado)
                }
            }
            .chartXAxis {
                AxisMarks { _ in AxisValueLabel().font(.outfit(12, relativeTo: .caption)).foregroundStyle(Paleta.apagado) }
            }
            .frame(height: 200)
            .accessibilityElement()
            .accessibilityLabel(reporte.descripcionDelGrafico)
        }
    }
}

private struct Leyenda: View {
    let color: Color
    let texto: String

    var body: some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 3).fill(color).frame(width: 10, height: 10)
            Text(texto).font(.outfit(12.5, relativeTo: .caption)).foregroundStyle(Paleta.apagado)
        }
    }
}

private struct Tabla: View {
    let titulo: String
    let vacio: String
    let filas: [FilaDeReporte]
    let oculto: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(titulo).font(.outfit(16, .semibold, relativeTo: .headline)).foregroundStyle(Paleta.tinta)
            if filas.isEmpty {
                Text(vacio).font(.outfit(14)).foregroundStyle(Paleta.apagado)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(filas.enumerated()), id: \.element.clave) { indice, fila in
                        if indice > 0 { Divider().overlay(Paleta.linea) }
                        HStack {
                            Text(fila.etiqueta).font(.outfit(15)).foregroundStyle(Paleta.tinta)
                            Spacer()
                            Text(oculto ? "••••" : formatearSoles(monto: fila.monto, decimales: 2))
                                .font(.outfit(15, .semibold).monospacedDigit())
                                .foregroundStyle(Paleta.tinta)
                        }
                        .padding(.vertical, 11)
                        .accessibilityElement(children: .combine)
                    }
                }
            }
        }
    }
}

/// El rango de fechas: dos selectores y «Aceptar». Las fechas viajan a `shared` como texto `yyyy-MM-dd`.
private struct HojaDeRango: View {
    let alAceptar: (String, String) -> Void
    @Environment(\.dismiss) private var cerrar
    @State private var desde: Date
    @State private var hasta: Date

    private static let formato: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    init(desde: String, hasta: String, alAceptar: @escaping (String, String) -> Void) {
        self.alAceptar = alAceptar
        _desde = State(initialValue: Self.formato.date(from: desde) ?? .now)
        _hasta = State(initialValue: Self.formato.date(from: hasta) ?? .now)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            DatePicker("Desde", selection: $desde, in: ...hasta, displayedComponents: .date)
            DatePicker("Hasta", selection: $hasta, in: desde..., displayedComponents: .date)
            HStack {
                Button(TextosDeReportes.shared.CANCELAR) { cerrar() }
                Spacer()
                Button(TextosDeReportes.shared.ACEPTAR) {
                    alAceptar(Self.formato.string(from: desde), Self.formato.string(from: hasta))
                    cerrar()
                }
                .fontWeight(.semibold)
            }
            .tint(Paleta.marcaTinta)
            Spacer()
        }
        .font(.outfit(16))
        .padding(20)
        .background(Paleta.fondo)
        .presentationDetents([.height(240)])
    }
}
