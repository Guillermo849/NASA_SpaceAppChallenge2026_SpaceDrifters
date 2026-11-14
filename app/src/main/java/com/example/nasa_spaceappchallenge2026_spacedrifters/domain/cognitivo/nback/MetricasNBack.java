package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import androidx.annotation.Nullable;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.DPrima;

import java.util.List;

/**
 * Métricas del resumen de una sesión N-Back. La principal es d′.
 * Solo cuentan los estímulos puntuables: los [nivel] primeros no pueden ser coincidencia y se
 * excluyen (en la sesión real, 40 puntuables: 12 coincidencias y 28 no coincidencias).
 */
public final class MetricasNBack {

    @Nullable private final Double aciertosCoincidencias;
    private final int falsasAlarmas;
    @Nullable private final Double dPrima;
    @Nullable private final Double tiempoAciertosMs;

    private MetricasNBack(@Nullable Double aciertosCoincidencias, int falsasAlarmas,
                          @Nullable Double dPrima, @Nullable Double tiempoAciertosMs) {
        this.aciertosCoincidencias = aciertosCoincidencias;
        this.falsasAlarmas = falsasAlarmas;
        this.dPrima = dPrima;
        this.tiempoAciertosMs = tiempoAciertosMs;
    }

    /** Porcentaje de coincidencias detectadas (0 a 100); null si no hubo coincidencias. */
    @Nullable
    public Double getAciertosCoincidencias() {
        return aciertosCoincidencias;
    }

    /** Pulsaciones en estímulos que no eran coincidencia. */
    public int getFalsasAlarmas() {
        return falsasAlarmas;
    }

    /** Métrica principal. Null si no hay coincidencias o no hay no coincidencias. */
    @Nullable
    public Double getDPrima() {
        return dPrima;
    }

    /** Tiempo medio de reacción en los aciertos; null si no hubo ninguno. */
    @Nullable
    public Double getTiempoAciertosMs() {
        return tiempoAciertosMs;
    }

    public static MetricasNBack calcular(List<ResultadoEnsayoNBack> resultados, int nivel) {
        int coincidencias = 0, noCoincidencias = 0, aciertos = 0, falsasAlarmas = 0;
        double sumaTiempos = 0;

        for (ResultadoEnsayoNBack r : resultados) {
            if (r.getNumero() <= nivel) continue; // estímulos iniciales: no puntúan
            if (r.esCoincidencia()) {
                coincidencias++;
                if (r.esPulsado()) {
                    aciertos++;
                    sumaTiempos += r.getTiempoMs();
                }
            } else {
                noCoincidencias++;
                if (r.esPulsado()) falsasAlarmas++;
            }
        }

        Double porcentaje = coincidencias > 0 ? 100.0 * aciertos / coincidencias : null;
        Double d = (coincidencias > 0 && noCoincidencias > 0)
                ? DPrima.calcular(aciertos, coincidencias, falsasAlarmas, noCoincidencias)
                : null;
        Double tiempo = aciertos > 0 ? sumaTiempos / aciertos : null;
        return new MetricasNBack(porcentaje, falsasAlarmas, d, tiempo);
    }
}
