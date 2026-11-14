package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

/** Cálculo de d′ (sensibilidad en el N-Back). */
public final class DPrima {

    private DPrima() {
    }

    /**
     * d′ con corrección para tasas de 0 o 1: se suma 0,5 a cada cuenta y 1 a cada total,
     * así nunca sale infinito.
     */
    public static double calcular(int aciertos, int coincidencias, int falsasAlarmas, int noCoincidencias) {
        double tasaAciertos = (aciertos + 0.5) / (coincidencias + 1.0);
        double tasaFA = (falsasAlarmas + 0.5) / (noCoincidencias + 1.0);
        return z(tasaAciertos) - z(tasaFA);
    }

    /** Inversa aproximada de la normal estándar (Abramowitz y Stegun 26.2.23). */
    private static double z(double p) {
        double q = p < 0.5 ? p : 1 - p;
        double t = Math.sqrt(-2.0 * Math.log(q));
        double x = t - (2.515517 + 0.802853 * t + 0.010328 * t * t)
                / (1 + 1.432788 * t + 0.189269 * t * t + 0.001308 * t * t * t);
        return p < 0.5 ? -x : x;
    }
}
