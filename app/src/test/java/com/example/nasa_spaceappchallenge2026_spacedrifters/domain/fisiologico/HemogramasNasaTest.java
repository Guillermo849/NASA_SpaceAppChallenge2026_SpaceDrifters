package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.HemogramasNasa.Muestra;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.HemogramasNasa.Parametro;
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

/** Hemogramas reales de NASA LSDS-7 (assets/LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv). */
public class HemogramasNasaTest {

    private List<Muestra> todas;

    @Before
    public void leer() {
        todas = HemogramasNasa.leer(Assets.leer("LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv"));
    }

    private Muestra muestra(String tripulante, String momento) {
        for (Muestra m : HemogramasNasa.de(todas, tripulante)) if (m.getMomento().equals(momento)) return m;
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
        for (Muestra m : HemogramasNasa.de(todas, "C002")) momentos.add(m.getMomento());
        assertEquals(Arrays.asList("L-92", "L-44", "L-3", "R+1", "R+45", "R+82", "R+194"), momentos);
        assertTrue(HemogramasNasa.de(todas, "C002").get(0).isAntesDelVuelo());
        assertFalse(HemogramasNasa.de(todas, "C002").get(3).isAntesDelVuelo());
        assertTrue(HemogramasNasa.de(todas, "C999").isEmpty());
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
            assertNotNull(p.getCodigo(), TextosFisiologicoKt.nombreParametroHemograma(p.getCodigo()));
        }
    }

    @Test
    public void archivoVacioOMalFormado_sinMuestras() {
        assertTrue(HemogramasNasa.leer("").isEmpty());
        assertTrue(HemogramasNasa.leer("Sample Name\nfila_rara,1,2").isEmpty());
    }
}
