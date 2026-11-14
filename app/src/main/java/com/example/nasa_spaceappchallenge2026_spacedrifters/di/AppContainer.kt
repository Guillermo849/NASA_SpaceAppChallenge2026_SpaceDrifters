package com.example.nasa_spaceappchallenge2026_spacedrifters.di

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase

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
}
