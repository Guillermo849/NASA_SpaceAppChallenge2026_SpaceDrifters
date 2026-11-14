package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.InicioAstronautaScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.nback.NBackScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.registro.RegistroDiarioScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.stroop.StroopScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.DetalleSesionScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.PerfilAstronautaScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.TripulacionScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.rol.SelectorRolScreen

/**
 * Grafo de navegación de toda la app.
 * Las pantallas no conocen el NavController: reciben lambdas (onAtras, onMedico...).
 */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = RutaSelectorRol) {
        composable<RutaSelectorRol> {
            SelectorRolScreen(
                onAstronauta = { navController.navigate(RutaInicioAstronauta) },
                onMedico = { navController.navigate(RutaTripulacion) },
            )
        }
        composable<RutaInicioAstronauta> {
            InicioAstronautaScreen(
                onAtras = { navController.popBackStack() },
                onEmpezarStroop = { navController.navigate(RutaPruebaStroop) },
                onEmpezarNBack = { navController.navigate(RutaPruebaNBack) },
                onRegistro = { navController.navigate(RutaRegistroDiario) },
            )
        }
        composable<RutaRegistroDiario> {
            RegistroDiarioScreen(
                onAtras = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() },
            )
        }
        // Las pruebas cognitivas van sin animaciones de transición.
        composable<RutaPruebaStroop>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            StroopScreen(onVolver = { navController.popBackStack() })
        }
        composable<RutaPruebaNBack>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            NBackScreen(onVolver = { navController.popBackStack() })
        }
        composable<RutaTripulacion> {
            TripulacionScreen(
                onAtras = { navController.popBackStack() },
                onAstronauta = { id -> navController.navigate(RutaPerfilAstronauta(id)) },
            )
        }
        // El ViewModel lee el id de la ruta (ver AppViewModelProvider).
        composable<RutaPerfilAstronauta> {
            PerfilAstronautaScreen(
                onAtras = { navController.popBackStack() },
                onSesion = { id -> navController.navigate(RutaDetalleSesion(id)) },
            )
        }
        composable<RutaDetalleSesion> {
            DetalleSesionScreen(onAtras = { navController.popBackStack() })
        }
    }
}
