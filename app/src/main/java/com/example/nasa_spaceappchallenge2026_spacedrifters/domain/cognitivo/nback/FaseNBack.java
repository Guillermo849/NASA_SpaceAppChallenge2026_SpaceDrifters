package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import java.util.Objects;

/** Lo que tiene que pintar la pantalla en cada momento del N-Back. */
public abstract class FaseNBack {

    private FaseNBack() {
    }

    /** Antes del primer estímulo (pantalla vacía). */
    public static final class Preparada extends FaseNBack {
        public static final Preparada INSTANCIA = new Preparada();

        private Preparada() {
        }
    }

    /** Forma en pantalla. */
    public static final class Estimulo extends FaseNBack {
        private final int indice;
        private final FormaNBack forma;

        public Estimulo(int indice, FormaNBack forma) {
            this.indice = indice;
            this.forma = forma;
        }

        public int getIndice() {
            return indice;
        }

        public FormaNBack getForma() {
            return forma;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Estimulo)) return false;
            Estimulo otro = (Estimulo) o;
            return otro.indice == indice && otro.forma == forma;
        }

        @Override
        public int hashCode() {
            return Objects.hash(indice, forma);
        }
    }

    /** Pantalla vacía tras la forma; todavía se acepta el toque de este ensayo. */
    public static final class Vacio extends FaseNBack {
        private final int indice;

        public Vacio(int indice) {
            this.indice = indice;
        }

        public int getIndice() {
            return indice;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Vacio && ((Vacio) o).indice == indice;
        }

        @Override
        public int hashCode() {
            return indice;
        }
    }

    /** Solo en práctica: aviso de acierto, falsa alarma u omisión del ensayo [indice]. */
    public static final class Feedback extends FaseNBack {
        private final int indice;
        private final ResultadoEnsayoNBack resultado;

        public Feedback(int indice, ResultadoEnsayoNBack resultado) {
            this.indice = indice;
            this.resultado = resultado;
        }

        public int getIndice() {
            return indice;
        }

        public ResultadoEnsayoNBack getResultado() {
            return resultado;
        }
    }

    /** Todos los estímulos hechos. */
    public static final class Terminada extends FaseNBack {
        public static final Terminada INSTANCIA = new Terminada();

        private Terminada() {
        }
    }
}
