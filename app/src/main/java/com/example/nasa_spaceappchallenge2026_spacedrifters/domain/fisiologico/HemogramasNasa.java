package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hemogramas reales de NASA (Open Science Data Repository, estudio LSDS-7, archivo
 * assets/LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv): 4 tripulantes (C001–C004) con muestras
 * antes del vuelo (L-92, L-44, L-3) y tras el regreso (R+1, R+45, R+82, R+194).
 * <p>
 * Formato: una fila por muestra ("C002_whole-blood_R+82_cbc") y, por cada parámetro, tres columnas:
 * {@code <parámetro>_value_<unidad>}, {@code <parámetro>_range_min_<unidad>} y
 * {@code <parámetro>_range_max_<unidad>}. Un parámetro está fuera de rango si su valor queda por
 * debajo del mínimo o por encima del máximo de esa misma fila.
 */
public final class HemogramasNasa {

    private static final Pattern COLUMNA_VALOR = Pattern.compile("^(.+)_value_(.+)$");
    private static final Pattern MUESTRA = Pattern.compile("^(C\\d+)_whole-blood_([LR])([+-]\\d+)_cbc$");

    private HemogramasNasa() {}

    /** Un parámetro de un hemograma con su rango de referencia. */
    public static final class Parametro {
        private final String codigo;
        private final String unidad;
        private final double valor;
        private final double minimo;
        private final double maximo;

        public Parametro(String codigo, String unidad, double valor, double minimo, double maximo) {
            this.codigo = Objects.requireNonNull(codigo);
            this.unidad = Objects.requireNonNull(unidad);
            this.valor = valor;
            this.minimo = minimo;
            this.maximo = maximo;
        }

        /** Nombre de la columna sin "_value_…", p. ej. "hemoglobin" o "platelet_count". */
        public String getCodigo() { return codigo; }
        /** Unidad tal y como viene en el archivo, p. ej. "cells_per_microliter". */
        public String getUnidad() { return unidad; }
        public double getValor() { return valor; }
        public double getMinimo() { return minimo; }
        public double getMaximo() { return maximo; }

        public boolean isBajo() { return valor < minimo; }
        public boolean isAlto() { return valor > maximo; }
        public boolean isFueraDeRango() { return isBajo() || isAlto(); }
    }

    /** Una muestra de sangre de un tripulante. */
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

        /** Nombre de la muestra en el archivo, p. ej. "C002_whole-blood_R+82_cbc". */
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
        /** Parámetros que esta muestra no trae (casilla vacía en el archivo): "Sin dato", nunca "normal". */
        public List<String> getSinDato() { return sinDato; }

        public List<Parametro> fueraDeRango() {
            List<Parametro> fuera = new ArrayList<>();
            for (Parametro p : parametros) if (p.isFueraDeRango()) fuera.add(p);
            return fuera;
        }

        public boolean isAlterado() { return !fueraDeRango().isEmpty(); }
    }

    /** Lee todas las muestras del CSV (admite BOM y finales de línea \r\n). */
    public static List<Muestra> leer(String csv) {
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
            String unidad = m.group(2);
            int min = indice(cabecera, codigo + "_range_min_" + unidad);
            int max = indice(cabecera, codigo + "_range_max_" + unidad);
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
                Double min = numero(celdas, col[1]);
                Double max = numero(celdas, col[2]);
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
}
