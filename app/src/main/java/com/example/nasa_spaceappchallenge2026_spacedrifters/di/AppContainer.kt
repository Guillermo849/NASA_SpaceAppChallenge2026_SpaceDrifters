package com.example.nasa_spaceappchallenge2026_spacedrifters.di

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigMision
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Inyección de dependencias manual (sin Hilt).
 *
 * Aquí se crean, una sola vez, la base de datos, los repositorios y cualquier otro
 * objeto compartido. Los ViewModels los obtienen a través de [AppViewModelProvider].
 *
 * Ejemplo de cómo añadir algo nuevo:
 * ```
 * val tripulacionRepository by lazy { TripulacionRepository(database.astronautaDao()) }
 * ```
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.crear(appContext) }

    /** assets/config_mision.json: retraso con la Tierra, umbrales de estado... */
    val configMision: ConfigMision by lazy { ConfigMision.leer(appContext) }

    val cognitivoRepository: CognitivoRepository by lazy {
        CognitivoRepository(database, configMision.aConfigCognitiva())
    }

    /**
     * Astronauta que usa la app en el rol de astronauta. Provisional: cuando haya tripulación
     * (datos de ejemplo), esto saldrá de ahí o de un selector.
     */
    val astronautaActualId: Long = 1L

    /**
     * Ámbito de toda la app, para guardados que tienen que terminar aunque se cierre la pantalla
     * (p. ej., una prueba interrumpida al salir de la app).
     */
    val ambitoAplicacion: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
