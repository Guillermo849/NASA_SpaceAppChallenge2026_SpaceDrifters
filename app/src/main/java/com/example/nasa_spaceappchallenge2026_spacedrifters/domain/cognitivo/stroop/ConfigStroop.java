package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

/**
 * Parámetros del Stroop. Los valores por defecto son los del diseño del módulo; se pasan como
 * configuración para poder cambiarlos sin tocar la lógica.
 */
public final class ConfigStroop {

    public final long fijacionMs;
    public final long limiteRespuestaMs;
    public final int coincidentes;
    public final int noCoincidentes;
    public final int practicaCoincidentes;
    public final int practicaNoCoincidentes;
    /** Solo en práctica: tiempo que se ve el aviso de acierto o fallo. */
    public final long feedbackMs;

    public ConfigStroop() {
        this(500, 3_000, 20, 20, 4, 4, 800);
    }

    public ConfigStroop(long fijacionMs, long limiteRespuestaMs, int coincidentes, int noCoincidentes,
                        int practicaCoincidentes, int practicaNoCoincidentes, long feedbackMs) {
        this.fijacionMs = fijacionMs;
        this.limiteRespuestaMs = limiteRespuestaMs;
        this.coincidentes = coincidentes;
        this.noCoincidentes = noCoincidentes;
        this.practicaCoincidentes = practicaCoincidentes;
        this.practicaNoCoincidentes = practicaNoCoincidentes;
        this.feedbackMs = feedbackMs;
    }
}
