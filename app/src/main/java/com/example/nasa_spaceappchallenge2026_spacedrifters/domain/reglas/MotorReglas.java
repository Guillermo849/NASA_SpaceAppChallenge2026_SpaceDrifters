package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas;

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

        ReglaCumplida(Regla regla, List<Sesion> sesiones, List<Noche> noches, Set<String> sintomas) {
            this.regla = regla;
            this.sesiones = Collections.unmodifiableList(sesiones);
            this.noches = Collections.unmodifiableList(noches);
            this.sintomas = Collections.unmodifiableSet(sintomas);
        }

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
