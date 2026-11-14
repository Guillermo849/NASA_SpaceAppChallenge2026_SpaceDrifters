package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.List;
import java.util.Random;

public class GeneradorStroopTest {

    private final ConfigStroop config = new ConfigStroop();

    private static long contarCoincidentes(List<EstimuloStroop> secuencia) {
        return secuencia.stream().filter(EstimuloStroop::esCoincidente).count();
    }

    @Test
    public void sesion_tiene20Coincidentesy20NoCoincidentes() {
        for (int semilla = 0; semilla < 200; semilla++) {
            List<EstimuloStroop> secuencia = GeneradorStroop.sesion(config, new Random(semilla));
            assertEquals(40, secuencia.size());
            assertEquals(20, contarCoincidentes(secuencia));
        }
    }

    @Test
    public void nuncaSeRepiteLaTintaDosVecesSeguidas() {
        for (int semilla = 0; semilla < 200; semilla++) {
            List<EstimuloStroop> secuencia = GeneradorStroop.sesion(config, new Random(semilla));
            for (int i = 1; i < secuencia.size(); i++) {
                assertNotEquals("semilla " + semilla + ", ensayo " + i,
                        secuencia.get(i - 1).getTinta(), secuencia.get(i).getTinta());
            }
        }
    }

    @Test
    public void palabraYTintaSegunElTipo() {
        for (EstimuloStroop e : GeneradorStroop.sesion(config, new Random(7))) {
            if (e.esCoincidente()) {
                assertEquals(e.getPalabra(), e.getTinta());
            } else {
                assertNotEquals(e.getPalabra(), e.getTinta());
            }
        }
    }

    @Test
    public void cadaSesionTieneUnaSecuenciaNueva() {
        assertNotEquals(
                GeneradorStroop.sesion(config, new Random(1)),
                GeneradorStroop.sesion(config, new Random(2)));
    }

    @Test
    public void practica_tiene8Ensayos4y4() {
        List<EstimuloStroop> practica = GeneradorStroop.practica(config, new Random(3));
        assertEquals(8, practica.size());
        assertEquals(4, contarCoincidentes(practica));
    }
}
