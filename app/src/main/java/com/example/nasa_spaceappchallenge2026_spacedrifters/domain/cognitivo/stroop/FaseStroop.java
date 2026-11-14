package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import java.util.Objects;

/** Lo que tiene que pintar la pantalla en cada momento de la prueba. */
public abstract class FaseStroop {

    private FaseStroop() {
    }

    /** Creada pero sin empezar. */
    public static final class Preparada extends FaseStroop {
        public static final Preparada INSTANCIA = new Preparada();

        private Preparada() {
        }
    }

    /** Cruz de fijación antes del ensayo [indice]. */
    public static final class Fijacion extends FaseStroop {
        private final int indice;

        public Fijacion(int indice) {
            this.indice = indice;
        }

        public int getIndice() {
            return indice;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Fijacion && ((Fijacion) o).indice == indice;
        }

        @Override
        public int hashCode() {
            return indice;
        }
    }

    /** Palabra en pantalla, esperando respuesta. */
    public static final class Estimulo extends FaseStroop {
        private final int indice;
        private final EstimuloStroop estimulo;

        public Estimulo(int indice, EstimuloStroop estimulo) {
            this.indice = indice;
            this.estimulo = estimulo;
        }

        public int getIndice() {
            return indice;
        }

        public EstimuloStroop getEstimulo() {
            return estimulo;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Estimulo)) return false;
            Estimulo otro = (Estimulo) o;
            return otro.indice == indice && otro.estimulo.equals(estimulo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(indice, estimulo);
        }
    }

    /** Solo en práctica: aviso de acierto, fallo u omisión del ensayo [indice]. */
    public static final class Feedback extends FaseStroop {
        private final int indice;
        private final ResultadoEnsayoStroop resultado;

        public Feedback(int indice, ResultadoEnsayoStroop resultado) {
            this.indice = indice;
            this.resultado = resultado;
        }

        public int getIndice() {
            return indice;
        }

        public ResultadoEnsayoStroop getResultado() {
            return resultado;
        }
    }

    /** Todos los ensayos hechos. */
    public static final class Terminada extends FaseStroop {
        public static final Terminada INSTANCIA = new Terminada();

        private Terminada() {
        }
    }
}
