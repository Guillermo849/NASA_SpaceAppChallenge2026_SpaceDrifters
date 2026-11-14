package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import androidx.annotation.Nullable;

import java.util.List;

/** Métricas del resumen de una sesión Stroop. La principal es el efecto Stroop. */
public final class MetricasStroop {

    @Nullable private final Double tiempoMedioMs;
    @Nullable private final Double porcentajeAciertos;
    @Nullable private final Double efectoStroopMs;
    private final int erroresNoCoincidentes;

    private MetricasStroop(@Nullable Double tiempoMedioMs, @Nullable Double porcentajeAciertos,
                           @Nullable Double efectoStroopMs, int erroresNoCoincidentes) {
        this.tiempoMedioMs = tiempoMedioMs;
        this.porcentajeAciertos = porcentajeAciertos;
        this.efectoStroopMs = efectoStroopMs;
        this.erroresNoCoincidentes = erroresNoCoincidentes;
    }

    /** Media de los tiempos de los aciertos; null si no hubo ninguno. */
    @Nullable
    public Double getTiempoMedioMs() {
        return tiempoMedioMs;
    }

    /** Aciertos sobre el total de ensayos (las omisiones cuentan como fallo), de 0 a 100. */
    @Nullable
    public Double getPorcentajeAciertos() {
        return porcentajeAciertos;
    }

    /**
     * Métrica principal: tiempo medio de los aciertos no coincidentes − tiempo medio de los
     * aciertos coincidentes. Null si falta alguno de los dos grupos.
     */
    @Nullable
    public Double getEfectoStroopMs() {
        return efectoStroopMs;
    }

    /** Respuestas equivocadas en ensayos no coincidentes (sin contar omisiones). */
    public int getErroresNoCoincidentes() {
        return erroresNoCoincidentes;
    }

    public static MetricasStroop calcular(List<ResultadoEnsayoStroop> resultados) {
        double sumaAciertos = 0, sumaCoincidentes = 0, sumaNoCoincidentes = 0;
        int aciertos = 0, aciertosCoincidentes = 0, aciertosNoCoincidentes = 0, errores = 0;

        for (ResultadoEnsayoStroop r : resultados) {
            boolean coincidente = r.getEstimulo().esCoincidente();
            if (r.esCorrecto() && r.getTiempoMs() != null) {
                double t = r.getTiempoMs();
                aciertos++;
                sumaAciertos += t;
                if (coincidente) {
                    aciertosCoincidentes++;
                    sumaCoincidentes += t;
                } else {
                    aciertosNoCoincidentes++;
                    sumaNoCoincidentes += t;
                }
            } else if (!coincidente && !r.esOmision()) {
                errores++;
            }
        }

        Double tiempoMedio = aciertos > 0 ? sumaAciertos / aciertos : null;
        Double porcentaje = resultados.isEmpty() ? null : 100.0 * aciertos / resultados.size();
        Double efecto = (aciertosCoincidentes > 0 && aciertosNoCoincidentes > 0)
                ? sumaNoCoincidentes / aciertosNoCoincidentes - sumaCoincidentes / aciertosCoincidentes
                : null;
        return new MetricasStroop(tiempoMedio, porcentaje, efecto, errores);
    }
}
