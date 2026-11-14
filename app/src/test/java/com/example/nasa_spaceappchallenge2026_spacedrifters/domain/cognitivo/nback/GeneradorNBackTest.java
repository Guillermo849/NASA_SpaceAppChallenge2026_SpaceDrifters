package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.Random;

public class GeneradorNBackTest {

    private final ConfigNBack config = new ConfigNBack();

    @Test
    public void sesion_42Estimulosy12CoincidenciasExactas() {
        for (int semilla = 0; semilla < 300; semilla++) {
            SecuenciaNBack s = GeneradorNBack.sesion(config, new Random(semilla));
            assertEquals(2, s.getNivel());
            assertEquals(42, s.size());
            assertEquals("semilla " + semilla, 12, s.contarCoincidencias());
        }
    }

    @Test
    public void losDosPrimerosNoSonCoincidencia() {
        SecuenciaNBack s = GeneradorNBack.sesion(config, new Random(4));
        assertFalse(s.esCoincidencia(0));
        assertFalse(s.esCoincidencia(1));
    }

    @Test
    public void nuncaSeRepiteLaMismaFormaSeguida() {
        for (int semilla = 0; semilla < 300; semilla++) {
            SecuenciaNBack s = GeneradorNBack.sesion(config, new Random(semilla));
            for (int i = 1; i < s.size(); i++) {
                assertNotEquals("semilla " + semilla + ", estímulo " + i, s.get(i - 1), s.get(i));
            }
        }
    }

    @Test
    public void cadaSesionTieneUnaSecuenciaNueva() {
        assertNotEquals(
                GeneradorNBack.sesion(config, new Random(1)),
                GeneradorNBack.sesion(config, new Random(2)));
    }

    @Test
    public void practicas_1Backy2Back() {
        for (int semilla = 0; semilla < 100; semilla++) {
            SecuenciaNBack p1 = GeneradorNBack.practica1Back(config, new Random(semilla));
            assertEquals(1, p1.getNivel());
            assertEquals(15, p1.size());
            assertEquals(4, p1.contarCoincidencias());

            SecuenciaNBack p2 = GeneradorNBack.practica2Back(config, new Random(semilla));
            assertEquals(2, p2.getNivel());
            assertEquals(10, p2.size());
            assertEquals(3, p2.contarCoincidencias());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void masCoincidenciasQuePosicionesEsImposible() {
        GeneradorNBack.generar(2, 10, 9, new Random(0));
    }
}
