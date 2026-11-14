package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Juego de memoria "Parejas orbitales": cartas boca abajo con cuerpos celestes; se dan la vuelta
 * de dos en dos buscando parejas. Sin tiempo límite ni puntuación que compita: es para
 * desconectar, no una prueba. Sin temporizador propio: tras un fallo, la pantalla espera un
 * momento y llama a [ocultarFallo].
 */
public final class JuegoParejas {

    public static final int PAREJAS_POR_DEFECTO = 6;

    public enum Resultado {
        /** Carta ya visible, fallo sin ocultar o juego terminado: no pasa nada. */
        IGNORADA,
        /** Primera carta del intento. */
        PRIMERA,
        /** Segunda carta, igual que la primera: se quedan boca arriba. */
        PAREJA,
        /** Segunda carta distinta: las dos se ven hasta [ocultarFallo]. */
        FALLO
    }

    private final int[] simbolos;
    private final boolean[] emparejadas;
    private int primera = -1;
    private int segunda = -1;
    private int intentos;
    private int parejasEncontradas;

    /** @param random barajado (en los tests, con semilla fija) */
    public JuegoParejas(int parejas, Random random) {
        if (parejas < 1) throw new IllegalArgumentException("parejas");
        List<Integer> mazo = new ArrayList<>();
        for (int s = 0; s < parejas; s++) {
            mazo.add(s);
            mazo.add(s);
        }
        Collections.shuffle(mazo, random);
        simbolos = new int[mazo.size()];
        for (int i = 0; i < simbolos.length; i++) simbolos[i] = mazo.get(i);
        emparejadas = new boolean[simbolos.length];
    }

    public int getNumCartas() { return simbolos.length; }

    /** Símbolo de la carta (0..parejas-1): la pantalla lo traduce a un cuerpo celeste. */
    public int simbolo(int carta) { return simbolos[carta]; }

    public boolean estaBocaArriba(int carta) {
        return emparejadas[carta] || carta == primera || carta == segunda;
    }

    public boolean estaEmparejada(int carta) { return emparejadas[carta]; }

    public boolean hayFalloPendiente() { return segunda != -1; }

    /** Intentos completos (pares de cartas levantadas). */
    public int getIntentos() { return intentos; }

    public int getParejasEncontradas() { return parejasEncontradas; }

    public int getParejas() { return simbolos.length / 2; }

    public boolean isTerminado() { return parejasEncontradas == getParejas(); }

    public Resultado voltear(int carta) {
        if (carta < 0 || carta >= simbolos.length) return Resultado.IGNORADA;
        if (isTerminado() || hayFalloPendiente() || estaBocaArriba(carta)) return Resultado.IGNORADA;
        if (primera == -1) {
            primera = carta;
            return Resultado.PRIMERA;
        }
        intentos++;
        if (simbolos[primera] == simbolos[carta]) {
            emparejadas[primera] = true;
            emparejadas[carta] = true;
            parejasEncontradas++;
            primera = -1;
            return Resultado.PAREJA;
        }
        segunda = carta;
        return Resultado.FALLO;
    }

    /** Vuelve a poner boca abajo las dos cartas del último fallo. */
    public void ocultarFallo() {
        if (!hayFalloPendiente()) return;
        primera = -1;
        segunda = -1;
    }
}
