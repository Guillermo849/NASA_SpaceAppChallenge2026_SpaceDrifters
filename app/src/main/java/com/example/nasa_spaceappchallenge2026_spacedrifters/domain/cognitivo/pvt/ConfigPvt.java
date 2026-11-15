package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt;

/**
 * Parámetros del PVT (Psychomotor Vigilance Task) de la NASA.
 *
 * Protocolo estándar de 10 minutos: el estímulo (un contador en ms) aparece tras un
 * intervalo aleatorio uniforme de 2–10 s. El astronauta pulsa lo antes posible.
 * Tiempo de reacción < 200 ms → "respuesta anticipada" (error); > 500 ms → lapso de vigilancia.
 *
 * Para la versión de 3 minutos (PVT-B, validada por la NASA para uso a bordo) se usan los
 * mismos umbrales, pero con 3 min de duración y un mínimo de ensayos de ~20–30.
 */
public final class ConfigPvt {

    /** Duración total de la sesión en ms (10 min estándar; 3 min para PVT-B). */
    public final long duracionSesionMs;

    /** Intervalo mínimo entre el fin del ensayo anterior y el inicio del siguiente (ms). */
    public final long isiMinMs;

    /** Intervalo máximo (ms). */
    public final long isiMaxMs;

    /**
     * Tiempo de reacción mínimo para que se considere una respuesta válida (ms).
     * Por debajo → "anticipación" (error de comisión de tipo anticipatorio).
     */
    public final long trMinimoMs;

    /**
     * Tiempo de reacción máximo (ms). Por encima → "lapso" (omisión conductual).
     * El estímulo desaparece y el ensayo cierra como lapso.
     */
    public final long trLapsoMs;

    /**
     * Tiempo que se muestra el feedback visual tras un anticipo o lapso (ms).
     * Solo se usa en la práctica.
     */
    public final long feedbackMs;

    /**
     * Número de ensayos durante la práctica (por defecto 3 intentos).
     * Permite al astronauta familiarizarse rápidamente con la dinámica sin esperar 3 minutos.
     */
    public final int ensayosPractica;

    /**
     * Duración máxima en ms para la práctica en caso de inactividad (por defecto 30 segundos).
     */
    public final long duracionPracticaMs;

    // Configuración estándar PVT-B (3 minutos oficial; 3 ensayos / máx 30s en práctica).
    public ConfigPvt() {
        this(3 * 60_000L, 2_000L, 10_000L, 100L, 500L, 1_000L, 3, 30_000L);
    }

    public ConfigPvt(long duracionSesionMs, long isiMinMs, long isiMaxMs,
                     long trMinimoMs, long trLapsoMs, long feedbackMs) {
        this(duracionSesionMs, isiMinMs, isiMaxMs, trMinimoMs, trLapsoMs, feedbackMs, 3, 30_000L);
    }

    public ConfigPvt(long duracionSesionMs, long isiMinMs, long isiMaxMs,
                     long trMinimoMs, long trLapsoMs, long feedbackMs,
                     int ensayosPractica, long duracionPracticaMs) {
        this.duracionSesionMs = duracionSesionMs;
        this.isiMinMs = isiMinMs;
        this.isiMaxMs = isiMaxMs;
        this.trMinimoMs = trMinimoMs;
        this.trLapsoMs = trLapsoMs;
        this.feedbackMs = feedbackMs;
        this.ensayosPractica = ensayosPractica;
        this.duracionPracticaMs = duracionPracticaMs;
    }
}
