package com.migasto.db

import androidx.test.platform.app.InstrumentationRegistry

actual fun fabricaDePrueba(): FabricaDeDriver =
    FabricaDeDriverAndroid(InstrumentationRegistry.getInstrumentation().targetContext)
