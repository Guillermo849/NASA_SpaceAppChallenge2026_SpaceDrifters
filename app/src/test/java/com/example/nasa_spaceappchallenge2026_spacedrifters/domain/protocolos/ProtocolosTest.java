package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ProtocolosJson;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cada rama de los protocolos 1–4 de assets/protocolos.json llega al nodo y nivel esperados. */
public class ProtocolosTest {

    private static final String P1 = "trombosis-yugular";
    private static final String P2 = "hemodinamico";
    private static final String P3 = "cognitivo-sueno";
    private static final String P4 = "cardiorrespiratorio";

    private MotorProtocolos motor;

    @Before
    public void cargar() {
        motor = ProtocolosJson.desdeTexto(Assets.leer("protocolos.json")).motor();
    }

    /** Hechos a partir de pares nombre, valor. */
    private static Hechos h(Object... pares) {
        Hechos h = new Hechos();
        for (int i = 0; i < pares.length; i += 2) h.con((String) pares[i], pares[i + 1]);
        return h;
    }

    private static void esta(ResultadoProtocolo r, String nodo, int nivel, EstadoProtocolo estado) {
        assertNotNull("el protocolo no se ha activado", r);
        assertEquals(r.toString(), nodo, r.getNodo());
        assertEquals(r.toString(), nivel, r.getNivel());
        assertEquals(r.toString(), estado, r.getEstado());
    }

    // ---- Carga y validez del JSON ----

    @Test
    public void cargaLosCuatroProtocolosYLosSignosDeAlarma() {
        Set<String> ids = new HashSet<>();
        for (Protocolo p : motor.getProtocolos()) ids.add(p.getId());
        assertEquals(new HashSet<>(Arrays.asList(P1, P2, P3, P4)), ids);
        assertEquals(Arrays.asList("DOLOR_TORACICO", "SINCOPE", "DISNEA_GRAVE", "ALTERACION_LENGUAJE"), motor.getSignosAlarma());
    }

    @Test
    public void todasLasPruebasPedidasExistenEnRoom() {
        for (Protocolo p : motor.getProtocolos()) {
            for (Nodo n : p.getNodos().values()) {
                for (String prueba : n.getPruebas()) {
                    TipoPruebaClinica.valueOf(prueba); // lanza si el JSON pide una prueba que no existe
                }
            }
        }
    }

    @Test
    public void unArbolConUnaSalidaQueNoExisteNoSeCarga() {
        Nodo roto = new Nodo("a", Nodo.Tipo.CONDICION, Condicion.hecho("X"), "no-existe", null,
                Collections.<String>emptyList(), null, Collections.<Nodo.Rama>emptyList(), null, 2,
                Collections.<String>emptyList(), false);
        try {
            new Protocolo("roto", Condicion.hecho("X"), null, "a", Collections.singletonList(roto));
            fail();
        } catch (IllegalArgumentException esperado) {
            assertTrue(esperado.getMessage().contains("no-existe"));
        }
    }

    // ---- Protocolo 1: trombosis venosa yugular ----

    @Test
    public void p1_sinEdemaNoSeActiva() {
        assertNull(motor.iniciar(P1, h("CEFALEA", true)));
    }

    @Test
    public void p1_edemaSinCefalea_nivel2Vigilando() {
        ResultadoProtocolo r = motor.iniciar(P1, h("EDEMA_FACIAL_CERVICAL", true, "CEFALEA", false));
        esta(r, "p1-cefalea", 2, EstadoProtocolo.ABIERTO);
        assertEquals(Arrays.asList("VIGILAR_EDEMA", "REGISTRAR_SINTOMAS"), r.getAcciones());
    }

