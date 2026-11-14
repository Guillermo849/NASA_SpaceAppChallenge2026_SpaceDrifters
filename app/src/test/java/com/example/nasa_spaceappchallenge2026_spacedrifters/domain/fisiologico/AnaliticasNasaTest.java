package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.AnaliticasNasa.Muestra;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.AnaliticasNasa.Parametro;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Assets;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos;
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.TextosFisiologicoKt;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Analíticas reales de NASA (Inspiration4): hemogramas LSDS-7 y bioquímicas LSDS-8. */
public class AnaliticasNasaTest {

    private List<Muestra> todas;

    @Before
    public void leer() {
        todas = AnaliticasNasa.leer(Assets.leer("LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv"));
    }

    private Muestra muestra(String tripulante, String momento) {
        for (Muestra m : AnaliticasNasa.de(todas, tripulante)) if (m.getMomento().equals(momento)) return m;
        throw new AssertionError("No existe " + tripulante + " " + momento);
    }

    private static List<String> codigos(List<Parametro> ps) {
        List<String> c = new ArrayList<>();
        for (Parametro p : ps) c.add(p.getCodigo());
        return c;
    }

    @Test
    public void leeLas28MuestrasConSus20Parametros() {
        assertEquals(28, todas.size());
        // 20 parámetros entre los que trae y los que vienen vacíos ("Sin dato").
        for (Muestra m : todas) assertEquals(m.getNombre(), 20, m.getParametros().size() + m.getSinDato().size());
        assertEquals(Collections.singletonList("mchc"), muestra("C002", "L-3").getSinDato());
        assertTrue(muestra("C001", "R+1").getSinDato().isEmpty());
        Parametro hb = null;
        for (Parametro p : muestra("C002", "R+82").getParametros()) if (p.getCodigo().equals("hemoglobin")) hb = p;
        assertNotNull(hb);
        assertEquals(11.5, hb.getValor(), 1e-9);
        assertEquals(11.7, hb.getMinimo(), 1e-9);
        assertEquals(15.5, hb.getMaximo(), 1e-9);
        assertTrue(hb.isBajo());
    }

    @Test
    public void cadaTripulanteConSieteMuestrasEnOrden() {
        List<String> momentos = new ArrayList<>();
        for (Muestra m : AnaliticasNasa.de(todas, "C002")) momentos.add(m.getMomento());
        assertEquals(Arrays.asList("L-92", "L-44", "L-3", "R+1", "R+45", "R+82", "R+194"), momentos);
        assertTrue(AnaliticasNasa.de(todas, "C002").get(0).isAntesDelVuelo());
        assertFalse(AnaliticasNasa.de(todas, "C002").get(3).isAntesDelVuelo());
        assertTrue(AnaliticasNasa.de(todas, "C999").isEmpty());
    }

    @Test
    public void fueraDeRango() {
        assertEquals(Arrays.asList("hematocrit", "hemoglobin"), codigos(muestra("C002", "R+82").fueraDeRango()));
        assertTrue(muestra("C002", "R+82").isAlterado());
        assertFalse(muestra("C001", "R+1").isAlterado());
        assertEquals(Collections.singletonList("absolute_monocytes"), codigos(muestra("C004", "R+1").fueraDeRango()));
    }

    @Test
    public void comoResultadoDeHemograma_alteradoHaceAlteradaLaAnalitica() {
        Map<String, Object> r = InterpretePruebas.resultadoHemograma(muestra("C002", "R+82"));
        assertEquals("ALTERADO", r.get("valor"));
        assertEquals("C002_whole-blood_R+82_cbc", r.get(InterpretePruebas.MUESTRA_NASA));
        assertEquals("hematocrit,hemoglobin", r.get(InterpretePruebas.FUERA_DE_RANGO));

        InterpretePruebas interprete = new InterpretePruebas(ConfigUmbrales.desdeTexto(Assets.leer("config_umbrales.json")));
        Hechos h = interprete.hechos(
                Collections.singletonList(new InterpretePruebas.Resultado("HEMOGRAMA", r, 1)), Collections.<InterpretePruebas.Resultado>emptyList());
        assertEquals("ALTERADO", h.get("HEMOGRAMA"));
        assertTrue(h.esCierto("ANALITICA_ALTERADA"));

        Map<String, Object> normal = InterpretePruebas.resultadoHemograma(muestra("C001", "R+1"));
        assertEquals("NORMAL", normal.get("valor"));
        assertEquals("", normal.get(InterpretePruebas.FUERA_DE_RANGO));
    }

    @Test
    public void cadaParametroTieneTextoEnLaApp() {
        for (Parametro p : todas.get(0).getParametros()) {
            assertNotNull(p.getCodigo(), TextosFisiologicoKt.nombreParametroAnalitica(p.getCodigo()));
        }
    }

    @Test
    public void archivoVacioOMalFormado_sinMuestras() {
        assertTrue(AnaliticasNasa.leer("").isEmpty());
        assertTrue(AnaliticasNasa.leer("Sample Name\nfila_rara,1,2").isEmpty());
    }

    // ---- Bioquímica: LSDS-8 (assets/LSDS-8_Comprehensive_Metabolic_Panel_CMP_TRANSFORMED.csv) ----

