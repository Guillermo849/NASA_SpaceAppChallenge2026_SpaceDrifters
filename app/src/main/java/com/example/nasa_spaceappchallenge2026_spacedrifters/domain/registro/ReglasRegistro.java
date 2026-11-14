package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.registro;

/** Límites del registro diario: horas de sueño y escala de somnolencia de Karolinska (KSS). */
public final class ReglasRegistro {

    public static final double SUENO_MIN = 0.0;
    public static final double SUENO_MAX = 16.0;
    /** Las horas de sueño se anotan de media en media hora. */
    public static final double SUENO_PASO = 0.5;

    public static final int KSS_MIN = 1;
    public static final int KSS_MAX = 9;

    /** Líquido bebido: de cuarto en cuarto de litro, 0–6 L. */
    public static final double AGUA_MIN = 0.0;
    public static final double AGUA_MAX = 6.0;
    public static final double AGUA_PASO = 0.25;

    /** Parte del menú planificado que se ha comido: 0, 25, 50, 75 o 100 %. */
    public static final int[] INGESTA_OPCIONES = {0, 25, 50, 75, 100};

    /** Ejercicio: de 15 en 15 minutos, 0–300 min. */
    public static final int EJERCICIO_MIN = 0;
    public static final int EJERCICIO_MAX = 300;
    public static final int EJERCICIO_PASO = 15;

    private ReglasRegistro() {
    }

    /**
     * Suma [pasos] cuartos de litro. Sin anotar (null) empieza en [inicio] (el objetivo diario),
     * para no tener que pulsar desde cero.
     */
    public static double ajustarAgua(Double litros, int pasos, double inicio) {
        if (litros == null) return limitar(redondear(inicio, AGUA_PASO), AGUA_MIN, AGUA_MAX);
        return limitar(redondear(litros, AGUA_PASO) + pasos * AGUA_PASO, AGUA_MIN, AGUA_MAX);
    }

    /** Suma [pasos] cuartos de hora. Sin anotar (null) empieza en [inicio] (el objetivo diario). */
    public static int ajustarEjercicio(Integer minutos, int pasos, int inicio) {
        int base = minutos == null ? inicio : minutos + pasos * EJERCICIO_PASO;
        int redondeado = (int) Math.round(base / (double) EJERCICIO_PASO) * EJERCICIO_PASO;
        return Math.max(EJERCICIO_MIN, Math.min(EJERCICIO_MAX, redondeado));
    }

    public static boolean ingestaValida(int porcentaje) {
        for (int opcion : INGESTA_OPCIONES) {
            if (opcion == porcentaje) return true;
        }
        return false;
    }

    /** Si [valor] llega al [objetivo]. null = sin datos (ni cumple ni incumple). */
    public static Boolean cumpleObjetivo(Double valor, double objetivo) {
        return valor == null ? null : valor >= objetivo;
    }

    private static double redondear(double valor, double paso) {
        return Math.round(valor / paso) * paso;
    }

    private static double limitar(double valor, double min, double max) {
        return Math.max(min, Math.min(max, valor));
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
