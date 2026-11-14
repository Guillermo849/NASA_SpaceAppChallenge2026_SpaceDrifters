package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.rules.ExternalResource

/**
 * Vacía la base de datos de la app antes de cada test, para que no dependan unos de otros
 * (p. ej., que la práctica ya esté hecha por un test anterior). Tiene que ir antes que la regla
 * que abre la Activity: `@get:Rule(order = 0)`.
 */
class BaseDeDatosLimpia : ExternalResource() {
    override fun before() {
        val app = ApplicationProvider.getApplicationContext<SpaceDriftersApp>()
        runBlocking { withContext(Dispatchers.IO) { app.container.database.clearAllTables() } }
    }
}
