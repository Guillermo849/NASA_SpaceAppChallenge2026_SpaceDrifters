package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import java.util.Objects;

/** Lo que se ve en un ensayo: la palabra escrita en tinta de un color. */
public final class EstimuloStroop {

    private final ColorStroop palabra;
    private final ColorStroop tinta;

    public EstimuloStroop(ColorStroop palabra, ColorStroop tinta) {
        this.palabra = Objects.requireNonNull(palabra);
        this.tinta = Objects.requireNonNull(tinta);
    }

    public ColorStroop getPalabra() {
        return palabra;
    }

    public ColorStroop getTinta() {
        return tinta;
    }

    /** Coincidente: ROJO escrito en rojo. */
    public boolean esCoincidente() {
        return palabra == tinta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EstimuloStroop)) return false;
        EstimuloStroop otro = (EstimuloStroop) o;
        return palabra == otro.palabra && tinta == otro.tinta;
    }

    @Override
    public int hashCode() {
        return Objects.hash(palabra, tinta);
    }

    @Override
    public String toString() {
        return palabra + " en " + tinta;
    }
}
