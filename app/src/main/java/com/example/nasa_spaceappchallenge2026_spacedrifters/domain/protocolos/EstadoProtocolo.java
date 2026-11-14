package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

/** En qué punto está un protocolo abierto para un astronauta. */
public enum EstadoProtocolo {
    /** Avanzando o esperando un dato (síntomas, cuestionario...). */
    ABIERTO,
    /** Ha pedido una o varias pruebas y espera a que la oficial médico introduzca el resultado. */
    ESPERANDO_PRUEBA,
    /** Terminado; ya no se reevalúa. */
    CERRADO
}
