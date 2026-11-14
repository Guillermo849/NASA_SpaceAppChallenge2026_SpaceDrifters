package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ProtocolosJson;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas.Resultado;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Assets;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.MotorProtocolos;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Nodo;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Protocolo;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.ResultadoProtocolo;
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.TextosFisiologicoKt;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Resultados de las pruebas a bordo → hechos, y que cada código de protocolos.json tiene texto. */
public class InterpretePruebasTest {

    private InterpretePruebas interprete;
    private MotorProtocolos motor;

    @Before
    public void cargar() {
        interprete = new InterpretePruebas(ConfigUmbrales.desdeTexto(Assets.leer("config_umbrales.json")));
        motor = ProtocolosJson.desdeTexto(Assets.leer("protocolos.json")).motor();
    }

    private static Resultado cerrado(String tipo, String opcion, long cuando) {
        return new Resultado(tipo, InterpretePruebas.resultadoCerrado(tipo, opcion), cuando);
    }

    private Hechos de(Resultado... delProtocolo) {
        List<Resultado> lista = Arrays.asList(delProtocolo);
        return interprete.hechos(lista, lista);
    }

    @Test
    public void opcionesCerradasDeCadaPrueba() {
        assertEquals(Arrays.asList("NORMAL", "ESTASIS", "RETROGRADO", "TROMBO"),
                InterpretePruebas.opciones(InterpretePruebas.ECO_DOPPLER_YUGULAR));
        assertTrue(InterpretePruebas.opciones(InterpretePruebas.ECOCARDIO).isEmpty());
        // Todas las pruebas que existen en Room tienen formulario (opciones o el ecocardio).
        for (TipoPruebaClinica t : TipoPruebaClinica.values()) {
            assertTrue(t.name(), t == TipoPruebaClinica.ECOCARDIO || !InterpretePruebas.opciones(t.name()).isEmpty());
        }
        try {
            InterpretePruebas.resultadoCerrado(InterpretePruebas.ECG, "RARO");
            fail();
        } catch (IllegalArgumentException esperado) {
            // solo opciones de la lista
        }
    }

    @Test
    public void ecoDoppler_flujoYComparacionConElAnterior() {
        Hechos h = de(cerrado(InterpretePruebas.ECO_DOPPLER_YUGULAR, "RETROGRADO", 10));
        assertEquals("RETROGRADO", h.get("ECO_DOPPLER_YUGULAR"));
        assertFalse("sin eco anterior no empeora", h.esCierto("ECO_DOPPLER_PEOR_QUE_ANTERIOR"));

        Resultado anterior = cerrado(InterpretePruebas.ECO_DOPPLER_YUGULAR, "ESTASIS", 5);
        Resultado actual = cerrado(InterpretePruebas.ECO_DOPPLER_YUGULAR, "RETROGRADO", 10);
        Hechos peor = interprete.hechos(Collections.singletonList(actual), Arrays.asList(anterior, actual));
        assertTrue(peor.esCierto("ECO_DOPPLER_PEOR_QUE_ANTERIOR"));
        assertEquals("TROMBO", de(cerrado(InterpretePruebas.ECO_DOPPLER_YUGULAR, "TROMBO", 1)).get("ECO_DOPPLER_YUGULAR"));
    }

    @Test
    public void analitica_alteradaSiAlgunaNoEsNormal() {
        // Una sola normal: aún no se sabe.
        assertFalse(de(cerrado("DIMERO_D", "NORMAL", 1)).tiene("ANALITICA_ALTERADA"));
        // Una alterada basta.
        assertTrue(de(cerrado("DIMERO_D", "ELEVADO", 1)).esCierto("ANALITICA_ALTERADA"));
        // Las tres normales: no alterada.
        Hechos normal = de(cerrado("DIMERO_D", "NORMAL", 1), cerrado("HEMOGRAMA", "NORMAL", 1), cerrado("COAGULACION", "NORMAL", 1));
        assertTrue(normal.tiene("ANALITICA_ALTERADA"));
        assertFalse(normal.esCierto("ANALITICA_ALTERADA"));
    }

