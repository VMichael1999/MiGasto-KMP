# Prompt para agente — MiGasto en Kotlin Multiplataforma + SwiftUI (v0.1)

> **Qué es esto:** rehacer MiGasto en un **repo nuevo**, `MiGasto-KMP`, con la lógica compartida en Kotlin y la interfaz nativa de cada sistema: **Jetpack Compose en Android y SwiftUI en iPhone**.
>
> El MiGasto de Flutter (https://github.com/VMichael1999/MiGasto) **no se toca**: es la referencia de comportamiento.
>
> **Archivo de referencia visual:** `migasto-kmp.html`. Cópialo a `docs/diseno/migasto-kmp.html`. Cada par de pantallas tiene un ID (`mg-01-resumen` … `mg-07-registro`) en `data-screen`.
>
> Revisa las líneas marcadas **(confirmar)**.

---

## Rol y objetivo

Actúa como un desarrollador móvil senior en Kotlin y Swift. Este repo es **portafolio**: lo va a leer alguien que contrata para Android (Kotlin) o iOS (Swift). Tiene que mostrar:

- Arquitectura limpia.
- Tests de verdad.
- CI.
- Código idiomático en cada lenguaje.

No basta con "una app que funciona".

## Reglas principales

1. **El comportamiento sale del MiGasto de Flutter:** reglas de lectura, duplicados, categorías, alias, presupuesto, respaldo y CSV. Antes de escribir cada parte, lee el código Dart y los tests equivalentes, y **porta los tests primero**.
2. **El aspecto sale del HTML y del tema actual:**
   - Tipografía **Outfit** (no cambiar). En Android con fuentes descargables o empaquetada; en iOS empaquetada en el bundle (revisa su licencia OFL).
   - Colores de `lib/core/theme/app_colors.dart` (claro y oscuro).
   - Radios y tamaños de `app_tokens.dart`.
3. **Los controles son los de cada sistema:**
   - **Android:** Material 3, barra inferior con agregar al centro.
   - **iPhone:**
     - Título grande.
     - `TabView`, `.searchable` y `List` agrupada.
     - Hojas con Cancelar y Guardar.
     - Regreso con el nombre de la pantalla anterior.

   Si un control del HTML no existe en un sistema, usa el equivalente nativo y dímelo.
4. **Textos en español (Perú) y montos `S/ 2,655.30`**, iguales a la app de Flutter.
5. **Si algo no está en el HTML ni en la app de Flutter, pregunta.**

---

## Estructura del repo

```text
MiGasto-KMP/
├── shared/                 # Kotlin Multiplatform
│   ├── commonMain/         # dominio, reglas, duplicados, repositorios, casos de uso, respaldo, CSV
│   ├── commonTest/         # tests portados de Flutter + nuevos
│   ├── androidMain/        # drivers y cifrado de Android (Keystore)
│   └── iosMain/            # drivers y cifrado de iOS (Keychain)
├── androidApp/             # Kotlin + Jetpack Compose
│   └── … notificaciones, accesibilidad, ventana flotante, avisos (portar de android/app/src/main/kotlin)
├── iosApp/                 # Swift + SwiftUI (proyecto de Xcode)
│   ├── MiGasto/            # app
│   ├── ShareExtension/     # capturas de Yape/Plin con Vision (portar de ios/ShareExtension)
│   ├── Intents/            # App Intents: «Registrar movimiento» para Atajos y Apple Pay
│   └── Widget/             # WidgetKit
├── docs/
└── .github/workflows/      # CI de shared, Android e iOS
```

### Decisiones técnicas (propuestas; confirma las versiones estables actuales antes de fijarlas)

| Tema | Propuesta | Por qué |
|---|---|---|
| Base de datos | SQLDelight con **SQLCipher** | Reemplaza Drift; la base sigue cifrada como hoy. **Riesgo:** en iOS hay que enlazar SQLCipher con el driver nativo. Haz primero una prueba mínima y dime si funciona. |
| Concurrencia | Corrutinas + `Flow` en `shared` | |
| Swift ↔ Kotlin | **SKIE** (Touchlab) | `Flow` como `AsyncSequence` y `suspend` como `async`. Si no convence, KMP-NativeCoroutines; dime cuál y por qué. |
| Inyección | Koin en `shared` y Android; en iOS, un contenedor simple que exponga los casos de uso | |
| Fechas y JSON | kotlinx-datetime y kotlinx-serialization | `reader_rules.json` y `category_rules.json` se leen desde `shared`, como hoy desde Kotlin y Dart. |
| Respaldo `.mgb` | AES-256-GCM y PBKDF2-SHA256 con 210 000 vueltas, **compatible byte a byte** con el de Flutter | Un respaldo hecho en la app de Flutter tiene que abrirse en esta, y al revés. Ponle test con un archivo real. |
| Gráficos | Vico en Compose y Swift Charts en iOS | |
| Mapas | Google Maps Compose en Android y MapKit en iOS | |
| Biometría | BiometricPrompt en Android y LocalAuthentication (Face ID) en iOS | Nativo en cada lado; no uses librerías de la comunidad. |
| Estado de pantallas | ViewModel con `StateFlow` en Android y `@Observable` en iOS, ambos leyendo los mismos casos de uso | |

**No instales nada fuera de esta tabla sin mi OK.**

---

## Barra de pestañas en iPhone: Liquid Glass nativo

Quiero la barra flotante de vidrio de iOS 26 (la de Apple Music): una cápsula de vidrio con las 4 pestañas, la pestaña elegida dentro de una "lente" de vidrio con borde que refracta, y la lupa en un círculo aparte. Así está dibujada en el HTML.

- **No uses librerías ni la dibujes a mano.** En SwiftUI es el `TabView` del sistema: al compilar con el SDK de iOS 26 o posterior, toma el diseño Liquid Glass solo, con la lente, el arrastre entre pestañas y la adaptación a lo que hay detrás.
- `liquidum` (pub.dev) es una imitación hecha en Flutter con shaders; aquí no hace falta.
- **Código:**
  - `TabView` con la API nueva de `Tab`: Resumen, Movimientos, Reportes, Ajustes y `Tab(role: .search)` para buscar movimientos. El sistema la separa en el círculo de la derecha.
  - `.tint(brandInk)`: verde `#1E7A36` en claro y `#8CE885` en oscuro, para la pestaña elegida.
  - `.tabBarMinimizeBehavior(.onScrollDown)` en Movimientos y Reportes, para que la barra se achique al bajar.
  - **Agregar movimiento:** botón redondo flotante de 56 pt, abajo a la derecha, 12 pt encima del círculo de la lupa, en las 4 pestañas (como el + central de Android).
    - Vidrio del sistema con `.glassEffect(.regular.tint(brandSoft).interactive(), in: .circle)`, ícono `plus` en `brandInk`.
    - Abre la hoja "Nuevo movimiento".
    - Se oculta cuando la barra se minimiza al hacer scroll y vuelve con ella.
    - Etiqueta de accesibilidad: "Agregar movimiento".
    - Confirma el nombre exacto de la API en el SDK; si no existe así, dime qué usas.
  - **Una sola búsqueda:** la lupa (`Tab(role: .search)`) busca en todos los movimientos por nombre, alias, fuente y monto. **Movimientos no lleva `.searchable` propio.**
- **Versiones viejas:** en iOS 17 a 25 el mismo código muestra la barra clásica, sin escribir nada aparte. **No imites el vidrio en iOS viejos.**
- **Tipografía:** las etiquetas de la barra usan la letra del sistema (SF), no Outfit. Es lo esperado en un control del sistema. **No lo fuerces.**
- **Accesibilidad:** con "Reducir transparencia" y "Aumentar contraste" el sistema ajusta el vidrio solo. Pruébalo y pon las capturas en `docs/comparacion/`.
- **Android no cambia:** Material 3, con la barra y el botón central. No le pongas vidrio.

## Pantallas (ver el HTML)

| ID | Android (Compose) | iPhone (SwiftUI) |
|---|---|---|
| `mg-01-resumen` | Barra superior con el mes, ojo y lupa; barra inferior con agregar | Título grande con el mes; ojo y lupa en la barra; `TabView` Liquid Glass con 4 pestañas y búsqueda aparte |
| `mg-02-movimientos` | Chips de filtro | `Picker` segmentado; la búsqueda es la lupa de la barra |
| `mg-03-nuevo` | Pantalla completa, Guardar abajo | Hoja (`.sheet`) con Cancelar y Guardar arriba |
| `mg-04-detalle` | Flecha atrás y Editar | `NavigationStack`, regreso con nombre |
| `mg-05-reportes` | Vico | Swift Charts |
| `mg-06-ajustes` | Lectura de notificaciones, accesibilidad, ventana flotante | Capturas compartidas, Atajos y Apple Pay, Face ID |
| `mg-07-registro` | Ventana flotante con la cuenta regresiva en Guardar | Extensión para compartir que lee la captura con Vision |

Las demás pantallas de la app de Flutter (bienvenida, filtros, categorías propias, alias, respaldo, mapa) **no están en el HTML**: tómalas de las capturas de `MiGasto/docs/screenshots` y adapta los controles a cada sistema. **Muéstramelas antes de terminarlas.**

---

## Fases (avanza en orden y espera mi OK entre fases)

### Fase 1 — `shared` con tests (lo más importante)

1. Entidades y reglas del dominio: movimiento, categorías, categorías propias, alias, duplicados.
2. Lector de reglas desde los JSON.
3. Base de datos cifrada con SQLDelight, con migraciones desde el día uno.
4. Respaldo `.mgb` compatible con Flutter, y CSV.
5. **Porta los tests de Flutter** (hoy son 118 de Flutter y 9 nativos) a `commonTest` y súmales los que falten. Meta: cobertura ≥ 80 % en `shared` (confirmar).
6. CI que corra `./gradlew :shared:allTests`.

### Fase 2 — Android completo (debe quedar presentable por sí solo)

1. Pantallas en Compose según el HTML, claro y oscuro.
2. Porta la capa nativa actual (`PaymentNotificationListener`, `MyAccessibilityService`, `OverlayService`, `PaymentNotifier`…): ahora escribe directo en `shared` y **ya no hace falta la cola** `NativeQueue`.
3. Tests de UI con Compose Testing en los flujos principales.
4. CI con build, tests y lint (ktlint o detekt).
5. README con capturas y la explicación de la arquitectura.

### Fase 3 — iPhone

1. Pantallas en SwiftUI según el HTML, claro y oscuro.
2. Porta la extensión para compartir (Vision), la acción de Atajos con App Intents y el widget.
3. Tests con XCTest o Swift Testing para la capa de Swift.
4. CI en `macos-latest` con `xcodebuild test`.
5. Si llega a tiempo: adapta al iPhone Duo con lo que ya documentamos para SaveReel y Run Rider (confirmar).

---

## Verificación (en cada pantalla terminada)

1. Capturas de Android y iPhone al lado de la misma pantalla del HTML, en `docs/comparacion/`.
2. Mismos datos de ejemplo que el HTML. Los montos tienen que cuadrar:

   | Concepto | Monto |
   |---|---|
   | Ingresos confirmados | S/ 2,800.00 |
   | Gastos | S/ 144.70 |
   | Quedan | S/ 2,655.30 |
   | Por confirmar | S/ 230.00 |
   | Presupuesto (10 % usado) | S/ 1,500 |

3. Ningún monto calculado en la vista: todo sale de casos de uso de `shared`.
4. Accesibilidad:
   - Android: TalkBack y texto al 200 %.
   - iPhone: VoiceOver y Dynamic Type `xxxLarge`.
5. Dime explícitamente **qué no quedó igual** y **qué no hace todavía** comparado con la app de Flutter.

## Límites

- Repo nuevo. **No modifiques** el MiGasto de Flutter; solo léelo.
- **Nada de servidor ni cuentas:** todo queda en el teléfono, como hoy. Respeta la política de privacidad actual (`docs/politica-de-privacidad.md`).
- Commits pequeños en Conventional Commits en español. Rama `main` protegida con PR.

## Preguntas abiertas (pregúntame, no decidas)

1. ¿SKIE o KMP-NativeCoroutines?
2. ¿Qué pasa si SQLCipher en iOS no funciona bien con SQLDelight? ¿Se prueba Room KMP?
3. ¿Mínimos de sistema? Propuesta: Android 7.0 (API 24), como hoy; iOS 17 por `@Observable` y Swift Charts. El vidrio de la barra se ve solo en iOS 26 o superior; en 17–25 la barra es la clásica. Si quieres el vidrio en todos los iPhone, el mínimo sube a iOS 26.
4. ¿Se publica en las tiendas o es solo portafolio?
