package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas;

import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo.ALERTA;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo.NORMAL;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo.VIGILAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.Noche;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.Sesion;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.MotorReglas.ReglaCumplida;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Las 4 reglas cognitivas del plan (las mismas que assets/reglas.json). */
public class MotorReglasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);
    private static final Set<EstadoCognitivo> MAL = EnumSet.of(VIGILAR, ALERTA);

    private static final Regla STROOP_TENDENCIA = new Regla("stroop-tendencia", Regla.Tipo.TENDENCIA,
            Collections.singletonList("STROOP"), MAL, 3, 0, Collections.emptySet(),
            Regla.Prioridad.PUEDE_ESPERAR, Collections.emptyList(), null);
    private static final Regla NBACK_TENDENCIA = new Regla("nback-tendencia", Regla.Tipo.TENDENCIA,
            Collections.singletonList("NBACK"), MAL, 3, 0, Collections.emptySet(),
            Regla.Prioridad.HOY, Arrays.asList("EVITAR_TAREAS_CRITICAS_SOLO", "REPETIR_PRUEBA_MANANA"), null);
    private static final Regla FATIGA = new Regla("fatiga-cognitiva-combinada", Regla.Tipo.PRUEBA_Y_SUENO,
            Collections.singletonList("NBACK"), MAL, 0, 3, Collections.emptySet(),
            Regla.Prioridad.HOY, Arrays.asList("REDUCIR_CARGA_TRABAJO", "DORMIR_8_HORAS", "REPETIR_PRUEBAS_MANANA"), null);
    private static final Regla CONCENTRACION = new Regla("concentracion-sintomas", Regla.Tipo.PRUEBA_Y_SINTOMAS,
            Arrays.asList("STROOP", "NBACK"), EnumSet.of(ALERTA), 0, 0,
            new HashSet<>(Arrays.asList("DIFICULTAD_CONCENTRACION", "DOLOR_CABEZA")),
            Regla.Prioridad.AHORA, Collections.emptyList(), "OFICIAL_MEDICO");
    private static final List<Regla> TODAS = Arrays.asList(STROOP_TENDENCIA, NBACK_TENDENCIA, FATIGA, CONCENTRACION);

    private long siguienteId = 1;

    /** Sesiones de una prueba, una por día terminando hoy; los estados de la más antigua a la más reciente. */
    private List<Sesion> sesiones(String prueba, EstadoCognitivo... estados) {
        List<Sesion> lista = new ArrayList<>();
        for (int i = estados.length - 1; i >= 0; i--) {
            long dias = estados.length - 1 - i;
            lista.add(new Sesion(siguienteId++, prueba, HOY.minusDays(dias), estados[i], "m", 1.0));
        }
        return lista; // más reciente primero
    }

    /** Horas de sueño de los últimos días, de la noche más antigua a la de hoy. */
    private static List<Noche> noches(Double... horas) {
        List<Noche> lista = new ArrayList<>();
        for (int i = 0; i < horas.length; i++) {
            lista.add(new Noche(HOY.minusDays(horas.length - 1 - i), horas[i]));
        }
        return lista;
    }

    private static ContextoReglas contexto(List<Sesion> sesiones, List<Noche> noches, String... sintomasHoy) {
        return new ContextoReglas(HOY, sesiones, noches, 7.5, new HashSet<>(Arrays.asList(sintomasHoy)));
    }

    private static List<String> ids(List<ReglaCumplida> cumplidas) {
        List<String> ids = new ArrayList<>();
        for (ReglaCumplida r : cumplidas) ids.add(r.getRegla().getId());
        return ids;
    }

    // ---- stroop-tendencia ----

    @Test
    public void stroopTendencia_tresSeguidasEnVigilarOAlerta() {
        List<ReglaCumplida> r = MotorReglas.evaluar(TODAS,
                contexto(sesiones("STROOP", NORMAL, VIGILAR, ALERTA, VIGILAR), noches()));
        assertEquals(Collections.singletonList("stroop-tendencia"), ids(r));
        assertEquals(3, r.get(0).getSesiones().size());
        assertEquals(HOY, r.get(0).getSesiones().get(0).getFecha()); // la más reciente primero
    }

    @Test
    public void stroopTendencia_unaNormalEnMedioLaCorta() {
        assertTrue(MotorReglas.evaluar(TODAS,
                contexto(sesiones("STROOP", VIGILAR, NORMAL, VIGILAR, ALERTA), noches())).isEmpty());
    }

    @Test
    public void stroopTendencia_conDosNoBasta() {
        assertTrue(MotorReglas.evaluar(TODAS, contexto(sesiones("STROOP", ALERTA, ALERTA), noches())).isEmpty());
    }

    // ---- nback-tendencia ----

    @Test
    public void nbackTendencia_prioridadHoyConPasos() {
        List<ReglaCumplida> r = MotorReglas.evaluar(Collections.singletonList(NBACK_TENDENCIA),
                contexto(sesiones("NBACK", VIGILAR, VIGILAR, ALERTA), noches()));
        assertEquals(1, r.size());
        assertEquals(Regla.Prioridad.HOY, r.get(0).getRegla().getPrioridad());
        assertEquals(2, r.get(0).getRegla().getPasos().size());
    }

    @Test
    public void nbackTendencia_noMiraLasSesionesDelStroop() {
        List<Sesion> mezcla = new ArrayList<>(sesiones("STROOP", ALERTA, ALERTA, ALERTA));
        mezcla.addAll(sesiones("NBACK", NORMAL, NORMAL, NORMAL));
        assertEquals(Collections.singletonList("stroop-tendencia"), ids(MotorReglas.evaluar(TODAS, contexto(mezcla, noches()))));
    }

    // ---- fatiga-cognitiva-combinada ----

    @Test
    public void fatiga_nbackMalYTresNochesPocoSueno() {
        List<ReglaCumplida> r = MotorReglas.evaluar(Collections.singletonList(FATIGA),
                contexto(sesiones("NBACK", NORMAL, VIGILAR), noches(5.2, 4.8, 5.0)));
        assertEquals(1, r.size());
        assertEquals(3, r.get(0).getNoches().size());
        assertEquals(HOY, r.get(0).getNoches().get(0).getFecha());
    }

    @Test
    public void fatiga_unaNocheNormalNoSalta() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(FATIGA),
                contexto(sesiones("NBACK", VIGILAR), noches(5.2, 7.6, 5.0))).isEmpty());
    }

    @Test
    public void fatiga_unaNocheSinDatosNoSalta() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(FATIGA),
                contexto(sesiones("NBACK", VIGILAR), noches(5.2, null, 5.0))).isEmpty());
    }

    @Test
    public void fatiga_conElNbackNormalNoSalta() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(FATIGA),
                contexto(sesiones("NBACK", ALERTA, NORMAL), noches(5.0, 5.0, 5.0))).isEmpty());
    }

    @Test
    public void fatiga_sinSuenoHabitualNoSalta() {
        ContextoReglas c = new ContextoReglas(HOY, sesiones("NBACK", ALERTA), noches(5.0, 5.0, 5.0), null, Collections.emptySet());
        assertTrue(MotorReglas.evaluar(Collections.singletonList(FATIGA), c).isEmpty());
    }

    // ---- concentracion-sintomas ----

    @Test
    public void concentracion_alertaHoyYDolorDeCabeza_asignadaALaOficialMedico() {
        List<ReglaCumplida> r = MotorReglas.evaluar(Collections.singletonList(CONCENTRACION),
                contexto(sesiones("STROOP", NORMAL, ALERTA), noches(), "DOLOR_CABEZA", "FATIGA"));
        assertEquals(1, r.size());
        assertEquals(Regla.Prioridad.AHORA, r.get(0).getRegla().getPrioridad());
        assertEquals("OFICIAL_MEDICO", r.get(0).getRegla().getAsignarA());
        assertEquals(Collections.singleton("DOLOR_CABEZA"), r.get(0).getSintomas());
    }

    @Test
    public void concentracion_vigilarNoBasta() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(CONCENTRACION),
                contexto(sesiones("NBACK", VIGILAR), noches(), "DIFICULTAD_CONCENTRACION")).isEmpty());
    }

    @Test
    public void concentracion_laAlertaTieneQueSerDeHoy() {
        // ALERTA ayer, NORMAL hoy.
        assertTrue(MotorReglas.evaluar(Collections.singletonList(CONCENTRACION),
                contexto(sesiones("STROOP", ALERTA, NORMAL), noches(), "DOLOR_CABEZA")).isEmpty());
    }

    @Test
    public void concentracion_otroSintomaNoCuenta() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(CONCENTRACION),
                contexto(sesiones("STROOP", ALERTA), noches(), "NAUSEAS")).isEmpty());
    }

    @Test
    public void sinDatos_ningunaRegla() {
        assertTrue(MotorReglas.evaluar(TODAS, contexto(Collections.emptyList(), Collections.emptyList())).isEmpty());
    }
}
