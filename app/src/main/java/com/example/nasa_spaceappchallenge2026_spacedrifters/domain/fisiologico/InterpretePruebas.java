package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resultados de las pruebas a bordo (los introduce la oficial médico con opciones cerradas) y su
 * traducción a hechos para los protocolos de assets/protocolos.json:
 * <ul>
 *   <li>ECO_DOPPLER_YUGULAR {"flujo": NORMAL|ESTASIS|RETROGRADO|TROMBO, "trombo": bool} →
 *       ECO_DOPPLER_YUGULAR = flujo y ECO_DOPPLER_PEOR_QUE_ANTERIOR (frente al eco anterior).</li>
 *   <li>DIMERO_D / HEMOGRAMA / COAGULACION {"valor": NORMAL|…} → ANALITICA_ALTERADA si alguno no es
 *       normal (o false cuando las tres son normales).</li>
 *   <li>ECG {"valor": NORMAL|ALTERADO} → ECG_ALTERADO.</li>
 *   <li>REPETICION_CONSTANTES {"valor": MEJOR|IGUAL_O_PEOR} → REPETICION_ALTERADA.</li>
 *   <li>ECOCARDIO {"vs_ml": n, "gc_lmin": n} → VS_BAJO / GC_BAJO frente a su primer ecocardio o,
 *       si no tiene, a la referencia de config_umbrales.json.</li>
 * </ul>
 * Además, cada prueba hecha deja un hecho con su nombre: es lo que espera un nodo PRUEBA.
 */
public final class InterpretePruebas {

    public static final String ECO_DOPPLER_YUGULAR = "ECO_DOPPLER_YUGULAR";
    public static final String ECOCARDIO = "ECOCARDIO";
    public static final String ECG = "ECG";
    public static final String DIMERO_D = "DIMERO_D";
    public static final String HEMOGRAMA = "HEMOGRAMA";
    public static final String COAGULACION = "COAGULACION";
    public static final String REPETICION_CONSTANTES = "REPETICION_CONSTANTES";

    public static final String NORMAL = "NORMAL";
    public static final String TROMBO = "TROMBO";
    public static final String IGUAL_O_PEOR = "IGUAL_O_PEOR";

    /** Campos del ecocardio. */
    public static final String VS_ML = "vs_ml";
    public static final String GC_LMIN = "gc_lmin";

    private static final List<String> ANALITICA = Arrays.asList(DIMERO_D, HEMOGRAMA, COAGULACION);
    /** Del flujo yugular menos grave al más grave (para comparar con el eco anterior). */
    private static final List<String> FLUJO = Arrays.asList(NORMAL, "ESTASIS", "RETROGRADO", TROMBO);

    private static final Map<String, List<String>> OPCIONES = new LinkedHashMap<>();
    static {
        OPCIONES.put(ECO_DOPPLER_YUGULAR, FLUJO);
        OPCIONES.put(ECG, Arrays.asList(NORMAL, "ALTERADO"));
        OPCIONES.put(DIMERO_D, Arrays.asList(NORMAL, "ELEVADO"));
        OPCIONES.put(HEMOGRAMA, Arrays.asList(NORMAL, "ALTERADO"));
        OPCIONES.put(COAGULACION, Arrays.asList(NORMAL, "ALTERADA"));
        OPCIONES.put(REPETICION_CONSTANTES, Arrays.asList("MEJOR", IGUAL_O_PEOR));
    }

    /** Un resultado ya introducido. */
    public static final class Resultado {
        private final String tipo;
        private final Map<String, Object> valores;
        private final long realizadaEn;

        public Resultado(String tipo, Map<String, Object> valores, long realizadaEn) {
            this.tipo = Objects.requireNonNull(tipo);
            this.valores = Collections.unmodifiableMap(new HashMap<>(valores));
            this.realizadaEn = realizadaEn;
        }

        public String getTipo() { return tipo; }
        public Map<String, Object> getValores() { return valores; }
        public long getRealizadaEn() { return realizadaEn; }
    }

    private final UmbralesFisiologicos umbrales;

    public InterpretePruebas(UmbralesFisiologicos umbrales) {
        this.umbrales = Objects.requireNonNull(umbrales);
    }

    // ---- Formulario ----

    /** Opciones cerradas de una prueba (de menos a más alterado); vacía si se introducen números (ECOCARDIO). */
    public static List<String> opciones(String tipo) {
        List<String> o = OPCIONES.get(tipo);
        return o == null ? Collections.<String>emptyList() : Collections.unmodifiableList(o);
    }

    /** Resultado de una prueba de opciones cerradas, listo para guardar como JSON. */
    public static Map<String, Object> resultadoCerrado(String tipo, String opcion) {
        if (!opciones(tipo).contains(opcion)) throw new IllegalArgumentException(tipo + ": opción no válida " + opcion);
        Map<String, Object> r = new LinkedHashMap<>();
        if (ECO_DOPPLER_YUGULAR.equals(tipo)) {
            r.put("flujo", opcion);
            r.put("trombo", TROMBO.equals(opcion));
        } else {
            r.put("valor", opcion);
        }
        return r;
    }

