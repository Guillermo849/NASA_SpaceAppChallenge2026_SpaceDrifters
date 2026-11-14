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
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.login.LoginScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.DetalleSesionScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.PerfilAstronautaScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico.TripulacionScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.rol.SelectorRolScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial.OficialMedicoScreen
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial.ResultadoPruebaScreen

/**
 * Grafo de navegación de toda la app.
 * Las pantallas no conocen el NavController: reciben lambdas (onAtras, onMedico...).
 *
 * El punto de entrada es [RutaLogin]. Según el rol del usuario autenticado se navega a
 * [RutaInicioAstronauta] (astronauta) o [RutaTripulacion] (admin/médico). La oficial médico de a
 * bordo entra como astronauta y desde su panel abre [RutaOficialMedico].
 */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = RutaLogin) {

        // ── Login ─────────────────────────────────────────────────────────────
        composable<RutaLogin> {
            LoginScreen(
                onAstronauta = {
                    navController.navigate(RutaInicioAstronauta) {
                        // Eliminar el login del back-stack para que no se pueda volver.
                        popUpTo(RutaLogin) { inclusive = true }
                    }
                },
                onAdmin = {
                    navController.navigate(RutaTripulacion) {
                        popUpTo(RutaLogin) { inclusive = true }
                    }
                },
            )
        }

        // ── Selector de rol (ya no es startDestination, pero se conserva) ────
        composable<RutaSelectorRol> {
            SelectorRolScreen(
                onAstronauta = { navController.navigate(RutaInicioAstronauta) },
                onMedico = { navController.navigate(RutaTripulacion) },
                onOficial = { navController.navigate(RutaOficialMedico) },
            )
        }
        // Oficial médico de a bordo
        composable<RutaOficialMedico> {
            OficialMedicoScreen(
                onAtras = { navController.popBackStack() },
                onPrueba = { id -> navController.navigate(RutaResultadoPrueba(id)) },
            )
        }
        // El ViewModel lee el id de la prueba de la ruta (ver AppViewModelProvider).
        composable<RutaResultadoPrueba> {
            ResultadoPruebaScreen(
                onAtras = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() },
            )
        }

        // ── Panel del astronauta ──────────────────────────────────────────────
        composable<RutaInicioAstronauta> {
            InicioAstronautaScreen(
                onAtras = { navController.popBackStack() },
                onEmpezarStroop = { navController.navigate(RutaPruebaStroop) },
                onEmpezarNBack = { navController.navigate(RutaPruebaNBack) },
                onRegistro = { navController.navigate(RutaRegistroDiario) },
                // Solo lo ve la oficial médico de a bordo.
                onOficial = { navController.navigate(RutaOficialMedico) },
                onCerrarSesion = {
                    navController.navigate(RutaLogin) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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

        // ── Panel del médico / admin ──────────────────────────────────────────
        composable<RutaTripulacion> {
            TripulacionScreen(
                onAtras = { navController.popBackStack() },
                onAstronauta = { id -> navController.navigate(RutaPerfilAstronauta(id)) },
                onCerrarSesion = {
                    navController.navigate(RutaLogin) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
