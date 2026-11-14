package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Todos los umbrales de assets/config_umbrales.json. Convierte las constantes de un astronauta
 * (y su línea base) en hechos para los protocolos: "FC_ALTA", "PAS_BAJA", "SPO2_BAJA"...
 */
public final class UmbralesFisiologicos {

    private final List<Umbral> umbrales;
    private final Map<String, Double> referencias;

    public UmbralesFisiologicos(List<Umbral> umbrales) {
        this(umbrales, Collections.<String, Double>emptyMap());
    }

    /**
     * @param referencias valor de referencia por métrica para quien aún no tiene línea base propia
     *                    (p. ej. VS y GC antes del primer ecocardio)
     */
    public UmbralesFisiologicos(List<Umbral> umbrales, Map<String, Double> referencias) {
        this.umbrales = Collections.unmodifiableList(new ArrayList<>(umbrales));
        this.referencias = Collections.unmodifiableMap(new HashMap<>(referencias));
    }

    public List<Umbral> getUmbrales() { return umbrales; }

    /** Valor de referencia de una métrica (config_umbrales.json, "referencias"), o null. */
    public Double referencia(String metrica) { return referencias.get(metrica); }

    /** El umbral que produce ese hecho, o null. */
    public Umbral umbral(String hecho) {
        for (Umbral u : umbrales) if (u.getHecho().equals(hecho)) return u;
        return null;
    }

    /**
     * @param valores último valor de cada métrica ("FC", "PAS", "SPO2", "FR", "VS", "GC")
     * @param bases   línea base de cada métrica para esta persona (puede faltar)
     * @return un hecho por umbral evaluable (true/false); los que no se pueden saber no aparecen
     */
    public Hechos evaluar(Map<String, Double> valores, Map<String, Double> bases) {
        Hechos hechos = new Hechos();
        for (Umbral u : umbrales) {
            Boolean alterado = u.alterado(valores.get(u.getMetrica()), bases.get(u.getMetrica()));
            if (alterado != null) hechos.con(u.getHecho(), alterado);
        }
        return hechos;
    }
}