    /** Resultado de un ecocardio: volumen sistólico (ml) y gasto cardiaco (l/min). */
    public static Map<String, Object> resultadoEcocardio(double vsMl, double gcLmin) {
        if (vsMl <= 0 || gcLmin <= 0) throw new IllegalArgumentException("VS y GC tienen que ser positivos");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put(VS_ML, vsMl);
        r.put(GC_LMIN, gcLmin);
        return r;
    }

    /** Texto principal del resultado ("RETROGRADO", "ELEVADO"...), o null para el ecocardio. */
    public static String opcionDe(String tipo, Map<String, Object> valores) {
        if (ECO_DOPPLER_YUGULAR.equals(tipo)) {
            if (Boolean.TRUE.equals(valores.get("trombo"))) return TROMBO;
            Object f = valores.get("flujo");
            return f == null ? null : f.toString();
        }
        Object v = valores.get("valor");
        return v == null ? null : v.toString();
    }

    // ---- Hechos para los protocolos ----

    /**
     * @param delProtocolo pruebas ya hechas dentro de este protocolo
     * @param historial    todas las pruebas hechas por el astronauta (para comparar con las anteriores)
     */
    public Hechos hechos(List<Resultado> delProtocolo, List<Resultado> historial) {
        Map<String, Resultado> ultima = new HashMap<>();
        for (Resultado r : delProtocolo) {
            Resultado previa = ultima.get(r.tipo);
            if (previa == null || r.realizadaEn >= previa.realizadaEn) ultima.put(r.tipo, r);
        }

        Hechos h = new Hechos();
        for (Resultado r : ultima.values()) {
            String opcion = opcionDe(r.tipo, r.valores);
            h.con(r.tipo, opcion != null ? opcion : Boolean.TRUE);
        }

        Resultado doppler = ultima.get(ECO_DOPPLER_YUGULAR);
        if (doppler != null) h.con("ECO_DOPPLER_PEOR_QUE_ANTERIOR", peorQueAnterior(doppler, historial));

        Boolean analitica = analiticaAlterada(ultima);
        if (analitica != null) h.con("ANALITICA_ALTERADA", analitica);

        Resultado ecg = ultima.get(ECG);
        if (ecg != null) h.con("ECG_ALTERADO", !NORMAL.equals(opcionDe(ECG, ecg.valores)));

        Resultado repeticion = ultima.get(REPETICION_CONSTANTES);
        if (repeticion != null) {
            h.con("REPETICION_ALTERADA", IGUAL_O_PEOR.equals(opcionDe(REPETICION_CONSTANTES, repeticion.valores)));
        }

        Resultado eco = ultima.get(ECOCARDIO);
        if (eco != null) h.con(ecocardio(eco, historial));
        return h;
    }

    private static boolean peorQueAnterior(Resultado actual, List<Resultado> historial) {
        Resultado anterior = null;
        for (Resultado r : historial) {
            if (!ECO_DOPPLER_YUGULAR.equals(r.tipo) || r.realizadaEn >= actual.realizadaEn) continue;
            if (anterior == null || r.realizadaEn > anterior.realizadaEn) anterior = r;
        }
        if (anterior == null) return false;
        return FLUJO.indexOf(opcionDe(ECO_DOPPLER_YUGULAR, actual.valores))
                > FLUJO.indexOf(opcionDe(ECO_DOPPLER_YUGULAR, anterior.valores));
    }

    /** true si alguna no es normal; false si las tres son normales; null si faltan y ninguna está alterada. */
    private static Boolean analiticaAlterada(Map<String, Resultado> ultima) {
        int normales = 0;
        for (String tipo : ANALITICA) {
            Resultado r = ultima.get(tipo);
            if (r == null) continue;
            if (!NORMAL.equals(opcionDe(tipo, r.valores))) return true;
            normales++;
        }
        return normales == ANALITICA.size() ? Boolean.FALSE : null;
    }

    private Hechos ecocardio(Resultado eco, List<Resultado> historial) {
        // Línea base: su primer ecocardio anterior a este; si no tiene, la referencia de la configuración.
        Resultado base = null;
        for (Resultado r : historial) {
            if (!ECOCARDIO.equals(r.tipo) || r.realizadaEn >= eco.realizadaEn) continue;
            if (base == null || r.realizadaEn < base.realizadaEn) base = r;
        }
        Map<String, Double> valores = new HashMap<>();
        Map<String, Double> bases = new HashMap<>();
        poner(valores, "VS", eco.valores.get(VS_ML));
        poner(valores, "GC", eco.valores.get(GC_LMIN));
        if (base != null) {
            poner(bases, "VS", base.valores.get(VS_ML));
            poner(bases, "GC", base.valores.get(GC_LMIN));
        } else {
            poner(bases, "VS", umbrales.referencia("VS"));
            poner(bases, "GC", umbrales.referencia("GC"));
        }
        return umbrales.evaluar(valores, bases);
    }

    private static void poner(Map<String, Double> mapa, String clave, Object valor) {
        if (valor instanceof Number) mapa.put(clave, ((Number) valor).doubleValue());
    }
}
