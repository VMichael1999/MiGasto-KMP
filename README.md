# MiGasto-KMP

Nueva versión de [MiGasto](https://github.com/VMichael1999/MiGasto), la app que anota tus pagos de Yape, Plin y Google Wallet sin que los escribas. Todo se guarda solo en tu teléfono, cifrado, sin cuentas ni servidor.

Esta versión reparte el trabajo así:

- `shared` (Kotlin Multiplataforma): la lógica, escrita una sola vez para los dos sistemas. Incluye el lector de pagos, las categorías, la protección contra duplicados, la base de datos cifrada, el respaldo `.mgb` y la exportación a CSV, con sus tests.
- `androidApp` (Kotlin + Jetpack Compose): la interfaz en Material 3 y lo propio de Android: lectura de notificaciones, la ventana flotante con cuenta regresiva y los avisos con botones.
- `iosApp` (Swift + SwiftUI): la interfaz con la barra Liquid Glass de iOS 26 y lo propio de iPhone: compartir capturas de Yape que se leen con Vision, registro de Apple Pay con Atajos, widget y Face ID.

Las dos apps se ven como MiGasto (la misma tipografía Outfit, el mismo verde y los mismos textos), pero cada una usa los controles de su sistema.

**Estado:** en desarrollo por fases: primero la lógica compartida, después Android completo y al final iPhone. La versión original en Flutter sigue en su repo.
