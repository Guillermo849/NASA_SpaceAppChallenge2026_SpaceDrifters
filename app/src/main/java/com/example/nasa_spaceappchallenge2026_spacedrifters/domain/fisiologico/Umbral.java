package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import java.util.Objects;

/**
 * Cuándo una constante se considera alterada (assets/config_umbrales.json): si se aleja de la línea
 * base de esa persona más que la desviación (en % o en puntos) o si pasa el umbral absoluto de
 * seguridad. Basta con una de las dos.
 */
public final class Umbral {

    /** Hacia dónde es malo: SUBE = alterado por encima, BAJA = por debajo. */
    public enum Sentido { SUBE, BAJA }

    private final String hecho;
    private final String metrica;
    private final Sentido sentido;
    private final Double desviacionPorcentaje;
    private final Double desviacionPuntos;
    private final Double absoluto;

    /**
     * @param hecho   nombre del hecho que produce, p. ej. "FC_ALTA"
     * @param metrica constante que mira, p. ej. "FC"
     * @param desviacionPorcentaje % respecto a la base (null = no se usa)
     * @param desviacionPuntos     puntos respecto a la base, p. ej. 3 para SpO2 (null = no se usa)
     * @param absoluto             umbral de seguridad sin mirar la base (null = no hay)
     */
    public Umbral(String hecho, String metrica, Sentido sentido,
                  Double desviacionPorcentaje, Double desviacionPuntos, Double absoluto) {
        this.hecho = Objects.requireNonNull(hecho);
        this.metrica = Objects.requireNonNull(metrica);
        this.sentido = Objects.requireNonNull(sentido);
        this.desviacionPorcentaje = desviacionPorcentaje;
        this.desviacionPuntos = desviacionPuntos;
        this.absoluto = absoluto;
        if (desviacionPorcentaje == null && desviacionPuntos == null && absoluto == null) {
            throw new IllegalArgumentException(hecho + ": falta la desviación o el umbral absoluto");
        }
    }

    public String getHecho() { return hecho; }
    public String getMetrica() { return metrica; }
    public Sentido getSentido() { return sentido; }
    public Double getDesviacionPorcentaje() { return desviacionPorcentaje; }
    public Double getDesviacionPuntos() { return desviacionPuntos; }
    public Double getAbsoluto() { return absoluto; }

    /**
     * Límite que marca su base: base ± % o base ± puntos. null si no hay base o no se usa desviación.
     */
    public Double limiteDesdeBase(Double base) {
        if (base == null) return null;
        double signo = sentido == Sentido.SUBE ? 1 : -1;
        if (desviacionPorcentaje != null) return base * (1 + signo * desviacionPorcentaje / 100.0);
        if (desviacionPuntos != null) return base + signo * desviacionPuntos;
        return null;
    }

    /**
     * true = alterado, false = dentro de lo esperado, null = no se puede saber (falta el valor, o
     * falta la base y no hay umbral absoluto).
     */
    public Boolean alterado(Double valor, Double base) {
        if (valor == null) return null;
        Double limite = limiteDesdeBase(base);
        if (limite == null && absoluto == null) return null;
        boolean porBase = limite != null && pasa(valor, limite);
        boolean porAbsoluto = absoluto != null && pasa(valor, absoluto);
        return porBase || porAbsoluto;
    }

    private boolean pasa(double valor, double limite) {
        return sentido == Sentido.SUBE ? valor > limite : valor < limite;
    }
}
