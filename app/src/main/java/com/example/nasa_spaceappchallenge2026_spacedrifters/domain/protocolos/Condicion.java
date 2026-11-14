package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Condición de un protocolo (assets/protocolos.json), evaluada sobre los {@link Hechos}:
 * <ul>
 *   <li>{@code {"hecho": "CEFALEA"}}: el hecho es true.</li>
 *   <li>{@code {"hecho": "ECO_DOPPLER_YUGULAR", "es": ["RETROGRADO", "ESTASIS"]}}: su valor es uno de esos.</li>
 *   <li>{@code {"hecho": "X", "mayorQue": 3}} / {@code "menorQue"}: comparación numérica.</li>
 *   <li>{@code {"todos": [...]}}, {@code {"alguno": [...]}}, {@code {"no": {...}}}.</li>
 * </ul>
 * Si falta un hecho el resultado es DESCONOCIDO, salvo que el resto ya decida (un "alguno" con
 * otro hecho cierto es SI; un "todos" con otro hecho falso es NO).
 */
public final class Condicion {

    public enum Tipo { HECHO, TODOS, ALGUNO, NO }

    private final Tipo tipo;
    private final String hecho;
    private final Set<String> valores;
    private final Double mayorQue;
    private final Double menorQue;
    private final List<Condicion> hijas;

    private Condicion(Tipo tipo, String hecho, Collection<String> valores, Double mayorQue, Double menorQue,
                      List<Condicion> hijas) {
        this.tipo = tipo;
        this.hecho = hecho;
        this.valores = Collections.unmodifiableSet(new LinkedHashSet<>(valores));
        this.mayorQue = mayorQue;
        this.menorQue = menorQue;
        this.hijas = Collections.unmodifiableList(new ArrayList<>(hijas));
    }

    /** Un hecho: cierto (sin más), con un valor de la lista o comparado con un número. */
    public static Condicion hecho(String hecho, Collection<String> es, Double mayorQue, Double menorQue) {
        Objects.requireNonNull(hecho, "falta el nombre del hecho");
        return new Condicion(Tipo.HECHO, hecho, es == null ? Collections.<String>emptyList() : es,
                mayorQue, menorQue, Collections.<Condicion>emptyList());
    }

    public static Condicion hecho(String hecho) {
        return hecho(hecho, null, null, null);
    }

    public static Condicion todos(List<Condicion> hijas) {
        if (hijas.isEmpty()) throw new IllegalArgumentException("\"todos\" vacío");
        return new Condicion(Tipo.TODOS, null, Collections.<String>emptyList(), null, null, hijas);
    }

    public static Condicion alguno(List<Condicion> hijas) {
        if (hijas.isEmpty()) throw new IllegalArgumentException("\"alguno\" vacío");
        return new Condicion(Tipo.ALGUNO, null, Collections.<String>emptyList(), null, null, hijas);
    }

    public static Condicion no(Condicion hija) {
        return new Condicion(Tipo.NO, null, Collections.<String>emptyList(), null, null,
                Collections.singletonList(hija));
    }

    public Tipo getTipo() { return tipo; }

    public Veredicto evaluar(Hechos h) {
        switch (tipo) {
            case HECHO:
                return evaluarHecho(h.get(hecho));
            case TODOS: {
                boolean desconocido = false;
                for (Condicion c : hijas) {
                    Veredicto v = c.evaluar(h);
                    if (v == Veredicto.NO) return Veredicto.NO;
                    if (v == Veredicto.DESCONOCIDO) desconocido = true;
                }
                return desconocido ? Veredicto.DESCONOCIDO : Veredicto.SI;
            }
            case ALGUNO: {
                boolean desconocido = false;
                for (Condicion c : hijas) {
                    Veredicto v = c.evaluar(h);
                    if (v == Veredicto.SI) return Veredicto.SI;
                    if (v == Veredicto.DESCONOCIDO) desconocido = true;
                }
                return desconocido ? Veredicto.DESCONOCIDO : Veredicto.NO;
            }
            case NO: {
                Veredicto v = hijas.get(0).evaluar(h);
                if (v == Veredicto.DESCONOCIDO) return v;
                return v == Veredicto.SI ? Veredicto.NO : Veredicto.SI;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private Veredicto evaluarHecho(Object valor) {
        if (valor == null) return Veredicto.DESCONOCIDO;
        if (!valores.isEmpty()) return Veredicto.de(valores.contains(String.valueOf(valor)));
        if (mayorQue != null || menorQue != null) {
            if (!(valor instanceof Number)) return Veredicto.DESCONOCIDO;
            double x = ((Number) valor).doubleValue();
            boolean ok = (mayorQue == null || x > mayorQue) && (menorQue == null || x < menorQue);
            return Veredicto.de(ok);
        }
        if (valor instanceof Boolean) return Veredicto.de((Boolean) valor);
        return Veredicto.SI; // un resultado presente (texto, número) cuenta como "hay dato"
    }

    /** Nombres de todos los hechos que mira la condición. */
    public Set<String> hechosUsados() {
        Set<String> nombres = new LinkedHashSet<>();
        if (hecho != null) nombres.add(hecho);
        for (Condicion c : hijas) nombres.addAll(c.hechosUsados());
        return nombres;
    }

    /**
     * El dato que la hace cierta, para el historial: "CEFALEA", "ECO_DOPPLER_YUGULAR=RETROGRADO",
     * "FC_ALTA+PAS_BAJA". Solo los hechos que están presentes y no son false.
     */
    public String motivo(Hechos h) {
        List<String> partes = new ArrayList<>();
        for (String nombre : hechosUsados()) {
            Object v = h.get(nombre);
            if (v == null || Boolean.FALSE.equals(v)) continue;
            partes.add(Boolean.TRUE.equals(v) ? nombre : nombre + "=" + v);
        }
        return String.join("+", partes);
    }
}
