package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.DPrima;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MetricasNBackTest {

    private static final double DELTA = 1e-9;

    /**
     * Sesión 2-back de 42 estímulos: 2 iniciales + 12 coincidencias + 28 no coincidencias.
     * [aciertos] coincidencias pulsadas (a 500 ms) y [falsasAlarmas] no coincidencias pulsadas.
     * Los iniciales se pulsan siempre, para comprobar que no cuentan.
     */
    private static List<ResultadoEnsayoNBack> sesion(int aciertos, int falsasAlarmas) {
        List<ResultadoEnsayoNBack> rs = new ArrayList<>();
        int n = 1;
        rs.add(new ResultadoEnsayoNBack(n++, FormaNBack.CRUZ, false, 300L));
        rs.add(new ResultadoEnsayoNBack(n++, FormaNBack.ROMBO, false, 300L));
        for (int i = 0; i < 12; i++) {
            rs.add(new ResultadoEnsayoNBack(n++, FormaNBack.CIRCULO, true, i < aciertos ? 500L : null));
        }
        for (int i = 0; i < 28; i++) {
            rs.add(new ResultadoEnsayoNBack(n++, FormaNBack.ESTRELLA, false, i < falsasAlarmas ? 700L : null));
        }
        return rs;
    }

    @Test
    public void metricasNormales() {
        MetricasNBack m = MetricasNBack.calcular(sesion(9, 3), 2);
        assertEquals(75.0, m.getAciertosCoincidencias(), DELTA);
        assertEquals(3, m.getFalsasAlarmas());
        assertEquals(500.0, m.getTiempoAciertosMs(), DELTA);
        assertEquals(DPrima.calcular(9, 12, 3, 28), m.getDPrima(), DELTA);
    }

    @Test
    public void cienPorCien_dPrimaFinitaYAlta() {
        MetricasNBack m = MetricasNBack.calcular(sesion(12, 0), 2);
        assertEquals(100.0, m.getAciertosCoincidencias(), DELTA);
        assertEquals(0, m.getFalsasAlarmas());
        assertTrue(Double.isFinite(m.getDPrima()));
        assertTrue(m.getDPrima() > 3);
    }

    @Test
    public void ceroPorCiento_todoFalsasAlarmas_dPrimaFinitaYNegativa() {
        MetricasNBack m = MetricasNBack.calcular(sesion(0, 28), 2);
        assertEquals(0.0, m.getAciertosCoincidencias(), DELTA);
        assertEquals(28, m.getFalsasAlarmas());
        assertNull(m.getTiempoAciertosMs());
        assertTrue(Double.isFinite(m.getDPrima()));
        assertTrue(m.getDPrima() < -3);
    }

    @Test
    public void sinPulsarNada_dPrimaCercaDeCero() {
        MetricasNBack m = MetricasNBack.calcular(sesion(0, 0), 2);
        assertEquals(0.0, m.getAciertosCoincidencias(), DELTA);
        // Misma tasa corregida baja en aciertos y falsas alarmas → d′ pequeña.
        assertTrue(Math.abs(m.getDPrima()) < 1);
    }

    @Test
    public void losEstimulosInicialesNoCuentan() {
        // Los 2 iniciales están pulsados, pero no son falsas alarmas.
        assertEquals(0, MetricasNBack.calcular(sesion(12, 0), 2).getFalsasAlarmas());
    }

    @Test
    public void dPrima_mismaTasaDeAciertosYFalsasAlarmas_esCero() {
        // (6 + 0,5) / 13 = 0,5 y (14 + 0,5) / 29 = 0,5 → no distingue nada.
        assertEquals(0.0, DPrima.calcular(6, 12, 14, 28), 1e-6);
    }
}
