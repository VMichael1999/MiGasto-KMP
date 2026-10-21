import Shared
import SwiftUI

/// La pantalla Resumen (mg-01). Solo dibuja: los montos, los textos y la lista los arma `shared`.
struct ResumenView: View {
    @Environment(Aplicacion.self) private var aplicacion
    @State private var modelo = ResumenModelo()

    var body: some View {
        NavigationStack {
            ScrollView {
                if let p = modelo.pantalla {
                    VStack(alignment: .leading, spacing: 14) {
                        TituloConMeses(pantalla: p) { modelo.mes = $0 }
                        Saldo(pantalla: p, oculto: aplicacion.montosOcultos)
                        IngresosYGastos(pantalla: p, oculto: aplicacion.montosOcultos)
                        if let texto = p.porConfirmarTexto {
                            PorConfirmar(texto: texto, monto: aplicacion.montosOcultos ? "••••" : p.porConfirmarMonto)
                        }
                        if let presupuesto = p.presupuesto {
                            PresupuestoView(presupuesto: presupuesto, oculto: aplicacion.montosOcultos)
                        }
                        ListaDelDia(pantalla: p, oculto: aplicacion.montosOcultos)
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                }
            }
            .background(Paleta.fondo)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        aplicacion.montosOcultos.toggle()
                    } label: {
                        Image(systemName: aplicacion.montosOcultos ? "eye.slash" : "eye")
                    }
                    .accessibilityLabel(aplicacion.montosOcultos ? "Mostrar montos" : "Ocultar montos")
                }
            }
            .toolbarTitleDisplayMode(.inline)
        }
        // Se vuelve a leer cada vez que cambia el mes elegido; dentro, shared emite cuando cambian los movimientos.
        .task(id: modelo.claveDelMes) { await modelo.observar(aplicacion.base) }
    }
}

private struct TituloConMeses: View {
    let pantalla: PantallaDelResumen
    let alElegir: (MesElegido) -> Void

    var body: some View {
        Menu {
            ForEach(pantalla.meses, id: \.nombre) { opcion in
                Button(opcion.nombre) { alElegir(opcion.mes) }
            }
        } label: {
            HStack(spacing: 6) {
                Text(pantalla.mes)
                    .font(.outfit(34, .bold, relativeTo: .largeTitle))
                    .foregroundStyle(Paleta.tinta)
                Image(systemName: "chevron.down")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Paleta.apagado)
            }
        }
        .disabled(pantalla.meses.count < 2)
        .accessibilityLabel("Mes: \(pantalla.mes). Elegir otro mes")
        .padding(.top, 2)
    }
}

private struct Saldo: View {
    let pantalla: PantallaDelResumen
    let oculto: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(pantalla.textoDelSaldo)
                .font(.outfit(14))
                .foregroundStyle(Paleta.apagado)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                if !oculto {
                    Text("S/")
                        .font(.outfit(18, .medium, relativeTo: .title3))
                        .foregroundStyle(Paleta.apagado)
                }
                Text(oculto ? "••••" : pantalla.saldo)
                    .font(.outfit(46, .bold, relativeTo: .largeTitle))
                    .foregroundStyle(Paleta.tinta)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct IngresosYGastos: View {
    let pantalla: PantallaDelResumen
    let oculto: Bool

    var body: some View {
        HStack(spacing: 12) {
            Tarjeta(titulo: "Ingresos", icono: "arrow.up", monto: oculto ? "••••" : pantalla.ingresos, color: Paleta.ingreso)
            Tarjeta(titulo: "Gastos", icono: "arrow.down", monto: oculto ? "••••" : pantalla.gastos, color: Paleta.apagado)
        }
    }

    private struct Tarjeta: View {
        let titulo: String
        let icono: String
        let monto: String
        let color: Color

        var body: some View {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Image(systemName: icono).font(.system(size: 12, weight: .semibold)).foregroundStyle(color)
                    Text(titulo).font(.outfit(13)).foregroundStyle(Paleta.apagado)
                }
                Text(monto)
                    .font(.outfit(18, .semibold, relativeTo: .title3))
                    .foregroundStyle(Paleta.tinta)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 16))
            .accessibilityElement(children: .combine)
        }
    }
}

private struct PorConfirmar: View {
    let texto: String
    let monto: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "checkmark.circle").foregroundStyle(Paleta.ingreso)
            Text("\(Text(texto).fontWeight(.semibold)) por confirmar")
                .font(.outfit(14))
                .foregroundStyle(Paleta.ingreso)
            Spacer()
            Text(monto).font(.outfit(14, .semibold)).foregroundStyle(Paleta.ingreso)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Paleta.ingresoSuave, in: RoundedRectangle(cornerRadius: 14))
        .accessibilityElement(children: .combine)
    }
}

private struct PresupuestoView: View {
    let presupuesto: PresupuestoDelResumen
    let oculto: Bool

    private var color: Color {
        switch presupuesto.estado {
        case .bien: return Paleta.presupuestoBien
        case .cerca: return Paleta.presupuestoCerca
        case .pasado: return Paleta.presupuestoPasado
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: presupuesto.estado == .bien ? "checkmark.circle" : "exclamationmark.circle")
                    .foregroundStyle(color)
                Text(presupuesto.titulo).font(.outfit(14, .semibold)).foregroundStyle(color)
                Spacer()
                Text(oculto ? "••••" : presupuesto.detalle).font(.outfit(12.5)).foregroundStyle(Paleta.apagado)
            }
            ProgressView(value: Double(presupuesto.relleno))
                .tint(color)
                .scaleEffect(x: 1, y: 1.6)
        }
        .padding(14)
        .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}

private struct ListaDelDia: View {
    let pantalla: PantallaDelResumen
    let oculto: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(pantalla.tituloDeLaLista)
                .font(.outfit(17, .semibold, relativeTo: .headline))
                .foregroundStyle(Paleta.tinta)
            if pantalla.hoy.isEmpty {
                Text(pantalla.textoSinMovimientos)
                    .font(.outfit(14))
                    .foregroundStyle(Paleta.apagado)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(pantalla.hoy.enumerated()), id: \.element.id) { indice, fila in
                        if indice > 0 { Divider().overlay(Paleta.linea) }
                        FilaDeMovimientoView(fila: fila, oculto: oculto)
                    }
                }
                .background(Paleta.superficie, in: RoundedRectangle(cornerRadius: 16))
            }
        }
    }
}
