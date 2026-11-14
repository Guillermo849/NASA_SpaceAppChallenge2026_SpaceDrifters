package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop.AMARILLO;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop.AZUL;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop.ROJO;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop.VERDE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.TemporizadorFalso;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ControladorStroopTest {

    private final ConfigStroop config = new ConfigStroop();
    private final TemporizadorFalso temporizador = new TemporizadorFalso();
    private final List<FaseStroop> fases = new ArrayList<>();

    // ROJO en tinta azul (no coincidente) y VERDE en verde (coincidente).
    private final EstimuloStroop rojoEnAzul = new EstimuloStroop(ROJO, AZUL);
    private final EstimuloStroop verdeEnVerde = new EstimuloStroop(VERDE, VERDE);
    private final List<EstimuloStroop> secuencia = Arrays.asList(rojoEnAzul, verdeEnVerde);

    private static long ms(long valor) {
        return valor * 1_000_000L;
    }

    private ControladorStroop crear(boolean esPractica) {
        return new ControladorStroop(secuencia, esPractica, config, temporizador, fases::add);
    }

    /** Empieza la sesión y deja pasar la cruz de fijación hasta que aparece el primer estímulo. */
    private ControladorStroop empezar(boolean esPractica) {
        ControladorStroop c = crear(esPractica);
        c.empezar();
        assertEquals(new FaseStroop.Fijacion(0), c.getFase());
        temporizador.avanzar(config.fijacionMs);
        assertEquals(new FaseStroop.Estimulo(0, rojoEnAzul), c.getFase());
        return c;
    }

    @Test
    public void acierto_mideElTiempoDesdeQueSeDibuja() {
        ControladorStroop c = empezar(false);
        c.estimuloMostrado(0, ms(10_000));
        c.responder(AZUL, ms(10_450));

        ResultadoEnsayoStroop r = c.getResultados().get(0);
        assertEquals(1, c.getResultados().size());
        assertEquals(AZUL, r.getRespuesta());
        assertTrue(r.esCorrecto());
        assertEquals(Long.valueOf(450), r.getTiempoMs());
        // Sin práctica no hay aviso: pasa directamente a la siguiente cruz.
        assertEquals(new FaseStroop.Fijacion(1), c.getFase());
    }

    @Test
    public void error_tocarLaPalabraEnVezDeLaTinta() {
        ControladorStroop c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        c.responder(ROJO, ms(1_700));

        ResultadoEnsayoStroop r = c.getResultados().get(0);
        assertEquals(ROJO, r.getRespuesta());
        assertFalse(r.esCorrecto());
        assertEquals(Long.valueOf(700), r.getTiempoMs());
    }

    @Test
    public void omision_sinRespuestaEn3Segundos() {
        ControladorStroop c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        temporizador.avanzar(config.limiteRespuestaMs - 1);
        assertTrue(c.getResultados().isEmpty());

        temporizador.avanzar(1);
        ResultadoEnsayoStroop r = c.getResultados().get(0);
        assertTrue(r.esOmision());
        assertNull(r.getRespuesta());
        assertNull(r.getTiempoMs());
        assertFalse(r.esCorrecto());
    }

    @Test
    public void elLimiteNoEmpiezaHastaQueElEstimuloSeDibuja() {
        ControladorStroop c = empezar(false);
        temporizador.avanzar(config.limiteRespuestaMs * 2);
        assertTrue(c.getResultados().isEmpty());
        assertEquals(new FaseStroop.Estimulo(0, rojoEnAzul), c.getFase());
    }

    @Test
    public void soloCuentaLaPrimeraRespuesta() {
        ControladorStroop c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        c.responder(AMARILLO, ms(1_300));
        c.responder(AZUL, ms(1_400));
        assertEquals(1, c.getResultados().size());
        assertEquals(AMARILLO, c.getResultados().get(0).getRespuesta());
    }

    @Test
    public void seIgnoranToquesDuranteLaFijacionOAntesDelDibujo() {
        ControladorStroop c = crear(false);
        c.empezar();
        c.responder(AZUL, ms(100));          // durante la cruz
        temporizador.avanzar(config.fijacionMs);
        c.responder(AZUL, ms(900));          // estímulo aún no dibujado
        c.estimuloMostrado(0, ms(1_000));
        c.responder(AZUL, ms(999));          // evento anterior al frame
        assertTrue(c.getResultados().isEmpty());

        c.responder(AZUL, ms(1_200));
        assertEquals(Long.valueOf(200), c.getResultados().get(0).getTiempoMs());
    }

    @Test
    public void estimuloMostradoDeOtroEnsayoSeIgnora() {
        ControladorStroop c = empezar(false);
        c.estimuloMostrado(5, ms(1_000));
        c.responder(AZUL, ms(1_200));
        assertTrue(c.getResultados().isEmpty());
    }

    @Test
    public void practica_muestraAvisoDeAciertoOFallo() {
        ControladorStroop c = empezar(true);
        c.estimuloMostrado(0, ms(1_000));
        c.responder(AZUL, ms(1_500));

        assertTrue(c.getFase() instanceof FaseStroop.Feedback);
        assertTrue(((FaseStroop.Feedback) c.getFase()).getResultado().esCorrecto());

        temporizador.avanzar(config.feedbackMs - 1);
        assertTrue(c.getFase() instanceof FaseStroop.Feedback);
        temporizador.avanzar(1);
        assertEquals(new FaseStroop.Fijacion(1), c.getFase());
    }

    @Test
    public void practica_avisoTambienEnOmision() {
        ControladorStroop c = empezar(true);
        c.estimuloMostrado(0, ms(1_000));
        temporizador.avanzar(config.limiteRespuestaMs);
        assertTrue(((FaseStroop.Feedback) c.getFase()).getResultado().esOmision());
    }

    @Test
    public void terminaTrasTodosLosEnsayos() {
        ControladorStroop c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        c.responder(AZUL, ms(1_400));
        temporizador.avanzar(config.fijacionMs);
        c.estimuloMostrado(1, ms(3_000));
        c.responder(VERDE, ms(3_350));

        assertTrue(c.getFase() instanceof FaseStroop.Terminada);
        assertTrue(fases.get(fases.size() - 1) instanceof FaseStroop.Terminada);
        List<ResultadoEnsayoStroop> rs = c.getResultados();
        assertEquals(2, rs.size());
        assertEquals(1, rs.get(0).getNumero());
        assertEquals(2, rs.get(1).getNumero());
        assertEquals(Long.valueOf(400), rs.get(0).getTiempoMs());
        assertEquals(Long.valueOf(350), rs.get(1).getTiempoMs());
    }

    @Test
    public void detener_ignoraTodoLoQueVieneDespues() {
        ControladorStroop c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        c.detener();
        c.responder(AZUL, ms(1_200));
        temporizador.avanzar(60_000);
        assertTrue(c.getResultados().isEmpty());
        assertEquals(new FaseStroop.Estimulo(0, rojoEnAzul), c.getFase());
    }
}
