package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Temporizador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * [Temporizador] de los controladores de las pruebas (Java) hecho con corrutinas.
 * Con viewModelScope las acciones van por el hilo principal y se cancelan al cerrar el ViewModel.
 */
fun CoroutineScope.temporizador(): Temporizador = Temporizador { retrasoMs, accion ->
    val trabajo = launch {
        delay(retrasoMs)
        accion.run()
    }
    Temporizador.Cancelable { trabajo.cancel() }
}
