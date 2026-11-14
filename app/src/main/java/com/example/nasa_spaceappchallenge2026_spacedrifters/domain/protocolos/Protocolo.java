package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Un protocolo de assets/protocolos.json: cuándo se activa, desde qué nodo empieza, qué lo lleva a
 * emergencia en cualquier punto (además de los signos de alarma comunes) y su árbol de nodos.
 * Al construirse comprueba que el árbol es válido (todas las salidas existen).
 */
public final class Protocolo {

    private final String id;
    private final Condicion activacion;
    private final Condicion emergencia;
    private final String inicio;
    private final Map<String, Nodo> nodos;

    public Protocolo(String id, Condicion activacion, Condicion emergencia, String inicio, List<Nodo> nodos) {
        this.id = Objects.requireNonNull(id);
        this.activacion = Objects.requireNonNull(activacion, id + ": falta la activación");
        this.emergencia = emergencia;
        this.inicio = Objects.requireNonNull(inicio, id + ": falta el nodo de inicio");
        Map<String, Nodo> mapa = new LinkedHashMap<>();
        for (Nodo n : nodos) {
            if (MotorProtocolos.NODO_EMERGENCIA.equals(n.getId())) {
                throw new IllegalArgumentException(id + ": el id " + n.getId() + " está reservado");
            }
            if (mapa.put(n.getId(), n) != null) throw new IllegalArgumentException(id + ": nodo repetido " + n.getId());
        }
        this.nodos = Collections.unmodifiableMap(mapa);
        if (!mapa.containsKey(inicio)) throw new IllegalArgumentException(id + ": no existe el nodo de inicio " + inicio);
        for (Nodo n : nodos) {
            for (String s : n.salidas()) {
                if (!mapa.containsKey(s)) throw new IllegalArgumentException(id + "/" + n.getId() + ": no existe el nodo " + s);
            }
        }
    }

    public String getId() { return id; }
    public Condicion getActivacion() { return activacion; }
    /** null = solo los signos de alarma comunes. */
    public Condicion getEmergencia() { return emergencia; }
    public String getInicio() { return inicio; }
    public Map<String, Nodo> getNodos() { return nodos; }

    public Nodo nodo(String id) {
        Nodo n = nodos.get(id);
        if (n == null) throw new IllegalArgumentException(this.id + ": no existe el nodo " + id);
        return n;
    }
}
