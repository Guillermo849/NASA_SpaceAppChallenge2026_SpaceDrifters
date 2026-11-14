package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback;

import androidx.annotation.Nullable;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Temporizador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lógica y tiempos de una sesión N-Back, sin nada de Android, para poder probarla en la JVM.
 *
 * Cada ensayo dura una ventana fija (2500 ms) desde que la forma se dibuja: la forma se ve los
 * primeros 1000 ms y el resto es pantalla vacía. El toque en "Coincide" vale durante toda la
 * ventana y solo cuenta el primero. El ritmo no cambia aunque se pulse.
 * En práctica, al cerrar cada ensayo se muestra un aviso si hubo acierto, falsa alarma u omisión.
 *
 * Los instantes ({@link #estimuloMostrado}, {@link #pulsar}) son nanosegundos de un reloj
 * monótono, el mismo para los dos (ver ControladorStroop).
 * No es seguro entre hilos: todo va por el hilo principal.
 */
public final class ControladorNBack {

    private static final long NANOS_POR_MS = 1_000_000L;

    private final SecuenciaNBack secuencia;
    private final boolean esPractica;
    private final ConfigNBack config;
    private final Temporizador temporizador;
    private final Consumer<FaseNBack> alCambiarFase;

    private final List<ResultadoEnsayoNBack> resultados = new ArrayList<>();
    private final List<Temporizador.Cancelable> pendientes = new ArrayList<>();
    private FaseNBack fase = FaseNBack.Preparada.INSTANCIA;
    private boolean empezado = false;
    private boolean detenido = false;
    /** Índice del ensayo cuya ventana está abierta; -1 si ninguno. */
    private int indiceAbierto = -1;
    @Nullable private Long inicioNanos = null;
    @Nullable private Long pulsoNanos = null;

    public ControladorNBack(SecuenciaNBack secuencia, boolean esPractica, ConfigNBack config,
                            Temporizador temporizador, Consumer<FaseNBack> alCambiarFase) {
        this.secuencia = Objects.requireNonNull(secuencia);
        this.esPractica = esPractica;
        this.config = Objects.requireNonNull(config);
        this.temporizador = Objects.requireNonNull(temporizador);
        this.alCambiarFase = Objects.requireNonNull(alCambiarFase);
    }

    public int getTotalEstimulos() {
        return secuencia.size();
    }

    public int getNivel() {
        return secuencia.getNivel();
    }

    public boolean esPractica() {
        return esPractica;
    }

    public FaseNBack getFase() {
        return fase;
    }

    public List<ResultadoEnsayoNBack> getResultados() {
        return Collections.unmodifiableList(new ArrayList<>(resultados));
    }

    /** Arranca la sesión tras una breve pausa. Solo se puede llamar una vez. */
    public void empezar() {
        if (empezado || detenido) throw new IllegalStateException("La sesión ya se ha empezado");
        empezado = true;
        if (secuencia.size() == 0) {
            cambiarFase(FaseNBack.Terminada.INSTANCIA);
            return;
        }
        programar(config.pausaInicialMs, () -> mostrarEstimulo(0));
    }

    /** Para la sesión (interrupción). A partir de aquí se ignora todo. */
    public void detener() {
        detenido = true;
        for (Temporizador.Cancelable p : pendientes) p.cancelar();
        pendientes.clear();
    }

    /** La pantalla avisa de que la forma [indice] se ha dibujado en el instante dado. */
    public void estimuloMostrado(int indice, long instanteNanos) {
        if (detenido || inicioNanos != null) return;
        if (!(fase instanceof FaseNBack.Estimulo) || ((FaseNBack.Estimulo) fase).getIndice() != indice) return;
        inicioNanos = instanteNanos;
        indiceAbierto = indice;
        // La forma se ve un tiempo fijo y la ventana se cierra a los 2500 ms, contados desde el dibujo.
        programar(config.estimuloMs, () -> ocultar(indice));
        programar(config.ventanaMs, () -> cerrarEnsayo(indice));
    }

    /**
     * Toque en "Coincide". Se ignora si no hay ventana abierta, si es anterior a que se dibujara
     * la forma o posterior al final de la ventana, o si ya se pulsó en este ensayo.
     */
    public void pulsar(long instanteNanos) {
        if (detenido || indiceAbierto < 0 || inicioNanos == null || pulsoNanos != null) return;
        long transcurrido = instanteNanos - inicioNanos;
        if (transcurrido < 0 || transcurrido > config.ventanaMs * NANOS_POR_MS) return;
        pulsoNanos = instanteNanos;
    }

    private void mostrarEstimulo(int indice) {
        if (detenido) return;
        inicioNanos = null;
        pulsoNanos = null;
        cambiarFase(new FaseNBack.Estimulo(indice, secuencia.get(indice)));
    }

    private void ocultar(int indice) {
        if (detenido) return;
        if (fase instanceof FaseNBack.Estimulo && ((FaseNBack.Estimulo) fase).getIndice() == indice) {
            cambiarFase(new FaseNBack.Vacio(indice));
        }
    }

    private void cerrarEnsayo(int indice) {
        if (detenido || indiceAbierto != indice || inicioNanos == null) return;
        Long tiempoMs = pulsoNanos == null ? null : (pulsoNanos - inicioNanos) / NANOS_POR_MS;
        ResultadoEnsayoNBack resultado = new ResultadoEnsayoNBack(
                indice + 1, secuencia.get(indice), secuencia.esCoincidencia(indice), tiempoMs);
        resultados.add(resultado);
        indiceAbierto = -1;
        inicioNanos = null;
        pulsoNanos = null;

        if (esPractica && resultado.getTipo() != ResultadoEnsayoNBack.Tipo.RECHAZO_CORRECTO) {
            cambiarFase(new FaseNBack.Feedback(indice, resultado));
            programar(config.feedbackMs, () -> siguiente(indice));
        } else {
            siguiente(indice);
        }
    }

    private void siguiente(int indiceActual) {
        if (detenido) return;
        pendientes.clear();
        if (indiceActual + 1 < secuencia.size()) {
            mostrarEstimulo(indiceActual + 1);
        } else {
            cambiarFase(FaseNBack.Terminada.INSTANCIA);
        }
    }

    private void programar(long retrasoMs, Runnable accion) {
        pendientes.add(temporizador.programar(retrasoMs, accion));
    }

    private void cambiarFase(FaseNBack nueva) {
        fase = nueva;
        alCambiarFase.accept(nueva);
    }
}