    @Test
    public void ecgYRepeticion() {
        assertTrue(de(cerrado("ECG", "ALTERADO", 1)).esCierto("ECG_ALTERADO"));
        assertFalse(de(cerrado("ECG", "NORMAL", 1)).esCierto("ECG_ALTERADO"));
        assertTrue(de(cerrado("REPETICION_CONSTANTES", "IGUAL_O_PEOR", 1)).esCierto("REPETICION_ALTERADA"));
        assertFalse(de(cerrado("REPETICION_CONSTANTES", "MEJOR", 1)).esCierto("REPETICION_ALTERADA"));
    }

    @Test
    public void ecocardio_frenteALaReferenciaOASuPrimerEco() {
        // Sin eco anterior: referencia de config_umbrales.json (VS 70 → límite 59,5; GC 5 → 4,25).
        Hechos h = de(new Resultado("ECOCARDIO", InterpretePruebas.resultadoEcocardio(55, 4.5), 10));
        assertTrue(h.esCierto("ECOCARDIO"));
        assertTrue(h.esCierto("VS_BAJO"));
        assertFalse(h.esCierto("GC_BAJO"));

        // Con un eco anterior propio (VS 60), 55 ya no es bajo (límite 51).
        Resultado base = new Resultado("ECOCARDIO", InterpretePruebas.resultadoEcocardio(60, 4.5), 1);
        Resultado actual = new Resultado("ECOCARDIO", InterpretePruebas.resultadoEcocardio(55, 4.5), 10);
        assertFalse(interprete.hechos(Collections.singletonList(actual), Arrays.asList(base, actual)).esCierto("VS_BAJO"));
    }

    @Test
    public void elResultadoHaceAvanzarElProtocolo() {
        // Leo: esperando el eco doppler; sale retrógrado → pide la analítica.
        ResultadoProtocolo r = motor.avanzar("trombosis-yugular", "p1-doppler", 3,
                de(cerrado(InterpretePruebas.ECO_DOPPLER_YUGULAR, "RETROGRADO", 10)));
        assertEquals("p1-flujo-alterado", r.getNodo());
        assertEquals(EstadoProtocolo.ESPERANDO_PRUEBA, r.getEstado());
        // Con el dímero D elevado (aunque falten las otras dos), nivel 4.
        r = motor.avanzar("trombosis-yugular", "p1-flujo-alterado", 3, de(
                cerrado(InterpretePruebas.ECO_DOPPLER_YUGULAR, "RETROGRADO", 10),
                cerrado("DIMERO_D", "ELEVADO", 11), cerrado("HEMOGRAMA", "NORMAL", 11), cerrado("COAGULACION", "NORMAL", 11)));
        assertEquals("p1-empeora", r.getNodo());
        assertEquals(4, r.getNivel());
    }

    @Test
    public void cadaCodigoDeProtocolosJsonTieneTextoEnLaApp() {
        for (Protocolo p : motor.getProtocolos()) {
            assertNotNull(p.getId(), TextosFisiologicoKt.nombreProtocolo(p.getId()));
            for (Nodo n : p.getNodos().values()) {
                assertNotNull("nodo " + n.getId(), TextosFisiologicoKt.nombreNodo(n.getId()));
                for (String a : n.getAcciones()) assertNotNull("acción " + a, TextosFisiologicoKt.nombreAccion(a));
            }
        }
        for (String a : ProtocolosJson.desdeTexto(Assets.leer("protocolos.json")).detectorAmbiental().getConfig().getAcciones()) {
            assertNotNull("acción " + a, TextosFisiologicoKt.nombreAccion(a));
        }
        for (String s : motor.getSignosAlarma()) {
            assertNotNull(s, com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.emergencia.EmergenciaUiKt.nombreSignoAlarma(s));
        }
        for (String tipo : Arrays.asList("ECO_DOPPLER_YUGULAR", "ECG", "DIMERO_D", "HEMOGRAMA", "COAGULACION", "REPETICION_CONSTANTES")) {
            for (String o : InterpretePruebas.opciones(tipo)) {
                assertNotNull(tipo + "/" + o, TextosFisiologicoKt.nombreOpcionResultado(o));
            }
        }
    }
}
