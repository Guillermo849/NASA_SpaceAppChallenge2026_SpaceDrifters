package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt;

import java.util.Objects;

/**
 * Lo que debe mostrar la pantalla en cada momento de la prueba PVT.
 *
 * Secuencia de cada ensayo:
 *   ISI (pantalla negra con «+») → Estímulo (contador ms) → Respuesta / Lapso
 *   → (solo práctica) Feedback → siguiente ensayo.
 *   Al agotar el tiempo total → Terminada.
 */
public abstract class FasePvt {

    private FasePvt() {}

    // ── Creada pero sin empezar ──────────────────────────────────────────────
    public static final class Preparada extends FasePvt {
        public static final Preparada INSTANCIA = new Preparada();
        private Preparada() {}
    }

    // ── ISI: intervalo entre estímulos (cruz de fijación) ───────────────────
    public static final class Isi extends FasePvt {
        private final int numero;   // número de ensayo que viene a continuación

        public Isi(int numero) { this.numero = numero; }

        public int getNumero() { return numero; }

        @Override public boolean equals(Object o) { return o instanceof Isi && ((Isi) o).numero == numero; }
        @Override public int hashCode() { return numero; }
    }

    // ── Estímulo activo: el contador corre en pantalla ───────────────────────
    public static final class Estimulo extends FasePvt {
        private final int numero;
        /** Instante en que el estímulo se dibujó en pantalla (nanos del reloj monótono). */
        private final long inicioNanos;

        public Estimulo(int numero, long inicioNanos) {
            this.numero = numero;
            this.inicioNanos = inicioNanos;
        }

        public int getNumero() { return numero; }
        public long getInicioNanos() { return inicioNanos; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Estimulo)) return false;
            Estimulo otro = (Estimulo) o;
            return otro.numero == numero && otro.inicioNanos == inicioNanos;
        }
        @Override public int hashCode() { return Objects.hash(numero, inicioNanos); }
    }

    // ── Solo práctica: feedback tras el ensayo ───────────────────────────────
    public static final class Feedback extends FasePvt {
        private final ResultadoEnsayoPvt resultado;

        public Feedback(ResultadoEnsayoPvt resultado) {
            this.resultado = resultado;
        }

        public ResultadoEnsayoPvt getResultado() { return resultado; }
    }

    // ── Sesión completada ────────────────────────────────────────────────────
    public static final class Terminada extends FasePvt {
        public static final Terminada INSTANCIA = new Terminada();
        private Terminada() {}
    }
}
