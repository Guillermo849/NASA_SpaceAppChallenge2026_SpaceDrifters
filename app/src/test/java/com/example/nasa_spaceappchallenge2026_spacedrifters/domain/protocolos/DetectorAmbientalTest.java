package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ProtocolosJson;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.DetectorAmbiental.Alteracion;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.DetectorAmbiental.Grupo;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Protocolo 5: salta con 2 astronautas en 24 h y no con 1 (configuración de assets/protocolos.json). */
public class DetectorAmbientalTest {

    private static final long HORA = 3_600_000L;
    private static final long ANE = 3, ILAN = 4, LEO = 2;

    private DetectorAmbiental detector;

    @Before
    public void cargar() {
        detector = ProtocolosJson.desdeTexto(Assets.leer("protocolos.json")).detectorAmbiental();
    }

    @Test
    public void configuracionLeidaDelJson() {
        DetectorAmbiental.Config c = detector.getConfig();
        assertEquals(24 * HORA, c.getVentanaMs());
        assertEquals(2, c.getMinAstronautas());
        assertEquals(4, c.getNivel());
        assertTrue(c.getAcciones().contains("REVISAR_CO2"));
    }

    @Test
    public void dosAstronautasConCefaleaElMismoDia_posibleCausaAmbiental() {
        List<Grupo> grupos = detector.detectar(Arrays.asList(
                new Alteracion(ANE, "CEFALEA", 10 * HORA),
                new Alteracion(ILAN, "CEFALEA", 12 * HORA)));
        assertEquals(1, grupos.size());
        assertEquals("CEFALEA", grupos.get(0).getTipo());
        assertEquals(new HashSet<>(Arrays.asList(ANE, ILAN)), grupos.get(0).getAstronautas());
        assertEquals(10 * HORA, grupos.get(0).getDesde());
        assertEquals(12 * HORA, grupos.get(0).getHasta());
    }

    @Test
    public void unSoloAstronauta_noSalta() {
        assertTrue(detector.detectar(Arrays.asList(
                new Alteracion(ANE, "CEFALEA", 10 * HORA),
                new Alteracion(ANE, "CEFALEA", 11 * HORA))).isEmpty());
    }

    @Test
    public void fueraDeLaVentanaDe24h_noSalta() {
        assertTrue(detector.detectar(Arrays.asList(
                new Alteracion(ANE, "CEFALEA", 0),
                new Alteracion(ILAN, "CEFALEA", 30 * HORA))).isEmpty());
    }

    @Test
    public void tiposDistintos_noSeJuntan() {
        assertTrue(detector.detectar(Arrays.asList(
                new Alteracion(ANE, "CEFALEA", 0),
                new Alteracion(ILAN, "SPO2_BAJA", HORA))).isEmpty());
    }

    @Test
    public void tipoQueNoEstaEnLaConfiguracion_noCuenta() {
        assertTrue(detector.detectar(Arrays.asList(
                new Alteracion(ANE, "MAREO", 0),
                new Alteracion(ILAN, "MAREO", HORA))).isEmpty());
    }

    @Test
    public void tresAstronautas_laVentanaConMas() {
        List<Grupo> grupos = detector.detectar(Arrays.asList(
                new Alteracion(LEO, "SPO2_BAJA", 0),
                new Alteracion(ANE, "SPO2_BAJA", 20 * HORA),
                new Alteracion(ILAN, "SPO2_BAJA", 26 * HORA)));
        assertEquals(1, grupos.size());
        // 0 h y 20 h, o 20 h y 26 h: dos astronautas como mucho en 24 h.
        assertEquals(2, grupos.get(0).getAstronautas().size());
    }
}
