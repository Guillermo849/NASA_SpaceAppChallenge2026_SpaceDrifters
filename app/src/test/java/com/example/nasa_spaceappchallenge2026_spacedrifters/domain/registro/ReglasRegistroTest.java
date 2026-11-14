package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.registro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReglasRegistroTest {

    private static final double DELTA = 1e-9;

    @Test
    public void sueno_sumaYRestaMediasHoras() {
        assertEquals(7.5, ReglasRegistro.ajustarSueno(7.0, 1), DELTA);
        assertEquals(6.0, ReglasRegistro.ajustarSueno(7.0, -2), DELTA);
    }

    @Test
    public void sueno_noBajaDe0NiPasaDe16() {
        assertEquals(0.0, ReglasRegistro.ajustarSueno(0.0, -1), DELTA);
        assertEquals(16.0, ReglasRegistro.ajustarSueno(16.0, 1), DELTA);
    }

    @Test
    public void sueno_redondeaAMediaHora() {
        // Un valor de los datos de ejemplo (7,3 h) pasa a 7,5 al ajustarlo.
        assertEquals(7.5, ReglasRegistro.redondearSueno(7.3), DELTA);
        assertEquals(7.0, ReglasRegistro.redondearSueno(7.2), DELTA);
        assertEquals(8.0, ReglasRegistro.ajustarSueno(7.3, 1), DELTA);
    }

    @Test
    public void somnolencia_escalaDe1a9() {
        assertTrue(ReglasRegistro.somnolenciaValida(1));
        assertTrue(ReglasRegistro.somnolenciaValida(9));
        assertFalse(ReglasRegistro.somnolenciaValida(0));
        assertFalse(ReglasRegistro.somnolenciaValida(10));
    }

    @Test
    public void agua_sinAnotarEmpiezaEnElObjetivo() {
        assertEquals(2.0, ReglasRegistro.ajustarAgua(null, 1, 2.0), DELTA);
        assertEquals(2.0, ReglasRegistro.ajustarAgua(null, -1, 2.0), DELTA);
    }

    @Test
    public void agua_cuartosDeLitroEntre0y6() {
        assertEquals(2.25, ReglasRegistro.ajustarAgua(2.0, 1, 2.0), DELTA);
        assertEquals(1.5, ReglasRegistro.ajustarAgua(2.0, -2, 2.0), DELTA);
        assertEquals(0.0, ReglasRegistro.ajustarAgua(0.0, -1, 2.0), DELTA);
        assertEquals(6.0, ReglasRegistro.ajustarAgua(6.0, 1, 2.0), DELTA);
        // 1,3 L de los datos de ejemplo → 1,25 + 0,25
        assertEquals(1.5, ReglasRegistro.ajustarAgua(1.3, 1, 2.0), DELTA);
    }

    @Test
    public void ejercicio_cuartosDeHoraEntre0y300() {
        assertEquals(120, ReglasRegistro.ajustarEjercicio(null, 1, 120));
        assertEquals(135, ReglasRegistro.ajustarEjercicio(120, 1, 120));
        assertEquals(0, ReglasRegistro.ajustarEjercicio(0, -1, 120));
        assertEquals(300, ReglasRegistro.ajustarEjercicio(300, 1, 120));
    }

    @Test
    public void ingesta_soloCuartos() {
        assertTrue(ReglasRegistro.ingestaValida(0));
        assertTrue(ReglasRegistro.ingestaValida(75));
        assertFalse(ReglasRegistro.ingestaValida(60));
    }

    @Test
    public void objetivo_sinDatosNoEsNiBienNiMal() {
        assertNull(ReglasRegistro.cumpleObjetivo(null, 2.0));
        assertTrue(ReglasRegistro.cumpleObjetivo(2.0, 2.0));
        assertFalse(ReglasRegistro.cumpleObjetivo(1.75, 2.0));
    }
}
