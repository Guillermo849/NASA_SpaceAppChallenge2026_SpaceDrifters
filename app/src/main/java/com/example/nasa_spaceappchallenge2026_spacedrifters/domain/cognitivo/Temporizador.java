package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

/**
 * Programa una acción para dentro de un tiempo. Lo usan los controladores de las pruebas para
 * marcar el ritmo (fijación, límite de respuesta...) sin depender de Android.
 * En la app lo implementa el ViewModel; en los tests, un temporizador falso con reloj simulado.
 * Todas las acciones se ejecutan en el mismo hilo que llama al controlador (el principal).
 */
public interface Temporizador {

    Cancelable programar(long retrasoMs, Runnable accion);

    interface Cancelable {
        void cancelar();
    }
}
