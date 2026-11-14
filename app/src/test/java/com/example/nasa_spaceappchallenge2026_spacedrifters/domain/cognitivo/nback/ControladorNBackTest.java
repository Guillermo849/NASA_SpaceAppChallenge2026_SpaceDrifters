package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FormaNBack.CIRCULO;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FormaNBack.CUADRADO;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FormaNBack.TRIANGULO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.TemporizadorFalso;

import org.junit.Test;

import java.util.Arrays;

public class ControladorNBackTest {

    private final ConfigNBack config = new ConfigNBack();
    private final TemporizadorFalso temporizador = new TemporizadorFalso();

    // 2-back: TRIANGULO, CIRCULO, TRIANGULO (coincidencia), CUADRADO (no coincidencia).
    private final SecuenciaNBack secuencia =
            new SecuenciaNBack(2, Arrays.asList(TRIANGULO, CIRCULO, TRIANGULO, CUADRADO));

    private static long ms(long valor) {
        return valor * 1_000_000L;
    }

    private ControladorNBack crear(boolean esPractica) {
        return new ControladorNBack(secuencia, esPractica, config, temporizador, fase -> { });
    }

    /** Empieza y deja pasar la pausa inicial: el primer estímulo está en pantalla. */
    private ControladorNBack empezar(boolean esPractica) {
        ControladorNBack c = crear(esPractica);
        c.empezar();
        assertTrue(c.getFase() instanceof FaseNBack.Preparada);
        temporizador.avanzar(config.pausaInicialMs);
        assertEquals(new FaseNBack.Estimulo(0, TRIANGULO), c.getFase());
        return c;
    }

    /** Dibuja el estímulo [indice] en [inicioMs], opcionalmente pulsa, y cierra su ventana. */
    private void ensayo(ControladorNBack c, int indice, long inicioMs, Long pulsoMs) {
        c.estimuloMostrado(indice, ms(inicioMs));
        if (pulsoMs != null) c.pulsar(ms(pulsoMs));
        temporizador.avanzar(config.ventanaMs);
    }

    @Test
    public void laFormaSeVe1000msYLuegoPantallaVacia() {
        ControladorNBack c = empezar(false);
        c.estimuloMostrado(0, ms(5_000));
        temporizador.avanzar(config.estimuloMs - 1);
        assertEquals(new FaseNBack.Estimulo(0, TRIANGULO), c.getFase());
        temporizador.avanzar(1);
        assertEquals(new FaseNBack.Vacio(0), c.getFase());
        temporizador.avanzar(config.ventanaMs - config.estimuloMs - 1);
        assertEquals(new FaseNBack.Vacio(0), c.getFase());
        temporizador.avanzar(1);
        assertEquals(new FaseNBack.Estimulo(1, CIRCULO), c.getFase());
    }

    @Test
    public void laVentanaNoEmpiezaHastaQueLaFormaSeDibuja() {
        ControladorNBack c = empezar(false);
        temporizador.avanzar(10_000);
        assertEquals(new FaseNBack.Estimulo(0, TRIANGULO), c.getFase());
        assertTrue(c.getResultados().isEmpty());
    }

    @Test
    public void acierto_pulsarEnUnaCoincidencia() {
        ControladorNBack c = empezar(false);
        ensayo(c, 0, 1_000, null);
        ensayo(c, 1, 4_000, null);
        // Pulsa ya con la pantalla vacía (1800 ms): sigue valiendo.
        ensayo(c, 2, 7_000, 8_800L);

        ResultadoEnsayoNBack r = c.getResultados().get(2);
        assertTrue(r.esCoincidencia());
        assertEquals(ResultadoEnsayoNBack.Tipo.ACIERTO, r.getTipo());
        assertEquals(Long.valueOf(1_800), r.getTiempoMs());
        assertTrue(r.esCorrecto());
    }

    @Test
    public void omision_noPulsarEnUnaCoincidencia() {
        ControladorNBack c = empezar(false);
        ensayo(c, 0, 1_000, null);
        ensayo(c, 1, 4_000, null);
        ensayo(c, 2, 7_000, null);

        ResultadoEnsayoNBack r = c.getResultados().get(2);
        assertEquals(ResultadoEnsayoNBack.Tipo.OMISION, r.getTipo());
        assertNull(r.getTiempoMs());
        assertFalse(r.esCorrecto());
    }

