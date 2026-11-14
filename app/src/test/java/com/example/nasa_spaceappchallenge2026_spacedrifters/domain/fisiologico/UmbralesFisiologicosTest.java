package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Assets;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/** Umbrales de assets/config_umbrales.json: desviación frente a su base O umbral absoluto. */
public class UmbralesFisiologicosTest {

    private UmbralesFisiologicos umbrales;

    @Before
    public void cargar() {
        umbrales = ConfigUmbrales.desdeTexto(Assets.leer("config_umbrales.json"));
    }

    private static Map<String, Double> m(Object... pares) {
        Map<String, Double> mapa = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) mapa.put((String) pares[i], ((Number) pares[i + 1]).doubleValue());
        return mapa;
    }

    @Test
    public void valoresDeLaTablaDelPlan() {
        Umbral fc = umbrales.umbral("FC_ALTA");
        assertEquals(20.0, fc.getDesviacionPorcentaje(), 0);
        assertEquals(100.0, fc.getAbsoluto(), 0);
        assertEquals(3.0, umbrales.umbral("SPO2_BAJA").getDesviacionPuntos(), 0);
        assertEquals(94.0, umbrales.umbral("SPO2_BAJA").getAbsoluto(), 0);
        assertEquals(90.0, umbrales.umbral("PAS_BAJA").getAbsoluto(), 0);
        assertEquals(140.0, umbrales.umbral("PAS_ALTA").getAbsoluto(), 0);
        assertEquals(20.0, umbrales.umbral("TAQUIPNEA").getAbsoluto(), 0);
        assertNull(umbrales.umbral("VS_BAJO").getAbsoluto());
    }

    @Test
    public void fc_porEncimaDeSuBaseMas20Porciento() {
        // Base 70 → límite 84.
        assertTrue(umbrales.evaluar(m("FC", 85), m("FC", 70)).esCierto("FC_ALTA"));
        Hechos normal = umbrales.evaluar(m("FC", 80), m("FC", 70));
        assertTrue(normal.tiene("FC_ALTA"));
        assertFalse(normal.esCierto("FC_ALTA"));
    }

    @Test
    public void fc_sinBaseSoloCuentaElUmbralAbsoluto() {
        assertTrue(umbrales.evaluar(m("FC", 105), m()).esCierto("FC_ALTA"));
        assertFalse(umbrales.evaluar(m("FC", 95), m()).esCierto("FC_ALTA"));
    }

    @Test
    public void spo2_tresPuntosPorDebajoDeSuBaseOMenosDe94() {
        assertTrue(umbrales.evaluar(m("SPO2", 95), m("SPO2", 99)).esCierto("SPO2_BAJA"));
        assertFalse(umbrales.evaluar(m("SPO2", 96), m("SPO2", 98)).esCierto("SPO2_BAJA"));
        assertTrue(umbrales.evaluar(m("SPO2", 93), m("SPO2", 95)).esCierto("SPO2_BAJA"));
    }

    @Test
    public void presionSistolica_bajaYAlta() {
        Hechos h = umbrales.evaluar(m("PAS", 95), m("PAS", 120)); // límite bajo 102, alto 138
        assertTrue(h.esCierto("PAS_BAJA"));
        assertFalse(h.esCierto("PAS_ALTA"));
        assertTrue(umbrales.evaluar(m("PAS", 145), m()).esCierto("PAS_ALTA"));
    }

    @Test
    public void frecuenciaRespiratoria_taquipnea() {
        assertTrue(umbrales.evaluar(m("FR", 22), m()).esCierto("TAQUIPNEA"));
        assertTrue(umbrales.evaluar(m("FR", 18), m("FR", 13)).esCierto("TAQUIPNEA")); // > 16,9
    }

    @Test
    public void volumenSistolico_soloFrenteASuBase() {
        assertTrue(umbrales.evaluar(m("VS", 60), m("VS", 75)).esCierto("VS_BAJO")); // límite 63,75
        // Sin base y sin umbral absoluto no se puede saber: el hecho no aparece.
        assertFalse(umbrales.evaluar(m("VS", 60), m()).tiene("VS_BAJO"));
    }

    @Test
    public void sinValor_elHechoNoAparece() {
        Hechos h = umbrales.evaluar(m(), m("FC", 70));
        assertFalse(h.tiene("FC_ALTA"));
    }

    @Test
    public void franjaAlrededorDeLaBase() {
        // FC solo tiene umbral hacia arriba (+20 %): la franja es simétrica, 56–84 con base 70.
        double[] fc = umbrales.franja("FC", 70);
        assertEquals(56, fc[0], 1e-9);
        assertEquals(84, fc[1], 1e-9);
        // PAS tiene umbral por los dos lados (±15 %).
        double[] pas = umbrales.franja("PAS", 120);
        assertEquals(102, pas[0], 1e-9);
        assertEquals(138, pas[1], 1e-9);
        // SpO2: 3 puntos por debajo.
        double[] spo2 = umbrales.franja("SPO2", 98);
        assertEquals(95, spo2[0], 1e-9);
        assertNull(umbrales.franja("TEMPERATURA", 36.5));
    }

    @Test
    public void lineaBase_mediaDeLaPrimeraVentana() {
        long dia = 86_400_000L;
        long[] t = {0, dia, 2 * dia, 8 * dia};
        double[] v = {60, 62, 64, 95};
        // Solo cuentan los 7 primeros días: (60 + 62 + 64) / 3.
        assertEquals(62, UmbralesFisiologicos.lineaBase(t, v, 7 * dia), 1e-9);
        assertNull(UmbralesFisiologicos.lineaBase(new long[0], new double[0], dia));
    }

    @Test
    public void losUmbralesSeLeenDeLaConfiguracion() {
        // Con un 50 % en vez de un 20 %, 85 lpm sobre una base de 70 ya no es FC alta.
        String json = Assets.leer("config_umbrales.json")
                .replace("\"desviacionPorcentaje\": 20, \"absoluto\": 100", "\"desviacionPorcentaje\": 50, \"absoluto\": 100");
        UmbralesFisiologicos otros = ConfigUmbrales.desdeTexto(json);
        assertEquals(50.0, otros.umbral("FC_ALTA").getDesviacionPorcentaje(), 0);
        assertFalse(otros.evaluar(m("FC", 85), m("FC", 70)).esCierto("FC_ALTA"));
    }
}
