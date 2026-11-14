package com.example.nasa_spaceappchallenge2026_spacedrifters.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nasa_spaceappchallenge2026_spacedrifters.SpaceDriftersApp

/**
 * Fábrica única de ViewModels. Cada ViewModel nuevo se registra con un `initializer`:
 * ```
 * initializer { TripulacionViewModel(app().container.tripulacionRepository) }
 * ```
 * y en la pantalla: `viewModel<TripulacionViewModel>(factory = AppViewModelProvider.Factory)`.
 * Si el ViewModel necesita los argumentos de la ruta: `createSavedStateHandle().toRoute<MiRuta>()`.
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        // Todavía no hay ViewModels.
    }
}

/** Acceso a la Application (y a su [AppContainer]) desde un `initializer`. */
@Suppress("unused")
fun CreationExtras.app(): SpaceDriftersApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SpaceDriftersApp
