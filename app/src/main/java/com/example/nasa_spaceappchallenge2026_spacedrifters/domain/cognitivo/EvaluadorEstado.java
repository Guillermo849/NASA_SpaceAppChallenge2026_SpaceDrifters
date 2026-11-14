package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

import androidx.annotation.Nullable;

/** Decide el estado de una sesión comparando su métrica principal con la línea base. */
public final class EvaluadorEstado {

    /** Hacia dónde es "peor" una métrica. */
    public enum Direccion {
        /** Más alto es peor (efecto Stroop en ms). */
        MAYOR_ES_PEOR,
        /** Más bajo es peor (d′ del N-Back). */
        MENOR_ES_PEOR
    }

    private EvaluadorEstado() {
    }

    /**
     * NORMAL dentro de media ± umbralVigilar desviaciones (o si es mejor de lo habitual);
     * VIGILAR hasta umbralAlerta desviaciones en la dirección mala; ALERTA por encima.
     * Sin línea base → SIN_LINEA_BASE; sin valor → SIN_DATOS. Nunca NORMAL por defecto.
     *
     * Si la desviación de la línea base es 0, cualquier empeoramiento cuenta como ALERTA.
     */
    public static EstadoCognitivo evaluar(@Nullable Double valor, @Nullable LineaBase base,
                                          Direccion direccion, ConfigCognitiva config) {
        if (base == null) return EstadoCognitivo.SIN_LINEA_BASE;
        if (valor == null || valor.isNaN()) return EstadoCognitivo.SIN_DATOS;

        // Cuánto ha empeorado respecto a la media (positivo = peor).
        double empeora = direccion == Direccion.MAYOR_ES_PEOR ? valor - base.getMedia() : base.getMedia() - valor;
        if (empeora <= 0) return EstadoCognitivo.NORMAL;
        if (base.getDesviacion() <= 0) return EstadoCognitivo.ALERTA;

        double desviaciones = empeora / base.getDesviacion();
        if (desviaciones <= config.umbralVigilar) return EstadoCognitivo.NORMAL;
        if (desviaciones <= config.umbralAlerta) return EstadoCognitivo.VIGILAR;
        return EstadoCognitivo.ALERTA;
    }
}
