package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

/**
 * Escala de gravedad 1–5 de los protocolos fisiológicos. El nivel solo sube automáticamente; para
 * bajarlo hace falta la confirmación de la oficial médico o del médico.
 */
public enum NivelGravedad {
    /** Dentro de su rango o con causa explicada: se registra y se sigue la tendencia. */
    INFORMATIVO(1),
    /** Se aleja de su línea base sin síntomas: repetir la medición o vigilar más a menudo. */
    VIGILAR(2),
    /** Desviación persistente o con síntomas leves: pruebas a bordo y aviso al médico (puede esperar). */
    VALORACION(3),
    /** Hallazgo relevante o síntomas que lo confirman: protocolo a bordo ya y aviso prioritario. */
    ALERTA(4),
    /** Signos de alarma: pantalla de emergencia, no se espera a la Tierra. */
    EMERGENCIA(5);

    public static final int MINIMO = 1;
    public static final int MAXIMO = 5;

    private final int numero;

    NivelGravedad(int numero) { this.numero = numero; }

    public int getNumero() { return numero; }

    public static NivelGravedad de(int numero) {
        for (NivelGravedad n : values()) if (n.numero == numero) return n;
        throw new IllegalArgumentException("Nivel fuera de 1–5: " + numero);
    }
}
