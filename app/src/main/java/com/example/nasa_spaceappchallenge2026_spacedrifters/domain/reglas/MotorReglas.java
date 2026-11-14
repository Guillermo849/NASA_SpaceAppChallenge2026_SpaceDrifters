package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.DiaAgua;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.DiaBienestar;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.Noche;
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas.Sesion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Evalúa las reglas de alerta sobre el contexto de un astronauta. Lógica pura, sin Android.
 * Devuelve las reglas que se cumplen con los datos que las disparan ("Por qué ha saltado").
 */
public final class MotorReglas {

    /** Una regla que se cumple y los datos concretos que la cumplen. */
    public static final class ReglaCumplida {
        private final Regla regla;
        private final List<Sesion> sesiones;
        private final List<Noche> noches;
        private final Set<String> sintomas;
        private final List<DiaAgua> agua;
        private final List<DiaBienestar> bienestar;

        ReglaCumplida(Regla regla, List<Sesion> sesiones, List<Noche> noches, Set<String> sintomas) {
            this(regla, sesiones, noches, sintomas, Collections.emptyList());
        }

        ReglaCumplida(Regla regla, List<Sesion> sesiones, List<Noche> noches, Set<String> sintomas, List<DiaAgua> agua) {
            this(regla, sesiones, noches, sintomas, agua, Collections.emptyList());
        }

        ReglaCumplida(Regla regla, List<Sesion> sesiones, List<Noche> noches, Set<String> sintomas,
                      List<DiaAgua> agua, List<DiaBienestar> bienestar) {
            this.bienestar = Collections.unmodifiableList(bienestar);
            this.regla = regla;
            this.sesiones = Collections.unmodifiableList(sesiones);
            this.noches = Collections.unmodifiableList(noches);
            this.sintomas = Collections.unmodifiableSet(sintomas);
            this.agua = Collections.unmodifiableList(agua);
        }

        /** Días de hidratación implicados (del más reciente al más antiguo), si los hay. */
        public List<DiaAgua> getAgua() { return agua; }

        /** Check-ins de bienestar implicados (del más reciente al más antiguo), si los hay. */
        public List<DiaBienestar> getBienestar() { return bienestar; }

        public Regla getRegla() { return regla; }
        /** Sesiones que la disparan, de la más reciente a la más antigua. La primera es la que la dispara. */
        public List<Sesion> getSesiones() { return sesiones; }
        /** Noches de sueño implicadas (de la más reciente a la más antigua), si las hay. */
        public List<Noche> getNoches() { return noches; }
        /** Síntomas del registro implicados, si los hay. */
        public Set<String> getSintomas() { return sintomas; }
    }

    private MotorReglas() {
    }

    public static List<ReglaCumplida> evaluar(List<Regla> reglas, ContextoReglas contexto) {
        List<ReglaCumplida> cumplidas = new ArrayList<>();
        for (Regla regla : reglas) {
            ReglaCumplida r;
            switch (regla.getTipo()) {
                case TENDENCIA: r = tendencia(regla, contexto); break;
                case PRUEBA_Y_SUENO: r = pruebaYSueno(regla, contexto); break;
                case PRUEBA_Y_SINTOMAS: r = pruebaYSintomas(regla, contexto); break;
                case HIDRATACION_BAJA: r = hidratacionBaja(regla, contexto); break;
                case BIENESTAR_BAJO: r = bienestarBajo(regla, contexto); break;
                default: r = null;
            }
            if (r != null) cumplidas.add(r);
        }
        return cumplidas;
    }

    /** Las N últimas sesiones válidas de la prueba, todas en los estados de la regla. */
    private static ReglaCumplida tendencia(Regla regla, ContextoReglas c) {
        List<Sesion> deLaPrueba = sesionesDe(c, regla.getPruebas());
        int n = regla.getSesionesSeguidas();
        if (deLaPrueba.size() < n) return null;
        List<Sesion> ultimas = deLaPrueba.subList(0, n);
        for (Sesion s : ultimas) {
            if (!regla.getEstados().contains(s.getEstado())) return null;
        }
        return new ReglaCumplida(regla, new ArrayList<>(ultimas), Collections.emptyList(), Collections.emptySet());
    }

