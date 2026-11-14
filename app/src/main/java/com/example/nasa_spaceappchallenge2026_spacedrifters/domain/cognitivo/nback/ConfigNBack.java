package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

/**
 * Parámetros del N-Back. Los valores por defecto son los del diseño del módulo; se pasan como
 * configuración para poder cambiarlos sin tocar la lógica.
 */
public final class ConfigNBack {

    /** Sesión real: 2-back, 2 estímulos iniciales + 40 puntuables, 12 de ellos coincidencias (30 %). */
    public final int nivel;
    public final int puntuables;
    public final int coincidencias;

    /** Práctica 1: 1-back. Totales incluyendo el estímulo inicial. */
    public final int practica1Total;
    public final int practica1Coincidencias;
    /** Práctica 2: 2-back. */
    public final int practica2Total;
    public final int practica2Coincidencias;

    /** Pantalla vacía antes del primer estímulo. */
    public final long pausaInicialMs;
    /** Tiempo que se ve cada forma desde que se dibuja. */
    public final long estimuloMs;
    /** Duración total del ensayo (forma + pantalla vacía); el toque vale durante todo él. */
    public final long ventanaMs;
    /** Solo en práctica: tiempo que se ve el aviso de acierto o fallo. */
    public final long feedbackMs;

    public ConfigNBack() {
        this(2, 40, 12, 15, 4, 10, 3, 1_000, 1_000, 2_500, 800);
    }

    public ConfigNBack(int nivel, int puntuables, int coincidencias,
                       int practica1Total, int practica1Coincidencias,
                       int practica2Total, int practica2Coincidencias,
                       long pausaInicialMs, long estimuloMs, long ventanaMs, long feedbackMs) {
        this.nivel = nivel;
        this.puntuables = puntuables;
        this.coincidencias = coincidencias;
        this.practica1Total = practica1Total;
        this.practica1Coincidencias = practica1Coincidencias;
        this.practica2Total = practica2Total;
        this.practica2Coincidencias = practica2Coincidencias;
        this.pausaInicialMs = pausaInicialMs;
        this.estimuloMs = estimuloMs;
        this.ventanaMs = ventanaMs;
        this.feedbackMs = feedbackMs;
    }
}