    @Test
    public void falsaAlarmaYRechazoCorrecto() {
        ControladorNBack c = empezar(false);
        ensayo(c, 0, 1_000, 1_400L);
        ensayo(c, 1, 4_000, null);

        assertEquals(ResultadoEnsayoNBack.Tipo.FALSA_ALARMA, c.getResultados().get(0).getTipo());
        assertEquals(Long.valueOf(400), c.getResultados().get(0).getTiempoMs());
        assertEquals(ResultadoEnsayoNBack.Tipo.RECHAZO_CORRECTO, c.getResultados().get(1).getTipo());
    }

    @Test
    public void soloCuentaLaPrimeraPulsacion() {
        ControladorNBack c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        c.pulsar(ms(1_300));
        c.pulsar(ms(1_900));
        temporizador.avanzar(config.ventanaMs);
        assertEquals(Long.valueOf(300), c.getResultados().get(0).getTiempoMs());
    }

    @Test
    public void seIgnoranPulsacionesFueraDeLaVentana() {
        ControladorNBack c = crear(false);
        c.empezar();
        c.pulsar(ms(100));                  // pausa inicial
        temporizador.avanzar(config.pausaInicialMs);
        c.pulsar(ms(900));                  // forma aún no dibujada
        c.estimuloMostrado(0, ms(1_000));
        c.pulsar(ms(999));                  // evento anterior al frame
        c.pulsar(ms(1_000 + config.ventanaMs + 1)); // después de la ventana
        temporizador.avanzar(config.ventanaMs);
        assertFalse(c.getResultados().get(0).esPulsado());
    }

    @Test
    public void elRitmoNoCambiaAlPulsar() {
        ControladorNBack c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        c.pulsar(ms(1_200));
        temporizador.avanzar(config.ventanaMs - 1);
        assertEquals(new FaseNBack.Vacio(0), c.getFase());
        assertTrue(c.getResultados().isEmpty());
    }

    @Test
    public void practica_avisoEnAciertoFalsaAlarmaYOmision_peroNoEnRechazoCorrecto() {
        ControladorNBack c = empezar(true);
        ensayo(c, 0, 1_000, 1_500L); // falsa alarma
        FaseNBack fase = c.getFase();
        assertTrue(fase instanceof FaseNBack.Feedback);
        assertEquals(ResultadoEnsayoNBack.Tipo.FALSA_ALARMA, ((FaseNBack.Feedback) fase).getResultado().getTipo());

        temporizador.avanzar(config.feedbackMs);
        assertEquals(new FaseNBack.Estimulo(1, CIRCULO), c.getFase());
        ensayo(c, 1, 5_000, null); // rechazo correcto: sin aviso
        assertEquals(new FaseNBack.Estimulo(2, TRIANGULO), c.getFase());

        ensayo(c, 2, 8_000, null); // omisión
        assertEquals(ResultadoEnsayoNBack.Tipo.OMISION,
                ((FaseNBack.Feedback) c.getFase()).getResultado().getTipo());
    }

    @Test
    public void terminaTrasTodosLosEstimulos() {
        ControladorNBack c = empezar(false);
        ensayo(c, 0, 1_000, null);
        ensayo(c, 1, 4_000, null);
        ensayo(c, 2, 7_000, 7_500L);
        ensayo(c, 3, 10_000, null);

        assertTrue(c.getFase() instanceof FaseNBack.Terminada);
        assertEquals(4, c.getResultados().size());
        assertEquals(4, c.getResultados().get(3).getNumero());
        assertTrue(c.getResultados().stream().allMatch(ResultadoEnsayoNBack::esCorrecto));
    }

    @Test
    public void detener_ignoraTodoLoQueVieneDespues() {
        ControladorNBack c = empezar(false);
        c.estimuloMostrado(0, ms(1_000));
        c.detener();
        c.pulsar(ms(1_200));
        temporizador.avanzar(60_000);
        assertTrue(c.getResultados().isEmpty());
        assertEquals(new FaseNBack.Estimulo(0, TRIANGULO), c.getFase());
    }
}
