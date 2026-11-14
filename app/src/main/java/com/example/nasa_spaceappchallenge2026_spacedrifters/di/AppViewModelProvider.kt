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
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation.RutaResultadoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial.OficialMedicoViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial.ResultadoPruebaViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.DetalleSesionViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.PerfilAstronautaViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.FisiologicoMedicoViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.TripulacionViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.InicioAstronautaViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.emergencia.EmergenciaViewModel
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
            val c = app().container
            LoginViewModel(
                userDao = c.userDao,
                astronautDao = c.database.astronautDao(),
                alEntrarAstronauta = { c.astronautaActualId = it },
            )
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
                fisiologico = c.fisiologicoRepository,
            )
        }
        initializer {
            val c = app().container
            EmergenciaViewModel(c.fisiologicoRepository, c.astronautaActualId, c.ambitoAplicacion)
        }
        // Oficial médico de a bordo
        initializer { OficialMedicoViewModel(app().container.fisiologicoRepository) }
        initializer {
            val c = app().container
            ResultadoPruebaViewModel(
                c.fisiologicoRepository,
                createSavedStateHandle().toRoute<RutaResultadoPrueba>().pruebaId,
                c.ambitoAplicacion,
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
            val c = app().container
            FisiologicoMedicoViewModel(
                c.fisiologicoMedicoRepository,
                c.medicoRepository,
                c.umbrales,
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
