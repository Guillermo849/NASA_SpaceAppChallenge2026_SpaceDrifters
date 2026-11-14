package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop.AZUL;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop.ROJO;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop.VERDE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MetricasStroopTest {

    private static final double DELTA = 1e-9;

    private static final EstimuloStroop COINCIDENTE = new EstimuloStroop(VERDE, VERDE);
    private static final EstimuloStroop NO_COINCIDENTE = new EstimuloStroop(ROJO, AZUL);

    private static ResultadoEnsayoStroop ensayo(int n, EstimuloStroop e, ColorStroop respuesta, Long ms) {
        return new ResultadoEnsayoStroop(n, e, respuesta, ms);
    }

    @Test
    public void efectoStroop_noCoincidentesMenosCoincidentes_soloAciertos() {
        List<ResultadoEnsayoStroop> rs = Arrays.asList(
                ensayo(1, COINCIDENTE, VERDE, 500L),
                ensayo(2, COINCIDENTE, VERDE, 600L),         // media coincidentes = 550
                ensayo(3, NO_COINCIDENTE, AZUL, 700L),
                ensayo(4, NO_COINCIDENTE, AZUL, 800L),       // media no coincidentes = 750
                ensayo(5, NO_COINCIDENTE, ROJO, 300L),       // error: no cuenta en tiempos
                ensayo(6, COINCIDENTE, null, null));         // omisión

        MetricasStroop m = MetricasStroop.calcular(rs);
        assertEquals(200.0, m.getEfectoStroopMs(), DELTA);
        assertEquals(650.0, m.getTiempoMedioMs(), DELTA);       // (500+600+700+800)/4
        assertEquals(100.0 * 4 / 6, m.getPorcentajeAciertos(), DELTA);
        assertEquals(1, m.getErroresNoCoincidentes());
    }

    @Test
    public void todoAciertos_100PorCiento() {
        MetricasStroop m = MetricasStroop.calcular(Arrays.asList(
                ensayo(1, COINCIDENTE, VERDE, 400L),
                ensayo(2, NO_COINCIDENTE, AZUL, 450L)));
        assertEquals(100.0, m.getPorcentajeAciertos(), DELTA);
        assertEquals(50.0, m.getEfectoStroopMs(), DELTA);
        assertEquals(0, m.getErroresNoCoincidentes());
    }

    @Test
    public void ningunAcierto_0PorCientoYSinEfecto() {
        MetricasStroop m = MetricasStroop.calcular(Arrays.asList(
                ensayo(1, COINCIDENTE, null, null),
                ensayo(2, NO_COINCIDENTE, ROJO, 500L)));
        assertEquals(0.0, m.getPorcentajeAciertos(), DELTA);
        assertNull(m.getEfectoStroopMs());
        assertNull(m.getTiempoMedioMs());
        assertEquals(1, m.getErroresNoCoincidentes());
    }

    @Test
    public void sinAciertosNoCoincidentes_efectoNull() {
        MetricasStroop m = MetricasStroop.calcular(Collections.singletonList(ensayo(1, COINCIDENTE, VERDE, 400L)));
        assertNull(m.getEfectoStroopMs());
        assertEquals(400.0, m.getTiempoMedioMs(), DELTA);
    }

    @Test
    public void sesionVacia() {
        MetricasStroop m = MetricasStroop.calcular(Collections.emptyList());
        assertNull(m.getPorcentajeAciertos());
        assertNull(m.getEfectoStroopMs());
    }
}
