package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EvaluadorEstado.Direccion.MAYOR_ES_PEOR;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EvaluadorEstado.Direccion.MENOR_ES_PEOR;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EvaluadorEstado.evaluar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;

public class LineaBaseYEstadoTest {

    private static final double DELTA = 1e-9;
    private final ConfigCognitiva config = new ConfigCognitiva();

    // ---- Línea base ----

    @Test
    public void lineaBase_mediaYDesviacionMuestral() {
        LineaBase b = LineaBase.calcular(Arrays.asList(40.0, 50.0, 60.0), 3);
        assertEquals(50.0, b.getMedia(), DELTA);
        assertEquals(10.0, b.getDesviacion(), DELTA); // sqrt((100+0+100)/2)
        assertEquals(3, b.getNSesiones());
    }

    @Test
    public void lineaBase_menosDe3Sesiones_noHay() {
        assertNull(LineaBase.calcular(Arrays.asList(40.0, 50.0), 3));
    }

    @Test
    public void lineaBase_ignoraSesionesSinLaMetrica() {
        assertNull(LineaBase.calcular(Arrays.asList(40.0, null, 60.0), 3));
        assertEquals(3, LineaBase.calcular(Arrays.asList(40.0, null, 50.0, 60.0), 3).getNSesiones());
    }

    // ---- Estado: efecto Stroop (más alto es peor). Base 50 ± 10 ms ----

    private final LineaBase stroop = new LineaBase(50, 10, 3);

    @Test
    public void stroop_dentroDelRango_normal() {
        assertEquals(EstadoCognitivo.NORMAL, evaluar(55.0, stroop, MAYOR_ES_PEOR, config));
        assertEquals(EstadoCognitivo.NORMAL, evaluar(60.0, stroop, MAYOR_ES_PEOR, config)); // justo 1 desviación
    }

    @Test
    public void stroop_haciaArriba_vigilarYAlerta() {
        assertEquals(EstadoCognitivo.VIGILAR, evaluar(65.0, stroop, MAYOR_ES_PEOR, config));
        assertEquals(EstadoCognitivo.VIGILAR, evaluar(70.0, stroop, MAYOR_ES_PEOR, config)); // justo 2
        assertEquals(EstadoCognitivo.ALERTA, evaluar(75.0, stroop, MAYOR_ES_PEOR, config));
    }

    @Test
    public void stroop_mejorQueLoHabitual_normal() {
        assertEquals(EstadoCognitivo.NORMAL, evaluar(10.0, stroop, MAYOR_ES_PEOR, config));
    }

    // ---- Estado: d′ (más bajo es peor). Base 2,0 ± 0,5 ----

    private final LineaBase nback = new LineaBase(2.0, 0.5, 3);

    @Test
    public void dPrima_haciaAbajo_vigilarYAlerta() {
        assertEquals(EstadoCognitivo.NORMAL, evaluar(1.6, nback, MENOR_ES_PEOR, config));
        assertEquals(EstadoCognitivo.VIGILAR, evaluar(1.2, nback, MENOR_ES_PEOR, config));
        assertEquals(EstadoCognitivo.ALERTA, evaluar(0.8, nback, MENOR_ES_PEOR, config));
    }

    @Test
    public void dPrima_haciaArriba_esMejor_normal() {
        assertEquals(EstadoCognitivo.NORMAL, evaluar(3.5, nback, MENOR_ES_PEOR, config));
    }

    // ---- Casos sin datos ----

    @Test
    public void sinLineaBase_nuncaNormal() {
        assertEquals(EstadoCognitivo.SIN_LINEA_BASE, evaluar(50.0, null, MAYOR_ES_PEOR, config));
    }

    @Test
    public void sinValor_sinDatos() {
        assertEquals(EstadoCognitivo.SIN_DATOS, evaluar(null, stroop, MAYOR_ES_PEOR, config));
    }

    @Test
    public void desviacionCero_cualquierEmpeoramientoEsAlerta() {
        LineaBase plana = new LineaBase(50, 0, 3);
        assertEquals(EstadoCognitivo.NORMAL, evaluar(50.0, plana, MAYOR_ES_PEOR, config));
        assertEquals(EstadoCognitivo.ALERTA, evaluar(51.0, plana, MAYOR_ES_PEOR, config));
    }

    @Test
    public void umbralesDesdeConfiguracion() {
        ConfigCognitiva estricta = new ConfigCognitiva(0, 0.5, 1.0, 3);
        assertEquals(EstadoCognitivo.VIGILAR, evaluar(58.0, stroop, MAYOR_ES_PEOR, estricta));
        assertEquals(EstadoCognitivo.ALERTA, evaluar(62.0, stroop, MAYOR_ES_PEOR, estricta));
    }
}
