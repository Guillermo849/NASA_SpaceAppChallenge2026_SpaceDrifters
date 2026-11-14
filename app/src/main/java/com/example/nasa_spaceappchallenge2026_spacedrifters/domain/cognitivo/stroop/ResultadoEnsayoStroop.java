package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import androidx.annotation.Nullable;

import java.util.Objects;

/** Resultado de un ensayo. Respuesta null = omisión (no respondió a tiempo). */
public final class ResultadoEnsayoStroop {

    private final int numero;
    private final EstimuloStroop estimulo;
    @Nullable private final ColorStroop respuesta;
    @Nullable private final Long tiempoMs;

    public ResultadoEnsayoStroop(int numero, EstimuloStroop estimulo,
                                 @Nullable ColorStroop respuesta, @Nullable Long tiempoMs) {
        this.numero = numero;
        this.estimulo = Objects.requireNonNull(estimulo);
        this.respuesta = respuesta;
        this.tiempoMs = tiempoMs;
    }

    /** Número de ensayo, empezando en 1. */
    public int getNumero() {
        return numero;
    }

    public EstimuloStroop getEstimulo() {
        return estimulo;
    }

    @Nullable
    public ColorStroop getRespuesta() {
        return respuesta;
    }

    /** Tiempo de reacción en ms; null si fue omisión. */
    @Nullable
    public Long getTiempoMs() {
        return tiempoMs;
    }

    /** Lo correcto es tocar el color de la TINTA. */
    public boolean esCorrecto() {
        return respuesta == estimulo.getTinta();
    }

    public boolean esOmision() {
        return respuesta == null;
    }
}
