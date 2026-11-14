package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.ReglasConstantes.Estado;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Assets;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Constantes introducidas a mano, con los umbrales reales de assets/config_umbrales.json. */
public class ReglasConstantesTest {

    private final UmbralesFisiologicos umbrales = ConfigUmbrales.desdeTexto(Assets.leer("config_umbrales.json"));

    @Test
    public void valoresImposibles_seRechazan() {
        assertTrue(ReglasConstantes.valida(ReglasConstantes.FC, 72));
        assertFalse("720 en vez de 72", ReglasConstantes.valida(ReglasConstantes.FC, 720));
        assertFalse(ReglasConstantes.valida(ReglasConstantes.SPO2, 101));
        assertTrue("sin medir = válido", ReglasConstantes.valida(ReglasConstantes.FR, null));
    }

    @Test
    public void tension_laDiastolicaPorDebajoDeLaSistolica() {
        assertTrue(ReglasConstantes.tensionCoherente(120, 80));
        assertFalse(ReglasConstantes.tensionCoherente(80, 120));
        assertTrue(ReglasConstantes.tensionCoherente(120, null));
    }

    private Map<String, Estado> estados(Map<String, Double> valores, Map<String, Double> bases) {
        Hechos h = umbrales.evaluar(valores, bases);
        return ReglasConstantes.estados(umbrales, h, valores.keySet());
    }

    @Test
    public void frenteASuBase_altaBajaODentro() {
        Map<String, Double> valores = new HashMap<>();
        valores.put("FC", 96.0);   // base 62: +55 %
        valores.put("PAS", 94.0);  // base 118: −20 %
        valores.put("SPO2", 98.0);
        valores.put("FR", 14.0);
        Map<String, Double> bases = new HashMap<>();
        bases.put("FC", 62.0);
        bases.put("PAS", 118.0);
        bases.put("SPO2", 98.0);
        bases.put("FR", 14.0);
        Map<String, Estado> e = estados(valores, bases);
        assertEquals(Estado.ALTO, e.get("FC"));
        assertEquals(Estado.BAJO, e.get("PAS"));
        assertEquals(Estado.DENTRO, e.get("SPO2"));
        assertEquals(Estado.DENTRO, e.get("FR"));
    }

    @Test
    public void sinBase_cuentanLosUmbralesAbsolutos() {
        Map<String, Double> valores = new HashMap<>();
        valores.put("SPO2", 91.0); // < 94 absoluto
        valores.put("FC", 80.0);   // < 100 absoluto
        Map<String, Estado> e = estados(valores, new HashMap<String, Double>());
        assertEquals(Estado.BAJO, e.get("SPO2"));
        assertEquals(Estado.DENTRO, e.get("FC"));
    }

    @Test
    public void sinUmbral_noSeDaPorBueno() {
        Map<String, Estado> e = ReglasConstantes.estados(umbrales, new Hechos(), Arrays.asList("PAD", "FC"));
        assertNull(e.get("PAD"));
        assertNull("sin hechos evaluados, sin estado", e.get("FC"));
    }
}
