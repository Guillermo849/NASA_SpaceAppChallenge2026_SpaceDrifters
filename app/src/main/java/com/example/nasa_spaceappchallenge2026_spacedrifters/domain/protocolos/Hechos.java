package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lo que se sabe ahora de un astronauta, por nombre: síntomas ("CEFALEA" = true), constantes ya
 * comparadas con su base ("FC_ALTA" = true/false, ver UmbralesFisiologicos), resultados de pruebas
 * ("ECO_DOPPLER_YUGULAR" = "RETROGRADO") o números. Un hecho que no está es "no se sabe", que no es
 * lo mismo que false: el protocolo se queda esperando ese dato.
 */
public final class Hechos {

    private final Map<String, Object> valores = new LinkedHashMap<>();

    /** Añade o sustituye un hecho; un valor null lo quita (vuelve a "no se sabe"). */
    public Hechos con(String nombre, Object valor) {
        if (valor == null) valores.remove(nombre);
        else valores.put(nombre, valor);
        return this;
    }

    /** Añade todos los hechos de otro conjunto (los suyos ganan). */
    public Hechos con(Hechos otros) {
        valores.putAll(otros.valores);
        return this;
    }

    /** Copia independiente. */
    public Hechos copia() {
        return new Hechos().con(this);
    }

    public Object get(String nombre) { return valores.get(nombre); }

    public boolean tiene(String nombre) { return valores.containsKey(nombre); }

    /** true solo si el hecho existe y es Boolean.TRUE. */
    public boolean esCierto(String nombre) { return Boolean.TRUE.equals(valores.get(nombre)); }

    public Map<String, Object> comoMapa() { return Collections.unmodifiableMap(valores); }

    @Override
    public String toString() { return valores.toString(); }
}
