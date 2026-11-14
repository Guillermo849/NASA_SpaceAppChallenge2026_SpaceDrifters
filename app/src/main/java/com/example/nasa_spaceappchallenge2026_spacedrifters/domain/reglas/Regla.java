package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Una regla de alerta, tal y como se define en assets/reglas.json.
 * Las pruebas y los síntomas van por nombre ("STROOP", "DOLOR_CABEZA"...) para no depender de
 * las clases de datos.
 */
public final class Regla {

    /** Qué combina la regla. */
    public enum Tipo {
        /** N sesiones válidas seguidas de una prueba en ciertos estados. */
        TENDENCIA,
        /** La última sesión de una prueba en ciertos estados + dormir por debajo de su base N noches seguidas. */
        PRUEBA_Y_SUENO,
        /** Una sesión de hoy en ciertos estados + alguno de ciertos síntomas en el registro de hoy. */
        PRUEBA_Y_SINTOMAS
    }

    /** Urgencia para el médico. */
    public enum Prioridad { AHORA, HOY, PUEDE_ESPERAR }

    private final String id;
    private final Tipo tipo;
    private final List<String> pruebas;
    private final Set<EstadoCognitivo> estados;
    private final int sesionesSeguidas;
    private final int nochesBajoBase;
    private final Set<String> sintomas;
    private final Prioridad prioridad;
    private final List<String> pasos;
    private final String asignarA;

    public Regla(String id, Tipo tipo, List<String> pruebas, Set<EstadoCognitivo> estados,
                 int sesionesSeguidas, int nochesBajoBase, Set<String> sintomas,
                 Prioridad prioridad, List<String> pasos, String asignarA) {
        this.id = Objects.requireNonNull(id);
        this.tipo = Objects.requireNonNull(tipo);
        this.pruebas = Collections.unmodifiableList(pruebas);
        this.estados = Collections.unmodifiableSet(estados.isEmpty() ? EnumSet.noneOf(EstadoCognitivo.class) : EnumSet.copyOf(estados));
        this.sesionesSeguidas = sesionesSeguidas;
        this.nochesBajoBase = nochesBajoBase;
        this.sintomas = Collections.unmodifiableSet(new LinkedHashSet<>(sintomas));
        this.prioridad = Objects.requireNonNull(prioridad);
        this.pasos = Collections.unmodifiableList(pasos);
        this.asignarA = asignarA;
        if (pruebas.isEmpty()) throw new IllegalArgumentException(id + ": falta la prueba");
        if (tipo == Tipo.TENDENCIA && sesionesSeguidas < 1) throw new IllegalArgumentException(id + ": sesionesSeguidas");
        if (tipo == Tipo.PRUEBA_Y_SUENO && nochesBajoBase < 1) throw new IllegalArgumentException(id + ": nochesBajoBase");
        if (tipo == Tipo.PRUEBA_Y_SINTOMAS && sintomas.isEmpty()) throw new IllegalArgumentException(id + ": faltan síntomas");
    }

    public String getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public List<String> getPruebas() { return pruebas; }
    public Set<EstadoCognitivo> getEstados() { return estados; }
    public int getSesionesSeguidas() { return sesionesSeguidas; }
    public int getNochesBajoBase() { return nochesBajoBase; }
    public Set<String> getSintomas() { return sintomas; }
    public Prioridad getPrioridad() { return prioridad; }
    /** Códigos de pasos recomendados al astronauta; vacío = solo "se ha enviado a tu médico". */
    public List<String> getPasos() { return pasos; }
    /** Cargo al que se asigna (p. ej. "OFICIAL_MEDICO"), o null. */
    public String getAsignarA() { return asignarA; }
}
