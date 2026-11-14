package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analíticas reales de NASA (Open Science Data Repository, misión SpaceX Inspiration4): 4
 * tripulantes (C001–C004) con muestras antes del vuelo (L-92, L-44, L-3) y tras el regreso (R+1,
 * R+45, R+82, R+194).
 * <ul>
 *   <li>Hemograma: LSDS-7 (OSD-569), assets/LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv,
 *       muestras "C002_whole-blood_R+82_cbc".</li>
 *   <li>Bioquímica (panel metabólico completo): LSDS-8 (OSD-575),
 *       assets/LSDS-8_Comprehensive_Metabolic_Panel_CMP_TRANSFORMED.csv, muestras "C002_serum_R+82".</li>
 * </ul>
 * Formato: una fila por muestra y, por cada parámetro, tres columnas: {@code <parámetro>_value[_<unidad>]},
 * {@code <parámetro>_range_min[_<unidad>]} y {@code <parámetro>_range_max[_<unidad>]} (en LSDS-8 algunas
 * se llaman {@code <parámetro>_max_<unidad>}). Los cocientes no llevan unidad.
 * <p>
 * Un parámetro está fuera de rango si su valor queda por debajo del mínimo o por encima del máximo de
 * esa misma fila. Un límite "N/A" es un rango abierto (el eGFR no tiene máximo). Un valor vacío o "NA"
 * es "sin dato", nunca "normal".
 */
public final class AnaliticasNasa {

    private static final Pattern COLUMNA_VALOR = Pattern.compile("^(.+)_value(?:_(.+))?$");
    private static final Pattern MUESTRA = Pattern.compile("^(C\\d+)_[a-z-]+_([LR])([+-]\\d+)(?:_[a-z]+)?$");
    /** Límite de rango sin valor: rango abierto por ese lado. */
    private static final String SIN_LIMITE = "N/A";

    private AnaliticasNasa() {}

    /** Un parámetro de una analítica con su rango de referencia. */
    public static final class Parametro {
        private final String codigo;
        private final String unidad;
        private final double valor;
        private final double minimo;
        private final double maximo;

        /** Sin mínimo: Double.NEGATIVE_INFINITY; sin máximo: Double.POSITIVE_INFINITY. */
        public Parametro(String codigo, String unidad, double valor, double minimo, double maximo) {
            this.codigo = Objects.requireNonNull(codigo);
            this.unidad = Objects.requireNonNull(unidad);
            this.valor = valor;
            this.minimo = minimo;
            this.maximo = maximo;
        }

        /** Nombre de la columna sin "_value…", p. ej. "hemoglobin" o "bun_to_creatinine_ratio". */
        public String getCodigo() { return codigo; }
        /** Unidad tal y como viene en el archivo, p. ej. "cells_per_microliter"; vacía en los cocientes. */
        public String getUnidad() { return unidad; }
        public double getValor() { return valor; }
        public double getMinimo() { return minimo; }
        public double getMaximo() { return maximo; }
        public boolean hasMinimo() { return !Double.isInfinite(minimo); }
        public boolean hasMaximo() { return !Double.isInfinite(maximo); }

        public boolean isBajo() { return valor < minimo; }
        public boolean isAlto() { return valor > maximo; }
        public boolean isFueraDeRango() { return isBajo() || isAlto(); }
    }

    /** Una muestra (de sangre o de suero) de un tripulante. */
    public static final class Muestra {
        private final String nombre;
        private final String tripulante;
        private final boolean antesDelVuelo;
        private final int dias;
        private final List<Parametro> parametros;
        private final List<String> sinDato;

        Muestra(String nombre, String tripulante, boolean antesDelVuelo, int dias, List<Parametro> parametros,
                List<String> sinDato) {
            this.nombre = nombre;
            this.tripulante = tripulante;
            this.antesDelVuelo = antesDelVuelo;
            this.dias = dias;
            this.parametros = Collections.unmodifiableList(new ArrayList<>(parametros));
            this.sinDato = Collections.unmodifiableList(new ArrayList<>(sinDato));
        }

        /** Nombre de la muestra en el archivo, p. ej. "C002_whole-blood_R+82_cbc" o "C002_serum_R+82". */
        public String getNombre() { return nombre; }
        /** Código del tripulante, p. ej. "C002". */
        public String getTripulante() { return tripulante; }
        /** L = antes del lanzamiento; R = después del regreso. */
        public boolean isAntesDelVuelo() { return antesDelVuelo; }
        /** Días respecto al lanzamiento (L, negativos) o al regreso (R, positivos), sin signo. */
        public int getDias() { return dias; }
        /** "L-3", "R+82"... */
        public String getMomento() { return (antesDelVuelo ? "L-" : "R+") + dias; }
        public List<Parametro> getParametros() { return parametros; }
        /** Parámetros que esta muestra no trae (casilla vacía o "NA"): "Sin dato", nunca "normal". */
        public List<String> getSinDato() { return sinDato; }

