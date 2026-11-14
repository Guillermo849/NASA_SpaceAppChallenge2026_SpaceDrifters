package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.test.core.app.ApplicationProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.ejemplo.CargadorDatosEjemplo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.rules.ExternalResource

/**
 * Deja la base de datos de la app con los datos de ejemplo recién cargados (hoy = día 14), aunque
 * otro test la haya vaciado antes. Tiene que ir antes que la regla que abre la Activity:
 * `@get:Rule(order = 0)`.
 */
class ConDatosEjemplo : ExternalResource() {
    override fun before() {
        val app = ApplicationProvider.getApplicationContext<SpaceDriftersApp>()
        val c = app.container
        runBlocking {
            c.datosEjemplo.await()
            withContext(Dispatchers.IO) { c.database.clearAllTables() }
            val json = app.assets.open(CargadorDatosEjemplo.ARCHIVO).bufferedReader().use { it.readText() }
            CargadorDatosEjemplo(c.database, c.cognitivoRepository, c.alertasRepository).cargarSiVacia(json)
        }
    }
}
