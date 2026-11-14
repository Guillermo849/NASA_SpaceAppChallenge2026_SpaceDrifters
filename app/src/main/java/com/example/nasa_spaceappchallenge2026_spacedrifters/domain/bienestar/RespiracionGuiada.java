package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar;

/**
 * Respiración en caja (box breathing): inspirar, mantener, espirar y mantener, 4 s cada fase.
 * Sin temporizador propio: la pantalla pregunta el estado para el tiempo transcurrido.
 */
public final class RespiracionGuiada {

    public enum Fase { INSPIRA, MANTEN_LLENO, ESPIRA, MANTEN_VACIO }

    public static final long FASE_MS = 4_000L;
    public static final long CICLO_MS = FASE_MS * Fase.values().length;

    /** Tamaño del círculo (fracción del máximo) con los pulmones vacíos. */
    public static final float TAMANO_MIN = 0.45f;

    /** Lo que se pinta en un instante de la sesión. */
    public static final class Estado {
        private final Fase fase;
        private final int segundosRestantesFase;
        private final float tamano;
        private final int ciclo;
        private final boolean terminado;

        Estado(Fase fase, int segundosRestantesFase, float tamano, int ciclo, boolean terminado) {
            this.fase = fase;
            this.segundosRestantesFase = segundosRestantesFase;
            this.tamano = tamano;
            this.ciclo = ciclo;
            this.terminado = terminado;
        }

        public Fase getFase() { return fase; }
        /** Cuenta atrás de la fase: 4, 3, 2, 1. */
        public int getSegundosRestantesFase() { return segundosRestantesFase; }
        /** Tamaño del círculo, de [TAMANO_MIN] a 1: crece al inspirar y encoge al espirar. */
        public float getTamano() { return tamano; }
        /** Ciclo en curso, desde 1. */
        public int getCiclo() { return ciclo; }
        public boolean isTerminado() { return terminado; }
    }

    private final int ciclos;

    /** @param ciclos ciclos completos de la sesión (4 ciclos ≈ 1 minuto) */
    public RespiracionGuiada(int ciclos) {
        if (ciclos < 1) throw new IllegalArgumentException("ciclos");
        this.ciclos = ciclos;
    }

    /** Ciclos completos que caben en [minutos]. */
    public static int ciclosEnMinutos(int minutos) {
        return (int) Math.max(1, minutos * 60_000L / CICLO_MS);
    }

    public int getCiclos() { return ciclos; }

    public long getDuracionMs() { return ciclos * CICLO_MS; }

    public Estado estado(long transcurridoMs) {
        long t = Math.max(0, transcurridoMs);
        if (t >= getDuracionMs()) {
            return new Estado(Fase.MANTEN_VACIO, 0, TAMANO_MIN, ciclos, true);
        }
        int ciclo = (int) (t / CICLO_MS) + 1;
        long enCiclo = t % CICLO_MS;
        Fase fase = Fase.values()[(int) (enCiclo / FASE_MS)];
        long enFase = enCiclo % FASE_MS;
        float progreso = enFase / (float) FASE_MS;
        int restantes = (int) ((FASE_MS - enFase + 999) / 1000);
        float tamano;
        switch (fase) {
            case INSPIRA: tamano = TAMANO_MIN + (1 - TAMANO_MIN) * progreso; break;
            case MANTEN_LLENO: tamano = 1f; break;
            case ESPIRA: tamano = 1 - (1 - TAMANO_MIN) * progreso; break;
            default: tamano = TAMANO_MIN;
        }
        return new Estado(fase, restantes, tamano, ciclo, false);
    }
}