        public List<Parametro> fueraDeRango() {
            List<Parametro> fuera = new ArrayList<>();
            for (Parametro p : parametros) if (p.isFueraDeRango()) fuera.add(p);
            return fuera;
        }

        /** El parámetro con ese código, o null si la muestra no lo trae. */
        public Parametro parametro(String codigo) {
            for (Parametro p : parametros) if (p.codigo.equals(codigo)) return p;
            return null;
        }

        public boolean isAlterado() { return !fueraDeRango().isEmpty(); }
    }

    /** Lee todas las muestras del CSV (admite BOM y finales de línea \r\n). */
    public static List<Muestra> leer(String csv) {
        return leer(csv, Collections.<String>emptyList());
    }

    /**
     * @param excluir códigos de parámetro que no se leen (p. ej. el eGFR, que en LSDS-8 se calcula con
     *                una fórmula basada en la raza y sale de la creatinina, que ya se muestra)
     */
    public static List<Muestra> leer(String csv, Collection<String> excluir) {
        String texto = csv.startsWith("﻿") ? csv.substring(1) : csv;
        String[] lineas = texto.split("\r?\n");
        if (lineas.length < 2) return Collections.emptyList();

        String[] cabecera = lineas[0].split(",", -1);
        List<int[]> columnas = new ArrayList<>(); // {valor, mínimo, máximo}
        List<String[]> nombres = new ArrayList<>(); // {código, unidad}
        for (int i = 1; i < cabecera.length; i++) {
            Matcher m = COLUMNA_VALOR.matcher(cabecera[i].trim());
            if (!m.matches()) continue;
            String codigo = m.group(1);
            if (excluir.contains(codigo)) continue;
            String unidad = m.group(2) == null ? "" : m.group(2);
            String sufijo = unidad.isEmpty() ? "" : "_" + unidad;
            int min = indice(cabecera, codigo + "_range_min" + sufijo);
            int max = indice(cabecera, codigo + "_range_max" + sufijo);
            if (max < 0) max = indice(cabecera, codigo + "_max" + sufijo);
            if (min < 0 || max < 0) continue;
            columnas.add(new int[] {i, min, max});
            nombres.add(new String[] {codigo, unidad});
        }

        List<Muestra> muestras = new ArrayList<>();
        for (int l = 1; l < lineas.length; l++) {
            if (lineas[l].trim().isEmpty()) continue;
            String[] celdas = lineas[l].split(",", -1);
            Matcher m = MUESTRA.matcher(celdas[0].trim());
            if (!m.matches()) continue;
            List<Parametro> parametros = new ArrayList<>();
            List<String> sinDato = new ArrayList<>();
            for (int c = 0; c < columnas.size(); c++) {
                int[] col = columnas.get(c);
                Double valor = numero(celdas, col[0]);
                Double min = limite(celdas, col[1], Double.NEGATIVE_INFINITY);
                Double max = limite(celdas, col[2], Double.POSITIVE_INFINITY);
                if (valor == null || min == null || max == null) {
                    sinDato.add(nombres.get(c)[0]);
                    continue;
                }
                parametros.add(new Parametro(nombres.get(c)[0], nombres.get(c)[1], valor, min, max));
            }
            muestras.add(new Muestra(celdas[0].trim(), m.group(1), m.group(2).equals("L"),
                    Math.abs(Integer.parseInt(m.group(3))), parametros, sinDato));
        }
        return muestras;
    }

    /** Las muestras de un tripulante en orden cronológico: L-92, L-44, L-3, R+1, R+45… */
    public static List<Muestra> de(List<Muestra> todas, String tripulante) {
        List<Muestra> suyas = new ArrayList<>();
        for (Muestra m : todas) if (m.tripulante.equals(tripulante)) suyas.add(m);
        Collections.sort(suyas, new Comparator<Muestra>() {
            @Override
            public int compare(Muestra a, Muestra b) { return Integer.compare(orden(a), orden(b)); }
        });
        return suyas;
    }

    private static int orden(Muestra m) { return m.antesDelVuelo ? -m.dias : 100_000 + m.dias; }

    private static int indice(String[] cabecera, String nombre) {
        for (int i = 0; i < cabecera.length; i++) if (cabecera[i].trim().equals(nombre)) return i;
        return -1;
    }

    private static Double numero(String[] celdas, int i) {
        if (i >= celdas.length) return null;
        try {
            return Double.parseDouble(celdas[i].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Un límite de rango: número, "N/A" (= sin límite por ese lado) o null si falta. */
    private static Double limite(String[] celdas, int i, double sinLimite) {
        if (i < celdas.length && SIN_LIMITE.equals(celdas[i].trim())) return sinLimite;
        return numero(celdas, i);
    }
}
