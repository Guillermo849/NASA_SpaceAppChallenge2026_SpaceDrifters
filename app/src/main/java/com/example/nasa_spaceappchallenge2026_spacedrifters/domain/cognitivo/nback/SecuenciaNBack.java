package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Formas de una sesión N-Back y su nivel (1-back, 2-back...). */
public final class SecuenciaNBack {

    private final int nivel;
    private final List<FormaNBack> formas;

    public SecuenciaNBack(int nivel, List<FormaNBack> formas) {
        if (nivel < 1) throw new IllegalArgumentException("El nivel tiene que ser 1 o más");
        this.nivel = nivel;
        this.formas = Collections.unmodifiableList(new ArrayList<>(formas));
    }

    public int getNivel() {
        return nivel;
    }

    public List<FormaNBack> getFormas() {
        return formas;
    }

    public int size() {
        return formas.size();
    }

    public FormaNBack get(int indice) {
        return formas.get(indice);
    }

    /** Coincidencia: la forma es igual a la de [nivel] posiciones atrás. Las primeras nunca lo son. */
    public boolean esCoincidencia(int indice) {
        return indice >= nivel && formas.get(indice) == formas.get(indice - nivel);
    }

    public int contarCoincidencias() {
        int total = 0;
        for (int i = 0; i < formas.size(); i++) {
            if (esCoincidencia(i)) total++;
        }
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecuenciaNBack)) return false;
        SecuenciaNBack otra = (SecuenciaNBack) o;
        return nivel == otra.nivel && formas.equals(otra.formas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nivel, formas);
    }
}
