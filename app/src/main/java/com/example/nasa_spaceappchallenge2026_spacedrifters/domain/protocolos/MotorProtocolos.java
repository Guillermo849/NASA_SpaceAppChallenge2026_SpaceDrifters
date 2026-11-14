package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor genérico que recorre los árboles de assets/protocolos.json. No tiene lógica médica propia:
 * todo sale del JSON. Reglas comunes a todos los protocolos:
 * <ul>
 *   <li>Los signos de alarma se miran antes que nada, y después la emergencia propia del protocolo:
 *       cualquiera de ellos lleva a nivel 5 (nodo {@link #NODO_EMERGENCIA}) esté donde esté.</li>
 *   <li>El nivel solo sube solo. Si el árbol llega a un nodo de nivel más bajo, queda como
 *       {@code nivelPropuesto} hasta que alguien lo confirme con {@link #confirmarNivel}.</li>
 *   <li>Cada subida es un {@link Cambio} con el dato que la provoca.</li>
 * </ul>
 * Las pruebas se dan por hechas cuando hay un hecho con su nombre (p. ej. "ECO_DOPPLER_YUGULAR").
 */
public final class MotorProtocolos {

    /** Nodo al que va un protocolo cuando salta la emergencia (no existe en el JSON). */
    public static final String NODO_EMERGENCIA = "EMERGENCIA";
    /** Acción de la pantalla de emergencia. */
    public static final String ACCION_EMERGENCIA = "PROTOCOLO_EMERGENCIA";
    /** Nivel con el que empieza un protocolo antes de aplicar su primer nodo. */
    public static final int NIVEL_INICIAL = NivelGravedad.INFORMATIVO.getNumero();

    private static final int NIVEL_EMERGENCIA = NivelGravedad.EMERGENCIA.getNumero();
    /** Un árbol bien hecho no tiene ciclos; esto evita colgarse si alguno se cuela en el JSON. */
    private static final int MAX_PASOS = 100;

    private final Map<String, Protocolo> protocolos;
    private final List<String> signosAlarma;

    public MotorProtocolos(List<Protocolo> protocolos, List<String> signosAlarma) {
        Map<String, Protocolo> mapa = new LinkedHashMap<>();
        for (Protocolo p : protocolos) {
            if (mapa.put(p.getId(), p) != null) throw new IllegalArgumentException("Protocolo repetido: " + p.getId());
        }
        this.protocolos = Collections.unmodifiableMap(mapa);
        this.signosAlarma = Collections.unmodifiableList(new ArrayList<>(signosAlarma));
    }

    public List<Protocolo> getProtocolos() { return new ArrayList<>(protocolos.values()); }

    public List<String> getSignosAlarma() { return signosAlarma; }

    public Protocolo protocolo(String id) {
        Protocolo p = protocolos.get(id);
        if (p == null) throw new IllegalArgumentException("No existe el protocolo " + id);
        return p;
    }

    /** Signos de alarma presentes ahora (hecho = true). Si hay alguno, emergencia aunque no haya protocolo. */
    public List<String> signosAlarmaPresentes(Hechos h) {
        List<String> presentes = new ArrayList<>();
        for (String s : signosAlarma) if (h.esCierto(s)) presentes.add(s);
        return presentes;
    }

    /** Si los hechos activan el protocolo (para abrirlo si no hay ya uno abierto de ese tipo). */
    public boolean seActiva(String protocoloId, Hechos h) {
        return protocolo(protocoloId).getActivacion().evaluar(h) == Veredicto.SI;
    }

    /**
     * Abre un protocolo y lo recorre desde su inicio.
     *
     * @return null si los hechos no lo activan
     */
    public ResultadoProtocolo iniciar(String protocoloId, Hechos h) {
        Protocolo p = protocolo(protocoloId);
        if (p.getActivacion().evaluar(h) != Veredicto.SI) return null;
        return recorrer(p, p.getInicio(), NIVEL_INICIAL, p.getActivacion().motivo(h), h);
    }

    /**
     * Sigue un protocolo abierto desde el nodo en el que estaba, con los hechos de ahora (p. ej.
     * cuando llega el resultado de una prueba o un síntoma nuevo).
     */
    public ResultadoProtocolo avanzar(String protocoloId, String nodoActual, int nivel, Hechos h) {
        return recorrer(protocolo(protocoloId), nodoActual, nivel, null, h);
    }

    /**
     * Cambio de nivel manual (bajar o subir) confirmado por la oficial médico o el médico.
     *
     * @throws IllegalArgumentException si no hay quién confirma o el nivel no es 1–5
     */
    public static Cambio confirmarNivel(String nodo, int nivelActual, int nivelNuevo, String motivo, String confirmadoPor) {
        if (confirmadoPor == null || confirmadoPor.trim().isEmpty()) {
            throw new IllegalArgumentException("Cambiar el nivel a mano necesita quién lo confirma");
        }
        NivelGravedad.de(nivelNuevo);
        return new Cambio(nodo, nivelActual, nivelNuevo, motivo, confirmadoPor);
    }

    private ResultadoProtocolo recorrer(Protocolo p, String desde, int nivelInicial, String motivoEntrada, Hechos h) {
        List<Cambio> cambios = new ArrayList<>();
        List<String> recorrido = new ArrayList<>();

        // 1. Emergencia antes que cualquier paso del árbol.
        String alarma = motivoEmergencia(p, h);
        if (alarma != null || NODO_EMERGENCIA.equals(desde)) {
            if (nivelInicial < NIVEL_EMERGENCIA) {
                cambios.add(new Cambio(NODO_EMERGENCIA, nivelInicial, NIVEL_EMERGENCIA, alarma, null));
            }
            recorrido.add(NODO_EMERGENCIA);
            return new ResultadoProtocolo(p.getId(), NODO_EMERGENCIA, NIVEL_EMERGENCIA, EstadoProtocolo.ABIERTO, null,
                    Collections.singletonList(ACCION_EMERGENCIA), Collections.<String>emptyList(), cambios, recorrido, true);
        }

        // 2. Recorrer el árbol hasta un nodo en el que haya que esperar o terminar.
        int nivel = nivelInicial;
        Integer propuesto = null;
        List<String> acciones = Collections.emptyList();
        String motivo = motivoEntrada;
        String id = desde;

        for (int paso = 0; paso < MAX_PASOS; paso++) {
            Nodo n = p.nodo(id);
            recorrido.add(id);

            // Nivel del nodo: sube solo; si es más bajo, queda propuesto.
            Integer nivelNodo = n.getNivel();
            if (nivelNodo != null) {
                if (nivelNodo > nivel) {
                    cambios.add(new Cambio(id, nivel, nivelNodo, motivo == null || motivo.isEmpty() ? id : motivo, null));
                    nivel = nivelNodo;
                    propuesto = null;
                } else if (nivelNodo < nivel) {
                    propuesto = nivelNodo;
                } else {
                    propuesto = null;
                }
            }
            if (!n.getAcciones().isEmpty()) acciones = n.getAcciones();

            String siguiente;
            switch (n.getTipo()) {
                case CONDICION: {
                    Veredicto v = n.getCondicion().evaluar(h);
                    siguiente = v == Veredicto.SI ? n.getSi() : v == Veredicto.NO ? n.getNo() : null;
                    if (siguiente != null && v == Veredicto.SI) motivo = n.getCondicion().motivo(h);
                    if (siguiente != null && v == Veredicto.NO) motivo = "NO:" + String.join("+", n.getCondicion().hechosUsados());
                    break;
                }
                case PRUEBA: {
                    List<String> faltan = new ArrayList<>();
                    List<String> resultados = new ArrayList<>();
                    for (String prueba : n.getPruebas()) {
                        Object r = h.get(prueba);
                        if (r == null) faltan.add(prueba);
                        else resultados.add(Boolean.TRUE.equals(r) ? prueba : prueba + "=" + r);
                    }
                    if (!faltan.isEmpty()) {
                        return new ResultadoProtocolo(p.getId(), id, nivel, EstadoProtocolo.ESPERANDO_PRUEBA, propuesto,
                                acciones, faltan, cambios, recorrido, false);
                    }
                    siguiente = n.getSiguiente();
                    motivo = String.join("+", resultados);
                    break;
                }
                case DECISION: {
                    siguiente = null;
                    boolean esperar = false;
                    for (Nodo.Rama r : n.getRamas()) {
                        Veredicto v = r.getCondicion().evaluar(h);
                        if (v == Veredicto.SI) {
                            siguiente = r.getSiguiente();
                            motivo = r.getCondicion().motivo(h);
                            break;
                        }
                        if (v == Veredicto.DESCONOCIDO) {
                            esperar = true; // falta un dato para decidir esta rama: no se salta a la siguiente
                            break;
                        }
                    }
                    if (!esperar && siguiente == null) siguiente = n.getOtro();
                    if (esperar) siguiente = null;
                    break;
                }
                case FIN: {
                    EstadoProtocolo estado = n.isCerrar() ? EstadoProtocolo.CERRADO : EstadoProtocolo.ABIERTO;
                    return new ResultadoProtocolo(p.getId(), id, nivel, estado, propuesto, acciones,
                            Collections.<String>emptyList(), cambios, recorrido, nivel >= NIVEL_EMERGENCIA);
                }
                default:
                    throw new IllegalStateException();
            }

            if (siguiente == null) {
                // Falta un dato o no hay rama para lo que se sabe: se queda aquí hasta que cambie algo.
                return new ResultadoProtocolo(p.getId(), id, nivel, EstadoProtocolo.ABIERTO, propuesto, acciones,
                        Collections.<String>emptyList(), cambios, recorrido, nivel >= NIVEL_EMERGENCIA);
            }
            id = siguiente;
        }
        throw new IllegalStateException(p.getId() + ": el árbol tiene un ciclo (más de " + MAX_PASOS + " pasos)");
    }

    /** Motivo de emergencia ("SINCOPE", "DISNEA"...) o null si no la hay. */
    private String motivoEmergencia(Protocolo p, Hechos h) {
        List<String> alarmas = signosAlarmaPresentes(h);
        if (!alarmas.isEmpty()) return String.join("+", alarmas);
        Condicion propia = p.getEmergencia();
        if (propia != null && propia.evaluar(h) == Veredicto.SI) {
            String m = propia.motivo(h);
            return m.isEmpty() ? NODO_EMERGENCIA : m;
        }
        return null;
    }

    @Override
    public String toString() {
        return "MotorProtocolos" + protocolos.keySet();
    }
}
