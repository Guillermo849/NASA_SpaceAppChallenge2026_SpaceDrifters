package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Métricas de una sesión PVT calculadas a partir de los ensayos individuales.
 *
 * Métricas estándar del PVT-B validadas por NASA/NSBRI:
 * - Tiempo de reacción medio (todos los ensayos válidos)
 * - 10% más lentos (percentil 90): marcador sensible de fatiga
 * - 1/TR media (throughput recíproco): métrica robusta ante distribuciones asimétricas
 * - Número de lapsos (TR ≥ 500 ms): el marcador clásico de vigilancia
 * - Número de anticipaciones (falsas alarmas)
 *
 * La métrica principal para comparar con la línea base es {@code trMedioMs}
 * (mayor = peor, dirección MAYOR_ES_PEOR).
 */
public final class MetricasPvt {

    @Nullable private final Double trMedioMs;
    @Nullable private final Double trP90Ms;
    @Nullable private final Double trReciprocoMedio;   // 1/TR en 1/ms, media
    private final int lapsos;
    private final int anticipaciones;
    private final int ensayosValidos;

    private MetricasPvt(@Nullable Double trMedioMs, @Nullable Double trP90Ms,
                        @Nullable Double trReciprocoMedio,
                        int lapsos, int anticipaciones, int ensayosValidos) {
        this.trMedioMs = trMedioMs;
        this.trP90Ms = trP90Ms;
        this.trReciprocoMedio = trReciprocoMedio;
        this.lapsos = lapsos;
        this.anticipaciones = anticipaciones;
        this.ensayosValidos = ensayosValidos;
    }

    /** Media de los TR válidos en ms; null si no hay ninguno. */
    @Nullable public Double getTrMedioMs() { return trMedioMs; }

    /** Percentil 90 de los TR válidos (10% más lentos); null si < 10 ensayos válidos. */
    @Nullable public Double getTrP90Ms() { return trP90Ms; }

    /** Media del recíproco de los TR válidos (1/TR en 1/ms); null si no hay ninguno. */
    @Nullable public Double getTrReciprocoMedio() { return trReciprocoMedio; }

    /** Número de lapsos (TR ≥ 500 ms). */
    public int getLapsos() { return lapsos; }

    /** Número de anticipaciones (TR < 100 ms). */
    public int getAnticipaciones() { return anticipaciones; }

    /** Ensayos con TR válido. */
    public int getEnsayosValidos() { return ensayosValidos; }

    public static MetricasPvt calcular(List<ResultadoEnsayoPvt> resultados) {
        int lapsos = 0;
        int anticipaciones = 0;
        int validos = 0;
        double sumaTr = 0;
        double sumaReciprocas = 0;

        // Recogemos los TR válidos para el percentil 90.
        double[] trsValidos = new double[resultados.size()];
        int idx = 0;

        for (ResultadoEnsayoPvt r : resultados) {
            if (r.isAnticipacion()) {
                anticipaciones++;
            } else if (r.esLapso()) {
                lapsos++;
            } else if (r.esValido() && r.getTiempoMs() != null) {
                double t = r.getTiempoMs();
                sumaTr += t;
                sumaReciprocas += 1.0 / t;
                trsValidos[idx++] = t;
                validos++;
            }
        }

        Double trMedio = validos > 0 ? sumaTr / validos : null;
        Double trReciprocaMedio = validos > 0 ? sumaReciprocas / validos : null;

        // Percentil 90 (10% más lentos) solo si hay ≥ 10 ensayos válidos.
        Double trP90 = null;
        if (validos >= 10) {
            double[] datos = java.util.Arrays.copyOf(trsValidos, validos);
            java.util.Arrays.sort(datos);
            int p90Idx = (int) Math.ceil(0.9 * validos) - 1;
            trP90 = datos[p90Idx];
        }

        return new MetricasPvt(trMedio, trP90, trReciprocaMedio, lapsos, anticipaciones, validos);
    }
}
