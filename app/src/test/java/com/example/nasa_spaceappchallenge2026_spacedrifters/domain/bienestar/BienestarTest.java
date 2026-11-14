package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.JuegoParejas.Resultado;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.ReglasBienestar.Sugerencia;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.RespiracionGuiada.Fase;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class BienestarTest {

    // ---- Check-in y sugerencias ----

    @Test
    public void escala_de1a5() {
        assertTrue(ReglasBienestar.valida(1));
        assertTrue(ReglasBienestar.valida(5));
        assertFalse(ReglasBienestar.valida(0));
        assertFalse(ReglasBienestar.valida(6));
    }

    @Test
    public void sugerencias_segunRespuestas() {
        assertEquals(Arrays.asList(Sugerencia.HABLAR_APOYO, Sugerencia.RESPIRACION, Sugerencia.CONEXION),
                ReglasBienestar.sugerencias(2, 4, 4));
        assertEquals(Collections.singletonList(Sugerencia.RESPIRACION), ReglasBienestar.sugerencias(4, 5, 1));
        assertTrue(ReglasBienestar.sugerencias(3, 3, 3).isEmpty());
    }

    @Test
    public void sugerencias_sinDatosNoSugiereNada() {
        assertTrue(ReglasBienestar.sugerencias(null, null, null).isEmpty());
    }

    // ---- Respiración en caja ----

    @Test
    public void respiracion_fasesDe4Segundos() {
        RespiracionGuiada r = new RespiracionGuiada(2);
        assertEquals(Fase.INSPIRA, r.estado(0).getFase());
        assertEquals(4, r.estado(0).getSegundosRestantesFase());
        assertEquals(1, r.estado(3_500).getSegundosRestantesFase());
        assertEquals(Fase.MANTEN_LLENO, r.estado(4_000).getFase());
        assertEquals(Fase.ESPIRA, r.estado(8_000).getFase());
        assertEquals(Fase.MANTEN_VACIO, r.estado(12_000).getFase());
        assertEquals(2, r.estado(16_000).getCiclo());
        assertEquals(Fase.INSPIRA, r.estado(16_000).getFase());
    }

    @Test
    public void respiracion_elCirculoCreceAlInspirarYEncogeAlEspirar() {
        RespiracionGuiada r = new RespiracionGuiada(1);
        assertEquals(RespiracionGuiada.TAMANO_MIN, r.estado(0).getTamano(), 1e-6);
        assertTrue(r.estado(2_000).getTamano() > r.estado(1_000).getTamano());
        assertEquals(1f, r.estado(5_000).getTamano(), 1e-6);
        assertTrue(r.estado(10_000).getTamano() < r.estado(9_000).getTamano());
    }

    @Test
    public void respiracion_terminaTrasLosCiclos() {
        RespiracionGuiada r = new RespiracionGuiada(RespiracionGuiada.ciclosEnMinutos(1));
        assertEquals(3, r.getCiclos()); // 3 ciclos de 16 s caben en un minuto
        assertFalse(r.estado(r.getDuracionMs() - 1).isTerminado());
        assertTrue(r.estado(r.getDuracionMs()).isTerminado());
    }

    // ---- Parejas orbitales ----

    private static int pareja(JuegoParejas j, int carta) {
        for (int i = 0; i < j.getNumCartas(); i++) {
            if (i != carta && j.simbolo(i) == j.simbolo(carta)) return i;
        }
        throw new AssertionError("sin pareja");
    }

    private static int distinta(JuegoParejas j, int carta) {
        for (int i = 0; i < j.getNumCartas(); i++) {
            if (j.simbolo(i) != j.simbolo(carta)) return i;
        }
        throw new AssertionError();
    }

    @Test
    public void parejas_mazoConDosDeCada() {
        JuegoParejas j = new JuegoParejas(6, new Random(1));
        assertEquals(12, j.getNumCartas());
        int[] cuenta = new int[6];
        for (int i = 0; i < 12; i++) cuenta[j.simbolo(i)]++;
        for (int c : cuenta) assertEquals(2, c);
    }

    @Test
    public void parejas_aciertoSeQuedaBocaArriba() {
        JuegoParejas j = new JuegoParejas(6, new Random(2));
        int otra = pareja(j, 0);
        assertEquals(Resultado.PRIMERA, j.voltear(0));
        assertEquals(Resultado.PAREJA, j.voltear(otra));
        assertTrue(j.estaEmparejada(0));
        assertTrue(j.estaBocaArriba(otra));
        assertEquals(1, j.getIntentos());
        assertEquals(1, j.getParejasEncontradas());
    }

    @Test
    public void parejas_falloSeVeHastaOcultarlo() {
        JuegoParejas j = new JuegoParejas(6, new Random(3));
        int otra = distinta(j, 0);
        j.voltear(0);
        assertEquals(Resultado.FALLO, j.voltear(otra));
        assertTrue(j.estaBocaArriba(0) && j.estaBocaArriba(otra));
        // Mientras se ve el fallo, no se pueden levantar más cartas.
        assertEquals(Resultado.IGNORADA, j.voltear(pareja(j, 0)));
        j.ocultarFallo();
        assertFalse(j.estaBocaArriba(0) || j.estaBocaArriba(otra));
    }

    @Test
    public void parejas_laMismaCartaDosVecesNoCuenta() {
        JuegoParejas j = new JuegoParejas(6, new Random(4));
        j.voltear(5);
        assertEquals(Resultado.IGNORADA, j.voltear(5));
        assertEquals(0, j.getIntentos());
    }

    @Test
    public void parejas_terminaAlEncontrarlasTodas() {
        JuegoParejas j = new JuegoParejas(3, new Random(5));
        for (int i = 0; i < j.getNumCartas(); i++) {
            if (j.estaEmparejada(i)) continue;
            j.voltear(i);
            j.voltear(pareja(j, i));
        }
        assertTrue(j.isTerminado());
        assertEquals(3, j.getIntentos());
        assertEquals(Resultado.IGNORADA, j.voltear(0));
    }
}
