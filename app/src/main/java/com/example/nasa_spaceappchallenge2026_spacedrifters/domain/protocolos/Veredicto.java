package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

/** Resultado de evaluar una condición: falta un dato = DESCONOCIDO (el protocolo espera). */
public enum Veredicto {
    SI, NO, DESCONOCIDO;

    public static Veredicto de(boolean b) { return b ? SI : NO; }
}
