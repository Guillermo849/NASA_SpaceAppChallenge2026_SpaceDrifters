package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

/**
 * Estado de una sesión frente a la línea base del astronauta. El nombre es lo que se guarda en
 * ResumenSesion.estado. Nunca se muestra solo con color: siempre con su texto.
 */
public enum EstadoCognitivo {
    /** Dentro de media ± 1 desviación (o mejor de lo habitual). */
    NORMAL,
    /** Entre 1 y 2 desviaciones en la dirección mala. */
    VIGILAR,
    /** Más de 2 desviaciones en la dirección mala. */
    ALERTA,
    /** El astronauta aún no tiene línea base para esta prueba. Nunca cuenta como NORMAL. */
    SIN_LINEA_BASE,
    /** La sesión no da para calcular la métrica (p. ej., ningún acierto). Nunca cuenta como NORMAL. */
    SIN_DATOS
}
