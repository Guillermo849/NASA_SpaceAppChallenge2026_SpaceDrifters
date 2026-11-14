package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Protocolo 5 (assets/protocolos.json, "ambiental"): si varios astronautas tienen a la vez una
 * alteración del mismo tipo dentro de la ventana, la causa probable es ambiental (CO2, O2,
 * presión, temperatura, soporte vital) y no individual.
 */
public final class DetectorAmbiental {

    /** Configuración del protocolo 5. */
    public static final class Config {
        private final String id;
        private final long ventanaMs;
        private final int minAstronautas;
        private final int nivel;
        private final Set<String> alteraciones;
        private final List<String> acciones;

        public Config(String id, long ventanaMs, int minAstronautas, int nivel, Set<String> alteraciones, List<String> acciones) {
            this.id = Objects.requireNonNull(id);
            if (ventanaMs <= 0) throw new IllegalArgumentException("ventana <= 0");
            if (minAstronautas < 2) throw new IllegalArgumentException("minAstronautas < 2");
            NivelGravedad.de(nivel);
            this.ventanaMs = ventanaMs;
            this.minAstronautas = minAstronautas;
            this.nivel = nivel;
            this.alteraciones = Collections.unmodifiableSet(new LinkedHashSet<>(alteraciones));
            this.acciones = Collections.unmodifiableList(new ArrayList<>(acciones));
        }

        public String getId() { return id; }
        public long getVentanaMs() { return ventanaMs; }
        public int getMinAstronautas() { return minAstronautas; }
        public int getNivel() { return nivel; }
        /** Tipos que cuentan, p. ej. "SPO2_BAJA", "CEFALEA", "DETERIORO_COGNITIVO". */
        public Set<String> getAlteraciones() { return alteraciones; }
        public List<String> getAcciones() { return acciones; }
    }

    /** Una alteración de un astronauta en un momento. */
    public static final class Alteracion {
        private final long astronautaId;
        private final String tipo;
        private final long instante;

        public Alteracion(long astronautaId, String tipo, long instante) {
            this.astronautaId = astronautaId;
            this.tipo = Objects.requireNonNull(tipo);
            this.instante = instante;
        }

        public long getAstronautaId() { return astronautaId; }
        public String getTipo() { return tipo; }
        public long getInstante() { return instante; }
    }

    /** "Posible causa ambiental": un tipo de alteración en varios astronautas. */
    public static final class Grupo {
        private final String tipo;
        private final Set<Long> astronautas;
        private final long desde;
        private final long hasta;

        Grupo(String tipo, Set<Long> astronautas, long desde, long hasta) {
            this.tipo = tipo;
            this.astronautas = Collections.unmodifiableSet(astronautas);
            this.desde = desde;
            this.hasta = hasta;
        }

        public String getTipo() { return tipo; }
        public Set<Long> getAstronautas() { return astronautas; }
        /** Primera y última alteración del grupo. */
        public long getDesde() { return desde; }
        public long getHasta() { return hasta; }
    }

    private final Config config;

    public DetectorAmbiental(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    public Config getConfig() { return config; }

    /**
     * Para cada tipo de alteración, la ventana con más astronautas distintos; sale si llega al mínimo.
     * Los tipos que no están en la configuración no cuentan.
     */
    public List<Grupo> detectar(List<Alteracion> alteraciones) {
        Map<String, List<Alteracion>> porTipo = new LinkedHashMap<>();
        for (Alteracion a : alteraciones) {
            if (!config.alteraciones.contains(a.tipo)) continue;
            List<Alteracion> lista = porTipo.get(a.tipo);
            if (lista == null) porTipo.put(a.tipo, lista = new ArrayList<>());
            lista.add(a);
        }

        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<String, List<Alteracion>> e : porTipo.entrySet()) {
            List<Alteracion> lista = e.getValue();
            Collections.sort(lista, new Comparator<Alteracion>() {
                @Override
                public int compare(Alteracion a, Alteracion b) { return Long.compare(a.instante, b.instante); }
            });
            Grupo mejor = null;
            for (int i = 0; i < lista.size(); i++) {
                long inicio = lista.get(i).instante;
                Set<Long> astronautas = new LinkedHashSet<>();
                long fin = inicio;
                for (int j = i; j < lista.size() && lista.get(j).instante - inicio <= config.ventanaMs; j++) {
                    astronautas.add(lista.get(j).astronautaId);
                    fin = lista.get(j).instante;
                }
                if (mejor == null || astronautas.size() > mejor.astronautas.size()) {
                    mejor = new Grupo(e.getKey(), astronautas, inicio, fin);
                }
            }
            if (mejor != null && mejor.astronautas.size() >= config.minAstronautas) grupos.add(mejor);
        }
        return grupos;
    }
}
