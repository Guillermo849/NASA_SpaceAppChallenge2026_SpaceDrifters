package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import androidx.annotation.Nullable;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constantes introducidas a mano (FC, PAS, PAD, SpO2, FR).
 * <ul>
 *   <li>Límites de lo físicamente posible, solo para atrapar errores al teclear (720 lpm en vez de
 *       72). No son umbrales médicos: esos están en assets/config_umbrales.json.</li>
 *   <li>Estado de cada constante (alta, baja, dentro de su rango) a partir de los hechos que produce
 *       {@link UmbralesFisiologicos#evaluar}.</li>
 * </ul>
 */
public final class ReglasConstantes {

    public static final String FC = "FC";
    public static final String PAS = "PAS";
    public static final String PAD = "PAD";
    public static final String SPO2 = "SPO2";
    public static final String FR = "FR";

    /** Estado de una constante frente a sus umbrales. */
    public enum Estado { DENTRO, ALTO, BAJO }

    private static final Map<String, int[]> POSIBLE = new LinkedHashMap<>();
    static {
        POSIBLE.put(FC, new int[] {20, 250});
        POSIBLE.put(PAS, new int[] {50, 260});
        POSIBLE.put(PAD, new int[] {30, 160});
        POSIBLE.put(SPO2, new int[] {50, 100});
        POSIBLE.put(FR, new int[] {4, 60});
    }

    private ReglasConstantes() {
    }

    /** {mínimo, máximo} posibles de una métrica. */
    public static int[] posible(String metrica) {
        int[] r = POSIBLE.get(metrica);
        if (r == null) throw new IllegalArgumentException("Métrica desconocida: " + metrica);
        return r.clone();
    }

    /** El valor es físicamente posible (sin valor = válido: no se ha medido). */
    public static boolean valida(String metrica, @Nullable Integer valor) {
        if (valor == null) return true;
        int[] r = posible(metrica);
        return valor >= r[0] && valor <= r[1];
    }

    /** La diastólica tiene que ser menor que la sistólica (si están las dos). */
    public static boolean tensionCoherente(@Nullable Integer pas, @Nullable Integer pad) {
        return pas == null || pad == null || pad < pas;
    }

    /**
     * Estado de cada métrica medida. Alta o baja si algún umbral de esa métrica se cumple (según su
     * sentido); dentro si se ha podido evaluar y no se cumple ninguno; sin entrada (null) si no tiene
     * umbrales o faltan datos para evaluarlos (p. ej. PAD): nunca se da por "bien".
     */
    public static Map<String, Estado> estados(UmbralesFisiologicos umbrales, Hechos hechos, Collection<String> medidas) {
        Map<String, Estado> estados = new LinkedHashMap<>();
        for (String metrica : medidas) {
            Estado estado = null;
            for (Umbral u : umbrales.getUmbrales()) {
                if (!u.getMetrica().equals(metrica) || !hechos.tiene(u.getHecho())) continue;
                if (hechos.esCierto(u.getHecho())) {
                    estado = u.getSentido() == Umbral.Sentido.SUBE ? Estado.ALTO : Estado.BAJO;
                    break;
                }
                estado = Estado.DENTRO;
            }
            if (estado != null) estados.put(metrica, estado);
        }
        return estados;
    }
}
