package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Rango habitual de un astronauta en una métrica: media ± desviación típica. */
public final class LineaBase {

    private final double media;
    private final double desviacion;
    private final int nSesiones;

    public LineaBase(double media, double desviacion, int nSesiones) {
        this.media = media;
        this.desviacion = desviacion;
        this.nSesiones = nSesiones;
    }

    public double getMedia() {
        return media;
    }

    /** Desviación típica muestral (n − 1). */
    public double getDesviacion() {
        return desviacion;
    }

    public int getNSesiones() {
        return nSesiones;
    }

    /**
     * Calcula la línea base con los valores de las sesiones de línea base. Los null (sesiones sin
     * esa métrica) se ignoran. Devuelve null si quedan menos de [minimo] valores.
     */
    @Nullable
    public static LineaBase calcular(List<Double> valores, int minimo) {
        List<Double> validos = new ArrayList<>();
        for (Double v : valores) {
            if (v != null && !v.isNaN()) validos.add(v);
        }
        int n = validos.size();
        if (n < minimo || n < 2) return null;

        double suma = 0;
        for (double v : validos) suma += v;
        double media = suma / n;

        double cuadrados = 0;
        for (double v : validos) cuadrados += (v - media) * (v - media);
        double desviacion = Math.sqrt(cuadrados / (n - 1));
        return new LineaBase(media, desviacion, n);
    }
}
