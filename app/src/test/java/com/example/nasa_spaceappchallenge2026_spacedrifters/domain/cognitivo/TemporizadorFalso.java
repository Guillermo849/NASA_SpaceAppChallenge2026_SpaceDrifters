package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Temporizador para tests: el tiempo solo avanza cuando se llama a {@link #avanzar}. */
public final class TemporizadorFalso implements Temporizador {

    private static final class Tarea {
        final long vence;
        final long orden;
        final Runnable accion;
        boolean cancelada = false;

        Tarea(long vence, long orden, Runnable accion) {
            this.vence = vence;
            this.orden = orden;
            this.accion = accion;
        }
    }

    private final List<Tarea> tareas = new ArrayList<>();
    private long ahoraMs = 0;
    private long contador = 0;

    @Override
    public Cancelable programar(long retrasoMs, Runnable accion) {
        Tarea tarea = new Tarea(ahoraMs + retrasoMs, contador++, accion);
        tareas.add(tarea);
        return () -> tarea.cancelada = true;
    }

    /** Avanza el reloj y ejecuta, en orden, todo lo que vence hasta entonces (incluido lo nuevo). */
    public void avanzar(long ms) {
        long objetivo = ahoraMs + ms;
        while (true) {
            Tarea siguiente = tareas.stream()
                    .filter(t -> !t.cancelada && t.vence <= objetivo)
                    .min(Comparator.comparingLong((Tarea t) -> t.vence).thenComparingLong(t -> t.orden))
                    .orElse(null);
            if (siguiente == null) break;
            tareas.remove(siguiente);
            ahoraMs = siguiente.vence;
            siguiente.accion.run();
        }
        ahoraMs = objetivo;
    }

    public long getAhoraMs() {
        return ahoraMs;
    }
}
