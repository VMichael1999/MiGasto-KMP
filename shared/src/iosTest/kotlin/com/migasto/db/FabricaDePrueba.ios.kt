package com.migasto.db

import platform.Foundation.NSTemporaryDirectory

actual fun fabricaDePrueba(): FabricaDeDriver = FabricaDeDriverIos(NSTemporaryDirectory())
