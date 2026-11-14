package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EvaluadorEstado.Direccion.MAYOR_ES_PEOR;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EvaluadorEstado.Direccion.MENOR_ES_PEOR;
import static org.junit.Assert.assertEquals;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Tendencia.Sentido;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TendenciaTest {

    @Test
    public void stroop_efectoQueSube_empeorando() {
        // De la más reciente a la más antigua: 110, 100, 90 ms (desviación 10).
        assertEquals(Sentido.EMPEORANDO, Tendencia.calcular(Arrays.asList(110.0, 100.0, 90.0), MAYOR_ES_PEOR, 10));
    }

    @Test
    public void stroop_efectoQueBaja_mejorando() {
        assertEquals(Sentido.MEJORANDO, Tendencia.calcular(Arrays.asList(80.0, 90.0, 100.0), MAYOR_ES_PEOR, 10));
    }

    @Test
    public void dPrima_queBaja_empeorando() {
        assertEquals(Sentido.EMPEORANDO, Tendencia.calcular(Arrays.asList(2.0, 2.4, 2.8), MENOR_ES_PEOR, 0.3));
    }

    @Test
    public void cambioPequeno_estable() {
        // 0,4 desviaciones de cambio < 0,5.
        assertEquals(Sentido.ESTABLE, Tendencia.calcular(Arrays.asList(104.0, 99.0, 100.0), MAYOR_ES_PEOR, 10));
    }

    @Test
    public void soloMiraLasTresUltimas() {
        // La cuarta (muy antigua) no cuenta: 100 → 100 es estable.
        assertEquals(Sentido.ESTABLE, Tendencia.calcular(Arrays.asList(100.0, 120.0, 100.0, 10.0), MAYOR_ES_PEOR, 10));
    }

    @Test
    public void conMenosDeDos_sinDatos() {
        assertEquals(Sentido.SIN_DATOS, Tendencia.calcular(Collections.singletonList(100.0), MAYOR_ES_PEOR, 10));
        assertEquals(Sentido.SIN_DATOS, Tendencia.calcular(Arrays.asList(100.0, null), MAYOR_ES_PEOR, 10));
    }

    @Test
    public void sinLineaBase_comparaElValor() {
        assertEquals(Sentido.EMPEORANDO, Tendencia.calcular(Arrays.asList(1.5, 2.0), MENOR_ES_PEOR, 0));
    }
}