    @Test
    public void p1_edemaYCefalea_nivel3PideEcoDoppler() {
        ResultadoProtocolo r = motor.iniciar(P1, h("EDEMA_FACIAL_CERVICAL", true, "CEFALEA", true));
        esta(r, "p1-doppler", 3, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertEquals(Collections.singletonList("ECO_DOPPLER_YUGULAR"), r.getPruebasPendientes());
        // Historial: 1→2 por el edema, 2→3 por la cefalea.
        assertEquals(2, r.getCambios().size());
        assertEquals("EDEMA_FACIAL_CERVICAL", r.getCambios().get(0).getMotivo());
        assertEquals(1, r.getCambios().get(0).getNivelAntes());
        assertEquals("CEFALEA", r.getCambios().get(1).getMotivo());
        assertNull(r.getCambios().get(1).getConfirmadoPor());
    }

    @Test
    public void p1_dopplerConTrombo_nivel4AlLlegarElResultado() {
        // El protocolo estaba esperando la prueba; la oficial médico introduce el resultado.
        ResultadoProtocolo r = motor.avanzar(P1, "p1-doppler", 3,
                h("EDEMA_FACIAL_CERVICAL", true, "CEFALEA", true, "ECO_DOPPLER_YUGULAR", "TROMBO"));
        esta(r, "p1-trombo", 4, EstadoProtocolo.ABIERTO);
        assertEquals(Arrays.asList("PROTOCOLO_TROMBOSIS", "NOTIFICAR_MEDICO_PRIORIDAD"), r.getAcciones());
        assertEquals(1, r.getCambios().size());
        assertEquals("ECO_DOPPLER_YUGULAR=TROMBO", r.getCambios().get(0).getMotivo());
    }

    @Test
    public void p1_dopplerNormal_proponeVolverANivel2PeroNoBajaSolo() {
        ResultadoProtocolo r = motor.avanzar(P1, "p1-doppler", 3, h("ECO_DOPPLER_YUGULAR", "NORMAL"));
        esta(r, "p1-normal", 3, EstadoProtocolo.ABIERTO);
        assertEquals(Integer.valueOf(2), r.getNivelPropuesto());
        assertTrue(r.getCambios().isEmpty());
        assertEquals(Collections.singletonList("REPETIR_VIGILANCIA_48H"), r.getAcciones());
    }

    @Test
    public void p1_flujoRetrogrado_nivel3PideAnalitica() {
        ResultadoProtocolo r = motor.avanzar(P1, "p1-doppler", 3, h("ECO_DOPPLER_YUGULAR", "RETROGRADO"));
        esta(r, "p1-flujo-alterado", 3, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertEquals(Arrays.asList("DIMERO_D", "HEMOGRAMA", "COAGULACION"), r.getPruebasPendientes());
        assertTrue(r.getAcciones().contains("COMPARAR_ECO_DOPPLER_ANTERIOR"));
    }

    @Test
    public void p1_estasisConAnaliticaParcial_sigueEsperandoLoQueFalta() {
        ResultadoProtocolo r = motor.avanzar(P1, "p1-flujo-alterado", 3,
                h("ECO_DOPPLER_YUGULAR", "ESTASIS", "DIMERO_D", "ALTO"));
        esta(r, "p1-flujo-alterado", 3, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertEquals(Arrays.asList("HEMOGRAMA", "COAGULACION"), r.getPruebasPendientes());
    }

    private static Hechos analitica(boolean alterada, boolean peorQueAnterior) {
        return h("ECO_DOPPLER_YUGULAR", "RETROGRADO", "DIMERO_D", "x", "HEMOGRAMA", "x", "COAGULACION", "x",
                "ANALITICA_ALTERADA", alterada, "ECO_DOPPLER_PEOR_QUE_ANTERIOR", peorQueAnterior);
    }

    @Test
    public void p1_analiticaNormalYNoEmpeora_sigueEnNivel3() {
        esta(motor.avanzar(P1, "p1-flujo-alterado", 3, analitica(false, false)), "p1-seguimiento", 3, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p1_analiticaAlterada_nivel4() {
        esta(motor.avanzar(P1, "p1-flujo-alterado", 3, analitica(true, false)), "p1-empeora", 4, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p1_peorQueElEcoAnterior_nivel4() {
        esta(motor.avanzar(P1, "p1-flujo-alterado", 3, analitica(false, true)), "p1-empeora", 4, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p1_disneaEnCualquierMomento_emergencia() {
        ResultadoProtocolo r = motor.avanzar(P1, "p1-flujo-alterado", 3, h("DISNEA", true));
        esta(r, MotorProtocolos.NODO_EMERGENCIA, 5, EstadoProtocolo.ABIERTO);
        assertTrue(r.isEmergencia());
        assertEquals("DISNEA", r.getCambios().get(0).getMotivo());
    }

    // ---- Protocolo 2: alteraciones hemodinámicas ----

    private static Hechos ramaA(Object... mas) {
        return h("FC_ALTA", true, "PAS_BAJA", true, "PAS_ALTA", false).con(h(mas));
    }

    private static Hechos ramaB(Object... mas) {
        return h("FC_ALTA", true, "PAS_BAJA", false, "PAS_ALTA", true).con(h(mas));
    }

    @Test
    public void p2_soloFcAlta_noSeActiva() {
        assertNull(motor.iniciar(P2, h("FC_ALTA", true, "PAS_BAJA", false, "PAS_ALTA", false)));
    }

    @Test
    public void p2a_fcSubePaBaja_nivel2YPreguntaSintomas() {
        ResultadoProtocolo r = motor.iniciar(P2, ramaA());
        esta(r, "p2a-sintomas", 2, EstadoProtocolo.ABIERTO);
        assertEquals(Collections.singletonList("RESPONDER_SINTOMAS"), r.getAcciones());
        assertEquals("FC_ALTA+PAS_BAJA", r.getCambios().get(0).getMotivo());
    }

    @Test
    public void p2a_sinSintomas_nivel2RepetirMedicion() {
        esta(motor.iniciar(P2, ramaA("MAREO", false, "FATIGA", false, "PRESINCOPE", false)),
                "p2a-sin-sintomas", 2, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p2a_conMareo_nivel3BalanceHidricoYRepetir() {
        ResultadoProtocolo r = motor.iniciar(P2, ramaA("MAREO", true));
        esta(r, "p2a-repetir", 3, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertEquals(Collections.singletonList("REPETICION_CONSTANTES"), r.getPruebasPendientes());
        assertTrue(r.getAcciones().contains("REVISAR_BALANCE_HIDRICO_24H"));
    }

    @Test
    public void p2a_repeticionMejora_proponeBajarA2() {
        ResultadoProtocolo r = motor.avanzar(P2, "p2a-repetir", 3,
                ramaA("MAREO", true, "REPETICION_CONSTANTES", true, "REPETICION_ALTERADA", false));
        esta(r, "p2a-mejora", 3, EstadoProtocolo.ABIERTO);
        assertEquals(Integer.valueOf(2), r.getNivelPropuesto());
    }

    @Test
    public void p2a_repeticionIgualOPeor_pideEcocardio() {
        ResultadoProtocolo r = motor.avanzar(P2, "p2a-repetir", 3,
                ramaA("REPETICION_CONSTANTES", true, "REPETICION_ALTERADA", true));
        esta(r, "p2a-ecocardio", 3, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertEquals(Collections.singletonList("ECOCARDIO"), r.getPruebasPendientes());
    }

    @Test
    public void p2a_volumenSistolicoBajo_nivel4() {
        esta(motor.avanzar(P2, "p2a-ecocardio", 3, ramaA("ECOCARDIO", true, "VS_BAJO", true)),
                "p2a-alteracion", 4, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p2a_ecocardioDentroDeSuBase_nivel3() {
        esta(motor.avanzar(P2, "p2a-ecocardio", 3, ramaA("ECOCARDIO", true, "VS_BAJO", false, "GC_BAJO", false)),
                "p2a-seguimiento", 3, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p2b_fcYPaSuben_esperaSaberSiHaHechoEjercicio() {
        esta(motor.iniciar(P2, ramaB()), "p2b-contexto", 1, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p2b_conEjercicio_nivel1RepetirEnReposo() {
        ResultadoProtocolo r = motor.iniciar(P2, ramaB("EJERCICIO_ULTIMA_HORA", true));
        esta(r, "p2b-esperable", 1, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertEquals(Collections.singletonList("REPETIR_EN_REPOSO_15_MIN"), r.getAcciones());
    }

    @Test
    public void p2b_enReposoSeNormaliza_seCierra() {
        esta(motor.avanzar(P2, "p2b-esperable", 1, ramaB("EJERCICIO_ULTIMA_HORA", true,
                "REPETICION_CONSTANTES", true, "REPETICION_ALTERADA", false)), "p2b-normal", 1, EstadoProtocolo.CERRADO);
    }

    @Test
    public void p2b_enReposoSigueAlta_nivel3PideEcg() {
        ResultadoProtocolo r = motor.avanzar(P2, "p2b-esperable", 1, ramaB("EJERCICIO_ULTIMA_HORA", true,
                "REPETICION_CONSTANTES", true, "REPETICION_ALTERADA", true));
        esta(r, "p2b-ecg", 3, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertEquals(Collections.singletonList("ECG"), r.getPruebasPendientes());
    }

    @Test
    public void p2b_sinEjercicioNiEstres_nivel3PideEcg() {
        esta(motor.iniciar(P2, ramaB("EJERCICIO_ULTIMA_HORA", false, "ESTRES_ULTIMA_HORA", false)),
                "p2b-ecg", 3, EstadoProtocolo.ESPERANDO_PRUEBA);
    }

    @Test
    public void p2b_ecgAlterado_nivel4() {
        esta(motor.avanzar(P2, "p2b-ecg", 3, ramaB("ECG", true, "ECG_ALTERADO", true)),
                "p2b-ecg-alterado", 4, EstadoProtocolo.ABIERTO);
        esta(motor.avanzar(P2, "p2b-ecg", 3, ramaB("ECG", true, "ECG_ALTERADO", false)),
                "p2b-seguimiento", 3, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p2_dolorToracicoYSincope_emergencia() {
        ResultadoProtocolo r = motor.avanzar(P2, "p2a-repetir", 3, ramaA("DOLOR_TORACICO", true, "SINCOPE", true));
        esta(r, MotorProtocolos.NODO_EMERGENCIA, 5, EstadoProtocolo.ABIERTO);
        assertEquals("DOLOR_TORACICO+SINCOPE", r.getCambios().get(0).getMotivo());
    }

    // ---- Protocolo 3: deterioro cognitivo y sueño ----

    @Test
    public void p3_nbackBajoYPocoSueno_nivel2Descansar() {
        ResultadoProtocolo r = motor.iniciar(P3, h("NBACK_BAJO", true, "SUENO_BAJO", true, "COGNITIVO_ALERTA", false));
        esta(r, "p3-sueno-vigilar", 2, EstadoProtocolo.ABIERTO);
        assertEquals(Arrays.asList("DESCANSAR", "REPETIR_PRUEBAS_COGNITIVAS"), r.getAcciones());
    }

    @Test
    public void p3_pocoSuenoYPruebaEnAlerta_nivel3() {
        esta(motor.iniciar(P3, h("TIEMPO_REACCION_ALTO", true, "SUENO_BAJO", true, "COGNITIVO_ALERTA", true)),
                "p3-sueno-alerta", 3, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p3_suenoNormal_pasaElCuestionarioDeEstres() {
        ResultadoProtocolo r = motor.iniciar(P3, h("NBACK_BAJO", true, "SUENO_BAJO", false));
        esta(r, "p3-estres", 2, EstadoProtocolo.ABIERTO);
        assertEquals(Collections.singletonList("RESPONDER_CUESTIONARIO_ESTRES"), r.getAcciones());
    }

    @Test
    public void p3_sinCausasEvidentes_nivel3ValoracionDelMedico() {
        esta(motor.iniciar(P3, h("NBACK_BAJO", true, "SUENO_BAJO", false, "ESTRES_ALTO", false)),
                "p3-sin-causa", 3, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p3_estresAlto_nivel2() {
        esta(motor.iniciar(P3, h("NBACK_BAJO", true, "SUENO_BAJO", false, "ESTRES_ALTO", true)),
                "p3-estres-alto", 2, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p3_deterioroBruscoOAlteracionDelLenguaje_emergencia() {
        assertTrue(motor.avanzar(P3, "p3-estres", 2, h("DETERIORO_COGNITIVO_BRUSCO", true)).isEmergencia());
        assertTrue(motor.avanzar(P3, "p3-estres", 2, h("ALTERACION_LENGUAJE", true)).isEmergencia());
    }

    // ---- Protocolo 4: alteraciones cardiorrespiratorias ----

    @Test
    public void p4_spo2BajaSinSintomas_nivel2RepetirPorSiEsElSensor() {
        ResultadoProtocolo r = motor.iniciar(P4, h("SPO2_BAJA", true, "TAQUIPNEA", false, "DISNEA", false));
        esta(r, "p4-repetir", 2, EstadoProtocolo.ESPERANDO_PRUEBA);
        assertTrue(r.getAcciones().contains("REVISAR_SENSOR"));
    }

    @Test
    public void p4_repeticionNormal_proponeBajarA1() {
        ResultadoProtocolo r = motor.avanzar(P4, "p4-repetir", 2, h("SPO2_BAJA", true, "TAQUIPNEA", false,
                "DISNEA", false, "REPETICION_CONSTANTES", true, "REPETICION_ALTERADA", false));
        esta(r, "p4-sensor", 2, EstadoProtocolo.ABIERTO);
        assertEquals(Integer.valueOf(1), r.getNivelPropuesto());
    }

    @Test
    public void p4_sigueBajaSinSintomas_nivel3() {
        esta(motor.avanzar(P4, "p4-repetir", 2, h("SPO2_BAJA", true, "TAQUIPNEA", false, "DISNEA", false,
                "REPETICION_CONSTANTES", true, "REPETICION_ALTERADA", true)), "p4-persistente", 3, EstadoProtocolo.ABIERTO);
    }

    @Test
    public void p4_spo2BajaConTaquipnea_nivel4Alerta() {
        ResultadoProtocolo r = motor.iniciar(P4, h("SPO2_BAJA", true, "TAQUIPNEA", true));
        esta(r, "p4-alerta", 4, EstadoProtocolo.ABIERTO);
        assertTrue(r.getAcciones().containsAll(Arrays.asList("MEDIR_PA", "HACER_ECG", "MEDIR_FC")));
        assertFalse(r.isEmergencia());
    }

    @Test
    public void p4_ademasMareoIntenso_emergencia() {
        ResultadoProtocolo r = motor.avanzar(P4, "p4-alerta", 4, h("SPO2_BAJA", true, "TAQUIPNEA", true, "MAREO_INTENSO", true));
        esta(r, "p4-emergencia", 5, EstadoProtocolo.ABIERTO);
        assertTrue(r.isEmergencia());
    }

    // ---- Reglas comunes ----

    @Test
    public void signoDeAlarma_nivel5DesdeCualquierNodoDeCualquierProtocolo() {
        for (Protocolo p : motor.getProtocolos()) {
            for (String nodo : p.getNodos().keySet()) {
                ResultadoProtocolo r = motor.avanzar(p.getId(), nodo, 2, h("SINCOPE", true));
                assertTrue(p.getId() + "/" + nodo, r.isEmergencia());
                assertEquals(p.getId() + "/" + nodo, 5, r.getNivel());
                assertEquals(MotorProtocolos.NODO_EMERGENCIA, r.getNodo());
                assertEquals("SINCOPE", r.getCambios().get(0).getMotivo());
            }
        }
    }

    @Test
    public void signoDeAlarma_tambienAlAbrirElProtocolo() {
        ResultadoProtocolo r = motor.iniciar(P1, h("EDEMA_FACIAL_CERVICAL", true, "DOLOR_TORACICO", true));
        esta(r, MotorProtocolos.NODO_EMERGENCIA, 5, EstadoProtocolo.ABIERTO);
        assertEquals(1, r.getCambios().get(0).getNivelAntes());
    }

    @Test
    public void signosDeAlarmaSinProtocolo() {
        assertEquals(Arrays.asList("SINCOPE", "ALTERACION_LENGUAJE"),
                motor.signosAlarmaPresentes(h("ALTERACION_LENGUAJE", true, "SINCOPE", true, "CEFALEA", true)));
        assertTrue(motor.signosAlarmaPresentes(h("DOLOR_TORACICO", false)).isEmpty());
    }

    @Test
    public void laEmergenciaNoSeQuitaSola() {
        ResultadoProtocolo r = motor.avanzar(P1, MotorProtocolos.NODO_EMERGENCIA, 5, new Hechos());
        esta(r, MotorProtocolos.NODO_EMERGENCIA, 5, EstadoProtocolo.ABIERTO);
        assertTrue(r.getCambios().isEmpty());
    }

    @Test
    public void elNivelNoBajaSinConfirmacion() {
        // Cualquier nodo de nivel más bajo solo deja una propuesta; el nivel se mantiene.
        for (Protocolo p : motor.getProtocolos()) {
            for (Nodo n : p.getNodos().values()) {
                if (n.getNivel() == null || n.getNivel() >= 4) continue;
                ResultadoProtocolo r = motor.avanzar(p.getId(), n.getId(), 4, new Hechos());
                assertTrue(p.getId() + "/" + n.getId(), r.getNivel() >= 4);
            }
        }
        // Bajarlo a mano necesita quién lo confirma, y queda en el historial.
        Cambio c = MotorProtocolos.confirmarNivel("p1-normal", 3, 2, "ECO_DOPPLER_YUGULAR=NORMAL", "OFICIAL_MEDICO:3");
        assertEquals(3, c.getNivelAntes());
        assertEquals(2, c.getNivelDespues());
        assertEquals("OFICIAL_MEDICO:3", c.getConfirmadoPor());
        try {
            MotorProtocolos.confirmarNivel("p1-normal", 3, 2, "x", null);
            fail();
        } catch (IllegalArgumentException esperado) {
            // sin confirmación no se baja
        }
    }

    @Test
    public void nivelesDeLaEscala() {
        List<NivelGravedad> niveles = Arrays.asList(NivelGravedad.values());
        assertEquals(5, niveles.size());
        assertEquals(NivelGravedad.VALORACION, NivelGravedad.de(3));
        assertEquals(5, NivelGravedad.EMERGENCIA.getNumero());
    }
}
