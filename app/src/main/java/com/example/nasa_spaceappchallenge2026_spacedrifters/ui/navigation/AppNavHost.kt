package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.InicioAstronautaScreen
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
            InicioAstronautaScreen(onAtras = { navController.popBackStack() })
        }
        composable<RutaTripulacion> {
            TripulacionScreen(onAtras = { navController.popBackStack() })
        }
    }
}