    /**
     * La última sesión de la prueba en los estados de la regla (y de las N últimas noches), y en
     * cada una de las N últimas noches (hoy incluida) durmió menos de sus horas habituales.
     * Una noche sin datos no cuenta como "bien" ni como "mal": la regla no salta.
     */
    private static ReglaCumplida pruebaYSueno(Regla regla, ContextoReglas c) {
        List<Sesion> deLaPrueba = sesionesDe(c, regla.getPruebas());
        if (deLaPrueba.isEmpty()) return null;
        Sesion ultima = deLaPrueba.get(0);
        int n = regla.getNochesBajoBase();
        LocalDate primeraNoche = c.getHoy().minusDays(n - 1L);
        if (!regla.getEstados().contains(ultima.getEstado()) || ultima.getFecha().isBefore(primeraNoche)) return null;

        Double base = c.getSuenoHabitual();
        if (base == null) return null;
        List<Noche> implicadas = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            Noche noche = nocheDe(c, c.getHoy().minusDays(k));
            if (noche == null || noche.getHoras() == null || noche.getHoras() >= base) return null;
            implicadas.add(noche);
        }
        return new ReglaCumplida(regla, Collections.singletonList(ultima), implicadas, Collections.emptySet());
    }

    /** Una sesión de hoy (de alguna de las pruebas) en los estados de la regla + alguno de sus síntomas hoy. */
    private static ReglaCumplida pruebaYSintomas(Regla regla, ContextoReglas c) {
        Set<String> sintomas = new LinkedHashSet<>(regla.getSintomas());
        sintomas.retainAll(c.getSintomasHoy());
        if (sintomas.isEmpty()) return null;
        for (Sesion s : sesionesDe(c, regla.getPruebas())) {
            if (s.getFecha().equals(c.getHoy()) && regla.getEstados().contains(s.getEstado())) {
                return new ReglaCumplida(regla, Collections.singletonList(s), Collections.emptyList(), sintomas);
            }
        }
        return null;
    }

    /**
     * En cada uno de los N últimos registros diarios (hoy incluido) el líquido anotado está por
     * debajo del umbral. Un día sin registro o sin anotar no cuenta como "bien" ni como "mal":
     * la regla no salta.
     */
    private static ReglaCumplida hidratacionBaja(Regla regla, ContextoReglas c) {
        List<DiaAgua> implicados = new ArrayList<>();
        for (int k = 0; k < regla.getDiasSeguidos(); k++) {
            DiaAgua dia = diaAguaDe(c, c.getHoy().minusDays(k));
            if (dia == null || dia.getLitros() == null || dia.getLitros() >= regla.getAguaMinLitros()) return null;
            implicados.add(dia);
        }
        return new ReglaCumplida(regla, Collections.emptyList(), Collections.emptyList(), Collections.emptySet(), implicados);
    }

    /**
     * En cada uno de los N últimos check-ins de bienestar (hoy incluido) alguno de los indicadores
     * está fuera de su umbral (ánimo bajo, estrés alto o aislamiento alto). Un día sin check-in
     * no la dispara.
     */
    private static ReglaCumplida bienestarBajo(Regla regla, ContextoReglas c) {
        UmbralesBienestar u = regla.getBienestar();
        List<DiaBienestar> implicados = new ArrayList<>();
        for (int k = 0; k < regla.getDiasSeguidos(); k++) {
            DiaBienestar dia = null;
            for (DiaBienestar d : c.getBienestar()) {
                if (Objects.equals(d.getFecha(), c.getHoy().minusDays(k))) dia = d;
            }
            if (dia == null) return null;
            boolean fuera = u.animoBajo(dia.getAnimo()) || u.estresAlto(dia.getEstres()) || u.aislamientoAlto(dia.getAislamiento());
            if (!fuera) return null;
            implicados.add(dia);
        }
        return new ReglaCumplida(regla, Collections.emptyList(), Collections.emptyList(), Collections.emptySet(),
                Collections.emptyList(), implicados);
    }

    private static DiaAgua diaAguaDe(ContextoReglas c, LocalDate fecha) {
        for (DiaAgua dia : c.getAgua()) {
            if (Objects.equals(dia.getFecha(), fecha)) return dia;
        }
        return null;
    }

    private static List<Sesion> sesionesDe(ContextoReglas c, List<String> pruebas) {
        List<Sesion> resultado = new ArrayList<>();
        for (Sesion s : c.getSesiones()) {
            if (pruebas.contains(s.getPrueba())) resultado.add(s);
        }
        return resultado;
    }

    private static Noche nocheDe(ContextoReglas c, LocalDate fecha) {
        for (Noche noche : c.getNoches()) {
            if (Objects.equals(noche.getFecha(), fecha)) return noche;
        }
        return null;
    }
}
