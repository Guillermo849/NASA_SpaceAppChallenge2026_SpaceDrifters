package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Resultado de un ensayo PVT.
 *
 * | tiempoMs | anticipacion | Interpretación                              |
 * |----------|--------------|---------------------------------------------|
 * | null     | false        | Lapso: no respondió en el límite (> 500 ms) |
 * | t < min  | true         | Anticipación: pulsó antes de que saliera    |
 * | t válido | false        | Respuesta válida                            |
 */
public final class ResultadoEnsayoPvt {

    private final int numero;
    /** Tiempo de reacción en ms desde que apareció el estímulo; null = lapso o anticipación sin estímulo. */
    @Nullable private final Long tiempoMs;
    /** Pulsó antes del tiempo mínimo (< trMinimoMs). */
    private final boolean anticipacion;

    public ResultadoEnsayoPvt(int numero, @Nullable Long tiempoMs, boolean anticipacion) {
        this.numero = numero;
        this.tiempoMs = tiempoMs;
        this.anticipacion = anticipacion;
    }

    public int getNumero() { return numero; }

    @Nullable
    public Long getTiempoMs() { return tiempoMs; }

    public boolean isAnticipacion() { return anticipacion; }

    /** El TR era válido (≥ trMinimoMs y < trLapsoMs). */
    public boolean esValido() { return tiempoMs != null && !anticipacion; }

    /** No respondió dentro del límite. */
    public boolean esLapso() { return tiempoMs == null && !anticipacion; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResultadoEnsayoPvt)) return false;
        ResultadoEnsayoPvt r = (ResultadoEnsayoPvt) o;
        return numero == r.numero
                && Objects.equals(tiempoMs, r.tiempoMs)
                && anticipacion == r.anticipacion;
    }

    @Override
    public int hashCode() { return Objects.hash(numero, tiempoMs, anticipacion); }
}
