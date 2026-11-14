package com.example.nasa_spaceappchallenge2026_spacedrifters.di

import android.content.Context

/**
 * Inyección de dependencias manual (sin Hilt).
 *
 * Aquí se crean, una sola vez, la base de datos, los repositorios y cualquier otro
 * objeto compartido. Los ViewModels los obtienen a través de [AppViewModelProvider].
 *
 * Ejemplo de cómo añadir algo nuevo:
 * ```
 * val database: AppDatabase by lazy { AppDatabase.crear(appContext) }
 * val tripulacionRepository by lazy { TripulacionRepository(database.astronautaDao()) }
 * ```
 */
class AppContainer(context: Context) {
    @Suppress("unused")
    private val appContext: Context = context.applicationContext
}
