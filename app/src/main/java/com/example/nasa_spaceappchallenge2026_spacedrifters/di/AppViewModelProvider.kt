package com.example.nasa_spaceappchallenge2026_spacedrifters.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.example.nasa_spaceappchallenge2026_spacedrifters.SpaceDriftersApp
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation.RutaDetalleSesion
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation.RutaPerfilAstronauta
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.DetalleSesionViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.PerfilAstronautaViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.TripulacionViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.InicioAstronautaViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.nback.NBackViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.registro.RegistroDiarioViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.stroop.StroopViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.login.LoginViewModel

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
        initializer {
            LoginViewModel(userDao = app().container.userDao)
        }
        initializer {
            val c = app().container
            InicioAstronautaViewModel(
                cognitivo = c.cognitivoRepository,
                tripulacion = c.tripulacionRepository,
                alertas = c.alertasRepository,
                medico = c.medicoRepository,
                astronautaId = c.astronautaActualId,
                retrasoMin = c.configMision.retrasoMisionMin,
            )
        }
        // Médico
        initializer { TripulacionViewModel(app().container.medicoRepository) }
        initializer {
            PerfilAstronautaViewModel(
                app().container.medicoRepository,
                createSavedStateHandle().toRoute<RutaPerfilAstronauta>().astronautaId,
            )
        }
        initializer {
            DetalleSesionViewModel(
                app().container.medicoRepository,
                createSavedStateHandle().toRoute<RutaDetalleSesion>().sesionId,
            )
        }
        initializer {
            val c = app().container
            StroopViewModel(c.cognitivoRepository, c.astronautaActualId, c.ambitoAplicacion, c.alertasRepository)
        }
        initializer {
            val c = app().container
            NBackViewModel(c.cognitivoRepository, c.astronautaActualId, c.ambitoAplicacion, c.alertasRepository)
        }
        initializer {
            val c = app().container
            RegistroDiarioViewModel(c.tripulacionRepository, c.alertasRepository, c.astronautaActualId)
        }
    }
}

/** Acceso a la Application (y a su [AppContainer]) desde un `initializer`. */
fun CreationExtras.app(): SpaceDriftersApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SpaceDriftersApp
