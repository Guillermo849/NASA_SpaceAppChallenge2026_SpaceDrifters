package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

/** Un cambio de nivel para el historial (EventoProtocolo): dónde, de qué a qué, por qué y quién. */
public final class Cambio {

    private final String nodo;
    private final int nivelAntes;
    private final int nivelDespues;
    private final String motivo;
    private final String confirmadoPor;

    public Cambio(String nodo, int nivelAntes, int nivelDespues, String motivo, String confirmadoPor) {
        this.nodo = nodo;
        this.nivelAntes = nivelAntes;
        this.nivelDespues = nivelDespues;
        this.motivo = motivo;
        this.confirmadoPor = confirmadoPor;
    }

    public String getNodo() { return nodo; }
    public int getNivelAntes() { return nivelAntes; }
    public int getNivelDespues() { return nivelDespues; }
    /** Dato que lo provoca, p. ej. "CEFALEA" o "ECO_DOPPLER_YUGULAR=TROMBO". */
    public String getMotivo() { return motivo; }
    /** null = automático. */
    public String getConfirmadoPor() { return confirmadoPor; }

    @Override
    public String toString() {
        return nodo + ": " + nivelAntes + "->" + nivelDespues + " (" + motivo + ")";
    }
}
