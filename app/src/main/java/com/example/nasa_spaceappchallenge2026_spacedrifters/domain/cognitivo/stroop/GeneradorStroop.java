package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Genera una secuencia nueva de ensayos Stroop en cada sesión. */
public final class GeneradorStroop {

    private static final ColorStroop[] COLORES = ColorStroop.values();

    private GeneradorStroop() {
    }

    /**
     * Devuelve coincidentes + noCoincidentes estímulos en orden aleatorio.
     * Nunca se repite el mismo color de tinta en dos ensayos seguidos.
     */
    public static List<EstimuloStroop> generar(int coincidentes, int noCoincidentes, Random random) {
        if (coincidentes < 0 || noCoincidentes < 0) {
            throw new IllegalArgumentException("Número de ensayos negativo");
        }
        List<Boolean> tipos = new ArrayList<>();
        for (int i = 0; i < coincidentes; i++) tipos.add(true);
        for (int i = 0; i < noCoincidentes; i++) tipos.add(false);
        Collections.shuffle(tipos, random);

        List<EstimuloStroop> secuencia = new ArrayList<>(tipos.size());
        ColorStroop tintaAnterior = null;
        for (boolean coincidente : tipos) {
            ColorStroop tinta = colorAlAzarDistintoDe(tintaAnterior, random);
            ColorStroop palabra = coincidente ? tinta : colorAlAzarDistintoDe(tinta, random);
            secuencia.add(new EstimuloStroop(palabra, tinta));
            tintaAnterior = tinta;
        }
        return Collections.unmodifiableList(secuencia);
    }

    public static List<EstimuloStroop> sesion(ConfigStroop config, Random random) {
        return generar(config.coincidentes, config.noCoincidentes, random);
    }

    public static List<EstimuloStroop> practica(ConfigStroop config, Random random) {
        return generar(config.practicaCoincidentes, config.practicaNoCoincidentes, random);
    }

    /** Color al azar distinto de [excluido] (cualquiera si excluido es null). */
    private static ColorStroop colorAlAzarDistintoDe(ColorStroop excluido, Random random) {
        List<ColorStroop> opciones = new ArrayList<>(COLORES.length);
        for (ColorStroop c : COLORES) {
            if (c != excluido) opciones.add(c);
        }
        return opciones.get(random.nextInt(opciones.size()));
    }
}
