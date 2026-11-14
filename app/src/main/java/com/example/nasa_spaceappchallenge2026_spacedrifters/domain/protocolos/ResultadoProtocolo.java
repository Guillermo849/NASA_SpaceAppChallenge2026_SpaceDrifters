package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Dónde se ha quedado un protocolo tras recorrerlo con los hechos de ahora. */
public final class ResultadoProtocolo {

    private final String protocoloId;
    private final String nodo;
    private final int nivel;
    private final EstadoProtocolo estado;
    private final Integer nivelPropuesto;
    private final List<String> acciones;
    private final List<String> pruebasPendientes;
    private final List<Cambio> cambios;
    private final List<String> recorrido;
    private final boolean emergencia;

    ResultadoProtocolo(String protocoloId, String nodo, int nivel, EstadoProtocolo estado, Integer nivelPropuesto,
                       List<String> acciones, List<String> pruebasPendientes, List<Cambio> cambios,
                       List<String> recorrido, boolean emergencia) {
        this.protocoloId = protocoloId;
        this.nodo = nodo;
        this.nivel = nivel;
        this.estado = estado;
        this.nivelPropuesto = nivelPropuesto;
        this.acciones = Collections.unmodifiableList(new ArrayList<>(acciones));
        this.pruebasPendientes = Collections.unmodifiableList(new ArrayList<>(pruebasPendientes));
        this.cambios = Collections.unmodifiableList(new ArrayList<>(cambios));
        this.recorrido = Collections.unmodifiableList(new ArrayList<>(recorrido));
        this.emergencia = emergencia;
    }

    public String getProtocoloId() { return protocoloId; }
    /** Nodo en el que se queda (MotorProtocolos.NODO_EMERGENCIA si ha saltado la emergencia). */
    public String getNodo() { return nodo; }
    public int getNivel() { return nivel; }
    public EstadoProtocolo getEstado() { return estado; }
    /** Nivel más bajo que propone el árbol, pendiente de confirmar; null si no hay. */
    public Integer getNivelPropuesto() { return nivelPropuesto; }
    /** Acciones vigentes (las del último nodo que las tenía). */
    public List<String> getAcciones() { return acciones; }
    /** Pruebas que faltan para seguir (solo en ESPERANDO_PRUEBA). */
    public List<String> getPruebasPendientes() { return pruebasPendientes; }
    /** Subidas de nivel de este recorrido, en orden. */
    public List<Cambio> getCambios() { return cambios; }
    /** Nodos por los que ha pasado en este recorrido. */
    public List<String> getRecorrido() { return recorrido; }
    /** Nivel 5: pantalla de emergencia. */
    public boolean isEmergencia() { return emergencia; }

    @Override
    public String toString() {
        return protocoloId + "@" + nodo + " nivel " + nivel + " " + estado
                + (nivelPropuesto != null ? " (propone " + nivelPropuesto + ")" : "")
                + " " + recorrido;
    }
}
