package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Genera una secuencia N-Back nueva en cada sesión. */
public final class GeneradorNBack {

    private static final FormaNBack[] FORMAS = FormaNBack.values();

    private GeneradorNBack() {
    }

    /**
     * Secuencia de [total] formas con exactamente [coincidencias] coincidencias de nivel [nivel].
     * Las [nivel] primeras nunca pueden ser coincidencia. Además, nunca se repite la misma forma
     * dos veces seguidas (sin coincidencias 1-back accidentales en el 2-back).
     */
    public static SecuenciaNBack generar(int nivel, int total, int coincidencias, Random random) {
        if (nivel < 1 || total < nivel || coincidencias < 0 || coincidencias > total - nivel) {
            throw new IllegalArgumentException("Parámetros de secuencia imposibles");
        }
        List<Integer> candidatas = new ArrayList<>();
        for (int i = nivel; i < total; i++) candidatas.add(i);
        Collections.shuffle(candidatas, random);
        Set<Integer> posicionesCoincidencia = new HashSet<>(candidatas.subList(0, coincidencias));

        List<FormaNBack> formas = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            if (posicionesCoincidencia.contains(i)) {
                formas.add(formas.get(i - nivel));
            } else {
                // Distinta de la de [nivel] atrás (no es coincidencia) y de la anterior (no se repite).
                Set<FormaNBack> excluidas = new HashSet<>();
                if (i >= nivel) excluidas.add(formas.get(i - nivel));
                if (i >= 1) excluidas.add(formas.get(i - 1));
                formas.add(formaAlAzarExcepto(excluidas, random));
            }
        }
        return new SecuenciaNBack(nivel, formas);
    }

    /** Sesión real: 2-back, 2 iniciales + 40 puntuables, 12 coincidencias. */
    public static SecuenciaNBack sesion(ConfigNBack config, Random random) {
        return generar(config.nivel, config.nivel + config.puntuables, config.coincidencias, random);
    }

    /** Primera práctica: 1-back. */
    public static SecuenciaNBack practica1Back(ConfigNBack config, Random random) {
        return generar(1, config.practica1Total, config.practica1Coincidencias, random);
    }

    /** Segunda práctica: 2-back corto. */
    public static SecuenciaNBack practica2Back(ConfigNBack config, Random random) {
        return generar(config.nivel, config.practica2Total, config.practica2Coincidencias, random);
    }

    private static FormaNBack formaAlAzarExcepto(Set<FormaNBack> excluidas, Random random) {
        List<FormaNBack> opciones = new ArrayList<>(FORMAS.length);
        for (FormaNBack f : FORMAS) {
            if (!excluidas.contains(f)) opciones.add(f);
        }
        return opciones.get(random.nextInt(opciones.size()));
    }
}
