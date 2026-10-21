package com.migasto.seguridad

import androidx.test.platform.app.InstrumentationRegistry

actual fun almacenSeguroReal(): AlmacenSeguro =
    AlmacenSeguroAndroid(InstrumentationRegistry.getInstrumentation().targetContext)
