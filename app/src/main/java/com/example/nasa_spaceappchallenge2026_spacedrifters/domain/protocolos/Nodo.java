package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Un paso del árbol de un protocolo. Al llegar a él se aplica su nivel (si lo tiene: solo sube;
 * si es más bajo queda como bajada propuesta) y sus acciones pasan a ser las vigentes.
 * <ul>
 *   <li>CONDICION: comprueba un dato o síntoma; va a {@code si} o a {@code no}. Si falta el dato, o
 *       la rama es null, se queda aquí esperando.</li>
 *   <li>PRUEBA: pide pruebas; espera (ESPERANDO_PRUEBA) hasta que todas tienen resultado en los
 *       hechos (un hecho con el nombre de la prueba) y entonces va a {@code siguiente}.</li>
 *   <li>DECISION: la primera rama cuya condición se cumple; si ninguna, {@code otro}.</li>
 *   <li>FIN: termina con su nivel y acciones; {@code cerrar} = el protocolo se cierra.</li>
 * </ul>
 */
public final class Nodo {

    public enum Tipo { CONDICION, PRUEBA, DECISION, FIN }

    /** Una salida de un nodo DECISION. */
    public static final class Rama {
        private final Condicion condicion;
        private final String siguiente;

        public Rama(Condicion condicion, String siguiente) {
            this.condicion = Objects.requireNonNull(condicion);
            this.siguiente = Objects.requireNonNull(siguiente);
        }

        public Condicion getCondicion() { return condicion; }
        public String getSiguiente() { return siguiente; }
    }

    private final String id;
    private final Tipo tipo;
    private final Condicion condicion;
    private final String si;
    private final String no;
    private final List<String> pruebas;
    private final String siguiente;
    private final List<Rama> ramas;
    private final String otro;
    private final Integer nivel;
    private final List<String> acciones;
    private final boolean cerrar;

    public Nodo(String id, Tipo tipo, Condicion condicion, String si, String no, List<String> pruebas,
                String siguiente, List<Rama> ramas, String otro, Integer nivel, List<String> acciones,
                boolean cerrar) {
        this.id = Objects.requireNonNull(id);
        this.tipo = Objects.requireNonNull(tipo);
        this.condicion = condicion;
        this.si = si;
        this.no = no;
        this.pruebas = Collections.unmodifiableList(new ArrayList<>(pruebas));
        this.siguiente = siguiente;
        this.ramas = Collections.unmodifiableList(new ArrayList<>(ramas));
        this.otro = otro;
        this.nivel = nivel;
        this.acciones = Collections.unmodifiableList(new ArrayList<>(acciones));
        this.cerrar = cerrar;

        if (nivel != null && (nivel < NivelGravedad.MINIMO || nivel > NivelGravedad.MAXIMO)) {
            throw new IllegalArgumentException(id + ": nivel fuera de 1-5");
        }
        switch (tipo) {
            case CONDICION:
                if (condicion == null) throw new IllegalArgumentException(id + ": CONDICION sin condición");
                if (si == null && no == null) throw new IllegalArgumentException(id + ": CONDICION sin salidas");
                break;
            case PRUEBA:
                if (this.pruebas.isEmpty()) throw new IllegalArgumentException(id + ": PRUEBA sin pruebas");
                if (siguiente == null) throw new IllegalArgumentException(id + ": PRUEBA sin siguiente");
                break;
            case DECISION:
                if (this.ramas.isEmpty()) throw new IllegalArgumentException(id + ": DECISION sin ramas");
                break;
            case FIN:
                if (nivel == null) throw new IllegalArgumentException(id + ": FIN sin nivel");
                break;
        }
    }

    public String getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public Condicion getCondicion() { return condicion; }
    public String getSi() { return si; }
    public String getNo() { return no; }
    public List<String> getPruebas() { return pruebas; }
    public String getSiguiente() { return siguiente; }
    public List<Rama> getRamas() { return ramas; }
    public String getOtro() { return otro; }
    /** Nivel que asigna al llegar, o null si no lo cambia. */
    public Integer getNivel() { return nivel; }
    /** Códigos de acción (p. ej. "PEDIR_ANALITICA"); los textos irán en strings.xml. */
    public List<String> getAcciones() { return acciones; }
    public boolean isCerrar() { return cerrar; }

    /** Nodos a los que puede ir desde aquí. */
    public List<String> salidas() {
        List<String> s = new ArrayList<>();
        if (si != null) s.add(si);
        if (no != null) s.add(no);
        if (siguiente != null) s.add(siguiente);
        for (Rama r : ramas) s.add(r.siguiente);
        if (otro != null) s.add(otro);
        return s;
    }
}
