package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas;

import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo.ALERTA;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo.NORMAL;
import static com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo.VIGILAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ReglasJson;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Assets;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.DiaAgua;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.DiaBienestar;
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
        assertTrue(MotorReglas.evaluar(Collections.singletonList(HIDRATACION), conAgua()).isEmpty());
    }

    // ---- hidratacion-baja ----

    private static final Regla HIDRATACION = new Regla("hidratacion-baja", Regla.Tipo.HIDRATACION_BAJA,
            Collections.emptyList(), Collections.emptySet(), 0, 0, Collections.emptySet(),
            Regla.Prioridad.HOY, Arrays.asList("BEBER_MAS_LIQUIDO", "VIGILAR_MAREO"), null, 2, 1.5);

    /** Litros anotados los últimos días, del más antiguo al de hoy. */
    private static ContextoReglas conAgua(Double... litros) {
        List<DiaAgua> lista = new ArrayList<>();
        for (int i = 0; i < litros.length; i++) {
            lista.add(new DiaAgua(HOY.minusDays(litros.length - 1 - i), litros[i]));
        }
        return new ContextoReglas(HOY, Collections.emptyList(), Collections.emptyList(), 7.5,
                Collections.emptySet(), lista);
    }

    @Test
    public void hidratacion_dosDiasSeguidosPorDebajo() {
        List<ReglaCumplida> r = MotorReglas.evaluar(Collections.singletonList(HIDRATACION), conAgua(2.0, 1.25, 1.0));
        assertEquals(Collections.singletonList("hidratacion-baja"), ids(r));
        // Los dos días implicados, del más reciente al más antiguo; sin sesiones.
        assertEquals(2, r.get(0).getAgua().size());
        assertEquals(HOY, r.get(0).getAgua().get(0).getFecha());
        assertEquals(1.0, r.get(0).getAgua().get(0).getLitros(), 1e-9);
        assertTrue(r.get(0).getSesiones().isEmpty());
    }

    @Test
    public void hidratacion_unSoloDiaNoBasta() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(HIDRATACION), conAgua(2.0, 1.0)).isEmpty());
        // Ayer bajo, hoy bien.
        assertTrue(MotorReglas.evaluar(Collections.singletonList(HIDRATACION), conAgua(1.0, 2.0)).isEmpty());
    }

    @Test
    public void hidratacion_justoEnElUmbralNoCuenta() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(HIDRATACION), conAgua(1.5, 1.0)).isEmpty());
    }

    @Test
    public void hidratacion_unDiaSinAnotarNoLaDispara() {
        assertTrue(MotorReglas.evaluar(Collections.singletonList(HIDRATACION), conAgua(1.0, null)).isEmpty());
        // Sin registro de hoy.
        ContextoReglas sinHoy = new ContextoReglas(HOY, Collections.emptyList(), Collections.emptyList(), 7.5,
                Collections.emptySet(), Arrays.asList(new DiaAgua(HOY.minusDays(1), 1.0), new DiaAgua(HOY.minusDays(2), 1.0)));
        assertTrue(MotorReglas.evaluar(Collections.singletonList(HIDRATACION), sinHoy).isEmpty());
    }

    @Test
    public void hidratacion_lasReglasCognitivasNoLaVen() {
        // Mucho líquido bajo, sin pruebas: solo salta la de hidratación.
        List<Regla> todas = new ArrayList<>(TODAS);
        todas.add(HIDRATACION);
        assertEquals(Collections.singletonList("hidratacion-baja"), ids(MotorReglas.evaluar(todas, conAgua(1.0, 1.0, 1.0))));
    }

    @Test
    public void reglasJson_laDeHidratacionSeLeeDelAsset() {
        List<Regla> reglas = ReglasJson.Companion.desdeTexto(Assets.leer("reglas.json"));
        Regla h = null;
        for (Regla r : reglas) if (r.getId().equals("hidratacion-baja")) h = r;
        assertEquals(Regla.Tipo.HIDRATACION_BAJA, h.getTipo());
        assertEquals(2, h.getDiasSeguidos());
        assertEquals(1.5, h.getAguaMinLitros(), 1e-9);
        assertEquals(6, reglas.size());
    }

    // ---- bienestar-bajo ----

    private static final Regla BIENESTAR = new Regla("bienestar-bajo", Regla.Tipo.BIENESTAR_BAJO,
            Collections.emptyList(), Collections.emptySet(), 0, 0, Collections.emptySet(),
            Regla.Prioridad.HOY, Arrays.asList("HABLAR_APOYO_PSICOLOGICO", "PROBAR_RESPIRACION"), null,
            2, 0.0, new UmbralesBienestar(2, 4, 4));

    /** Check-ins {ánimo, estrés, aislamiento} de los últimos días, del más antiguo al de hoy. */
    private static ContextoReglas conBienestar(Integer[]... dias) {
        List<DiaBienestar> lista = new ArrayList<>();
        for (int i = 0; i < dias.length; i++) {
            lista.add(new DiaBienestar(HOY.minusDays(dias.length - 1 - i), dias[i][0], dias[i][1], dias[i][2]));
        }
        return new ContextoReglas(HOY, Collections.emptyList(), Collections.emptyList(), 7.5,
                Collections.emptySet(), Collections.emptyList(), lista);
    }

    private static Integer[] dia(Integer animo, Integer estres, Integer aislamiento) {
        return new Integer[]{animo, estres, aislamiento};
    }

    private static List<String> evaluarBienestar(ContextoReglas c) {
        return ids(MotorReglas.evaluar(Collections.singletonList(BIENESTAR), c));
    }

    @Test
    public void bienestar_dosDiasSeguidosConAlgunIndicadorFuera() {
        // Ayer aislamiento alto, hoy ánimo bajo: cuentan los dos aunque sea por motivos distintos.
        List<ReglaCumplida> r = MotorReglas.evaluar(Collections.singletonList(BIENESTAR),
                conBienestar(dia(4, 2, 2), dia(3, 2, 4), dia(2, 2, 2)));
        assertEquals(Collections.singletonList("bienestar-bajo"), ids(r));
        assertEquals(2, r.get(0).getBienestar().size());
        assertEquals(HOY, r.get(0).getBienestar().get(0).getFecha());
        assertTrue(r.get(0).getSesiones().isEmpty());
    }

    @Test
    public void bienestar_unSoloDiaNoBasta() {
        assertTrue(evaluarBienestar(conBienestar(dia(4, 2, 2), dia(4, 5, 2))).isEmpty());
        assertTrue(evaluarBienestar(conBienestar(dia(1, 5, 5), dia(4, 2, 2))).isEmpty());
    }

    @Test
    public void bienestar_enElLimiteNoCuenta() {
        // Ánimo 3, estrés 3, aislamiento 3: dentro de lo esperable.
        assertTrue(evaluarBienestar(conBienestar(dia(3, 3, 3), dia(3, 3, 3))).isEmpty());
        // Estrés 4 dos días: sí.
        assertEquals(Collections.singletonList("bienestar-bajo"), evaluarBienestar(conBienestar(dia(3, 4, 1), dia(3, 4, 1))));
    }

    @Test
    public void bienestar_unDiaSinCheckinNoLaDispara() {
        ContextoReglas sinHoy = new ContextoReglas(HOY, Collections.emptyList(), Collections.emptyList(), 7.5,
                Collections.emptySet(), Collections.emptyList(),
                Arrays.asList(new DiaBienestar(HOY.minusDays(1), 1, 5, 5), new DiaBienestar(HOY.minusDays(2), 1, 5, 5)));
        assertTrue(evaluarBienestar(sinHoy).isEmpty());
        // Hoy sin respuestas (todo null) tampoco.
        assertTrue(evaluarBienestar(conBienestar(dia(1, 5, 5), dia(null, null, null))).isEmpty());
    }

    @Test
    public void reglasJson_laDeBienestarSeLeeDelAsset() {
        Regla b = null;
        for (Regla r : ReglasJson.Companion.desdeTexto(Assets.leer("reglas.json"))) {
            if (r.getId().equals("bienestar-bajo")) b = r;
        }
        assertEquals(Regla.Tipo.BIENESTAR_BAJO, b.getTipo());
        assertEquals(2, b.getDiasSeguidos());
        assertEquals(Integer.valueOf(2), b.getBienestar().getAnimoMax());
        assertEquals(Integer.valueOf(4), b.getBienestar().getEstresMin());
        assertEquals(Integer.valueOf(4), b.getBienestar().getAislamientoMin());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bienestar_sinUmbralesEsInvalida() {
        new Regla("x", Regla.Tipo.BIENESTAR_BAJO, Collections.emptyList(), Collections.emptySet(), 0, 0,
                Collections.emptySet(), Regla.Prioridad.HOY, Collections.emptyList(), null, 2, 0.0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hidratacion_sinUmbralEsInvalida() {
        new Regla("x", Regla.Tipo.HIDRATACION_BAJA, Collections.emptyList(), Collections.emptySet(), 0, 0,
                Collections.emptySet(), Regla.Prioridad.HOY, Collections.emptyList(), null, 2, 0.0);
    }
}
