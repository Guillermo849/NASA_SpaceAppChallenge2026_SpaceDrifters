package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

import java.util.ArrayList;
import java.util.List;

/** Tendencia de las últimas sesiones de una prueba, para el médico. */
public final class Tendencia {

    public enum Sentido { MEJORANDO, ESTABLE, EMPEORANDO, SIN_DATOS }

    /** Por debajo de este cambio (en desviaciones de la línea base) se considera estable. */
    public static final double UMBRAL_DESVIACIONES = 0.5;
    /** Sesiones que se miran. */
    public static final int SESIONES = 3;

    private Tendencia() {
    }

    /**
     * Compara la más reciente con la más antigua de las últimas [SESIONES] (las null se ignoran).
     * El cambio se mide en desviaciones de la línea base y en la dirección "buena" de la métrica.
     * Sin línea base (desviación ≤ 0) se compara el valor directamente, con una tolerancia mínima.
     *
     * @param valores de la sesión más reciente a la más antigua
     */
    public static Sentido calcular(List<Double> valores, EvaluadorEstado.Direccion direccion, double desviacion) {
        List<Double> validos = new ArrayList<>();
        for (Double v : valores) {
            if (v != null && !v.isNaN()) validos.add(v);
            if (validos.size() == SESIONES) break;
        }
        if (validos.size() < 2) return Sentido.SIN_DATOS;

        double reciente = validos.get(0);
        double antigua = validos.get(validos.size() - 1);
        double mejora;
        double umbral;
        if (desviacion > 0) {
            mejora = Rendimiento.puntuacion(reciente, antigua, desviacion, direccion);
            umbral = UMBRAL_DESVIACIONES;
        } else {
            mejora = Rendimiento.orientado(reciente, direccion) - Rendimiento.orientado(antigua, direccion);
            umbral = 1e-9;
        }
        if (Math.abs(mejora) < umbral) return Sentido.ESTABLE;
        return mejora > 0 ? Sentido.MEJORANDO : Sentido.EMPEORANDO;
    }
}
