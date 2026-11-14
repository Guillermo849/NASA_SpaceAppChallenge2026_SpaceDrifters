package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import androidx.annotation.Nullable;

import java.util.Objects;

/** Resultado de un estímulo del N-Back. */
public final class ResultadoEnsayoNBack {

    /** Las 4 combinaciones de "era coincidencia" y "pulsó". */
    public enum Tipo { ACIERTO, FALSA_ALARMA, OMISION, RECHAZO_CORRECTO }

    private final int numero;
    private final FormaNBack forma;
    private final boolean esCoincidencia;
    @Nullable private final Long tiempoMs;

    /** tiempoMs null = no pulsó. */
    public ResultadoEnsayoNBack(int numero, FormaNBack forma, boolean esCoincidencia, @Nullable Long tiempoMs) {
        this.numero = numero;
        this.forma = Objects.requireNonNull(forma);
        this.esCoincidencia = esCoincidencia;
        this.tiempoMs = tiempoMs;
    }

    /** Número de estímulo, empezando en 1. */
    public int getNumero() {
        return numero;
    }

    public FormaNBack getForma() {
        return forma;
    }

    public boolean esCoincidencia() {
        return esCoincidencia;
    }

    public boolean esPulsado() {
        return tiempoMs != null;
    }

    /** Tiempo de reacción en ms; null si no pulsó. */
    @Nullable
    public Long getTiempoMs() {
        return tiempoMs;
    }

    public Tipo getTipo() {
        if (esCoincidencia) return esPulsado() ? Tipo.ACIERTO : Tipo.OMISION;
        return esPulsado() ? Tipo.FALSA_ALARMA : Tipo.RECHAZO_CORRECTO;
    }

    public boolean esCorrecto() {
        return esCoincidencia == esPulsado();
    }
}