    private static final String CMP = "LSDS-8_Comprehensive_Metabolic_Panel_CMP_TRANSFORMED.csv";
    private static final List<String> SIN_EGFR = Arrays.asList("egfr_african_american", "egfr_non_african_american");

    private static Muestra bioquimica(List<Muestra> todas, String tripulante, String momento) {
        for (Muestra m : AnaliticasNasa.de(todas, tripulante)) if (m.getMomento().equals(momento)) return m;
        throw new AssertionError("No existe " + tripulante + " " + momento);
    }

    private static InterpretePruebas interprete() {
        return new InterpretePruebas(ConfigUmbrales.desdeTexto(Assets.leer("config_umbrales.json")));
    }

    @Test
    public void bioquimica_28MuestrasDeSueroCon17ParametrosSinEgfr() {
        List<Muestra> cmp = AnaliticasNasa.leer(Assets.leer(CMP), SIN_EGFR);
        assertEquals(28, cmp.size());
        for (Muestra m : cmp) assertEquals(m.getNombre(), 17, m.getParametros().size() + m.getSinDato().size());
        assertEquals("C001_serum_L-3", bioquimica(cmp, "C001", "L-3").getNombre());
        // Los cocientes no traen unidad; ALT y AST tienen la columna "_max_" en vez de "_range_max_".
        Muestra c4 = bioquimica(cmp, "C004", "R+1");
        assertEquals("", c4.parametro("bun_to_creatinine_ratio").getUnidad());
        assertEquals(46, c4.parametro("alt").getMaximo(), 1e-9);
        assertTrue(c4.parametro("ast").isAlto());
    }

    @Test
    public void bioquimica_naEsSinDatoYNaEnElRangoEsRangoAbierto() {
        List<Muestra> cmp = AnaliticasNasa.leer(Assets.leer(CMP));
        Muestra m = bioquimica(cmp, "C001", "L-44");
        // "NA" en el valor: sin dato, nunca normal.
        assertTrue(m.getSinDato().contains("bun_to_creatinine_ratio"));
        // "N/A" en el máximo del eGFR: rango abierto por arriba.
        Parametro egfr = m.parametro("egfr_non_african_american");
        assertFalse(egfr.hasMaximo());
        assertTrue(egfr.hasMinimo());
        assertFalse(egfr.isFueraDeRango());
    }

    @Test
    public void bioquimica_resultadosConDatosReales() {
        List<Muestra> cmp = AnaliticasNasa.leer(Assets.leer(CMP), SIN_EGFR);
        InterpretePruebas interprete = interprete();
        // C004 (Ilan en la demo) el día después de aterrizar: urea y BUN/creatinina altos.
        Map<String, Object> ilan = interprete.resultadoBioquimica(bioquimica(cmp, "C004", "R+1"));
        assertEquals("DESHIDRATACION", ilan.get("valor"));
        assertEquals("C004_serum_R+1", ilan.get(InterpretePruebas.MUESTRA_NASA));
        assertEquals("ast,bun_to_creatinine_ratio,urea_nitrogen_bun", ilan.get(InterpretePruebas.FUERA_DE_RANGO));
        // C001 tres días antes del lanzamiento: solo la creatinina alta → otra alteración.
        assertEquals("ALTERADA", interprete.resultadoBioquimica(bioquimica(cmp, "C001", "L-3")).get("valor"));
        // C001 el día después de aterrizar: todo en rango.
        assertEquals("NORMAL", interprete.resultadoBioquimica(bioquimica(cmp, "C001", "R+1")).get("valor"));
    }

    @Test
    public void bioquimica_hechosParaLosProtocolos() {
        List<Muestra> cmp = AnaliticasNasa.leer(Assets.leer(CMP), SIN_EGFR);
        InterpretePruebas interprete = interprete();
        Hechos h = interprete.hechos(Collections.singletonList(new InterpretePruebas.Resultado("BIOQUIMICA",
                interprete.resultadoBioquimica(bioquimica(cmp, "C004", "R+1")), 1)),
                Collections.<InterpretePruebas.Resultado>emptyList());
        assertEquals("DESHIDRATACION", h.get("BIOQUIMICA"));
        assertTrue(h.esCierto("BIOQUIMICA_ALTERADA"));
        assertTrue(h.esCierto("DESHIDRATACION_ANALITICA"));

        Hechos normal = interprete.hechos(Collections.singletonList(new InterpretePruebas.Resultado("BIOQUIMICA",
                InterpretePruebas.resultadoCerrado("BIOQUIMICA", "NORMAL"), 1)),
                Collections.<InterpretePruebas.Resultado>emptyList());
        assertFalse(normal.esCierto("BIOQUIMICA_ALTERADA"));
        assertFalse(normal.esCierto("DESHIDRATACION_ANALITICA"));
    }

    @Test
    public void bioquimica_cadaParametroTieneTextoEnLaApp() {
        for (Parametro p : AnaliticasNasa.leer(Assets.leer(CMP), SIN_EGFR).get(0).getParametros()) {
            assertNotNull(p.getCodigo(), TextosFisiologicoKt.nombreParametroAnalitica(p.getCodigo()));
        }
    }
}
