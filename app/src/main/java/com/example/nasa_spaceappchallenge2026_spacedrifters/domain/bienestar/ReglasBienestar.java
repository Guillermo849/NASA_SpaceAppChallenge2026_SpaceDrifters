package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Check-in de bienestar psicológico: ánimo, estrés y sensación de aislamiento en escalas de 1 a 5,
 * y qué herramienta sugerir según las respuestas. Las sugerencias son solo ayudas en pantalla; las
 * alertas al médico salen de assets/reglas.json (regla BIENESTAR_BAJO).
 */
public final class ReglasBienestar {

    public static final int MIN = 1;
    public static final int MAX = 5;

    /** Ánimo igual o por debajo: se sugiere hablar con apoyo psicológico. */
    public static final int ANIMO_BAJO = 2;
    /** Estrés igual o por encima: se sugiere la respiración guiada. */
    public static final int ESTRES_ALTO = 4;
    /** Aislamiento igual o por encima: se sugieren ideas para sentirse conectado. */
    public static final int AISLAMIENTO_ALTO = 4;

    /** Herramientas de la pantalla de bienestar. */
    public enum Sugerencia { HABLAR_APOYO, RESPIRACION, CONEXION }

    private ReglasBienestar() {
    }

    public static boolean valida(int valor) {
        return valor >= MIN && valor <= MAX;
    }

    /**
     * Herramientas a sugerir, de la más importante a la menos. Vacía si todo va bien o no hay datos
     * (sin datos no se sugiere nada: no se da por bueno ni por malo).
     */
    public static List<Sugerencia> sugerencias(@Nullable Integer animo, @Nullable Integer estres,
                                               @Nullable Integer aislamiento) {
        List<Sugerencia> lista = new ArrayList<>();
        if (animo != null && animo <= ANIMO_BAJO) lista.add(Sugerencia.HABLAR_APOYO);
        if (estres != null && estres >= ESTRES_ALTO) lista.add(Sugerencia.RESPIRACION);
        if (aislamiento != null && aislamiento >= AISLAMIENTO_ALTO) lista.add(Sugerencia.CONEXION);
        return lista;
    }
}
