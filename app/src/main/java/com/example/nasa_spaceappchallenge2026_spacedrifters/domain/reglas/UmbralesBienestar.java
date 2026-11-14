package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas;

import androidx.annotation.Nullable;

/**
 * Umbrales de la regla BIENESTAR_BAJO (escalas 1–5 del check-in). Un día cuenta si se cumple
 * alguno; un umbral null no se mira.
 */
public final class UmbralesBienestar {
    @Nullable private final Integer animoMax;
    @Nullable private final Integer estresMin;
    @Nullable private final Integer aislamientoMin;

    public UmbralesBienestar(@Nullable Integer animoMax, @Nullable Integer estresMin, @Nullable Integer aislamientoMin) {
        if (animoMax == null && estresMin == null && aislamientoMin == null) {
            throw new IllegalArgumentException("sin umbrales de bienestar");
        }
        this.animoMax = animoMax;
        this.estresMin = estresMin;
        this.aislamientoMin = aislamientoMin;
    }

    /** Ánimo igual o por debajo cuenta. */
    @Nullable public Integer getAnimoMax() { return animoMax; }
    /** Estrés igual o por encima cuenta. */
    @Nullable public Integer getEstresMin() { return estresMin; }
    /** Aislamiento igual o por encima cuenta. */
    @Nullable public Integer getAislamientoMin() { return aislamientoMin; }

    public boolean animoBajo(@Nullable Integer animo) {
        return animoMax != null && animo != null && animo <= animoMax;
    }

    public boolean estresAlto(@Nullable Integer estres) {
        return estresMin != null && estres != null && estres >= estresMin;
    }

    public boolean aislamientoAlto(@Nullable Integer aislamiento) {
        return aislamientoMin != null && aislamiento != null && aislamiento >= aislamientoMin;
    }
}
