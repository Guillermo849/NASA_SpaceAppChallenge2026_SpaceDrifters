package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.registro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}
