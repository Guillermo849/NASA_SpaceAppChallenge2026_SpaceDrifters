package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.registro;

/** Límites del registro diario: horas de sueño y escala de somnolencia de Karolinska (KSS). */
public final class ReglasRegistro {

    public static final double SUENO_MIN = 0.0;
    public static final double SUENO_MAX = 16.0;
    /** Las horas de sueño se anotan de media en media hora. */
    public static final double SUENO_PASO = 0.5;

    public static final int KSS_MIN = 1;
    public static final int KSS_MAX = 9;

    private ReglasRegistro() {
    }

    /** Suma [pasos] medias horas (negativo para restar), redondea a media hora y lo deja en 0–16 h. */
    public static double ajustarSueno(double horas, int pasos) {
        double nuevo = redondearSueno(horas) + pasos * SUENO_PASO;
        return Math.max(SUENO_MIN, Math.min(SUENO_MAX, nuevo));
    }

    /** Redondea a la media hora más cercana. */
    public static double redondearSueno(double horas) {
        return Math.round(horas / SUENO_PASO) * SUENO_PASO;
    }

    public static boolean somnolenciaValida(int kss) {
        return kss >= KSS_MIN && kss <= KSS_MAX;
    }
}
