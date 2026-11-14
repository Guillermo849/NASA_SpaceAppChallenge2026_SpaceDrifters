package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop;

import androidx.annotation.Nullable;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Temporizador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lógica y tiempos de una sesión Stroop, sin nada de Android, para poder probarla en la JVM.
 *
 * Secuencia de cada ensayo: cruz de fijación → estímulo → respuesta u omisión
 * (→ aviso de acierto/fallo, solo en práctica) → siguiente ensayo.
 *
 * Los instantes que recibe ({@link #estimuloMostrado}, {@link #responder}) son nanosegundos
 * de un reloj monótono. En la app ambos salen de CLOCK_MONOTONIC: el frame en que se dibuja el
 * estímulo y el evento táctil. Tienen que ser del mismo reloj para poder restarse.
 *
 * No es seguro entre hilos: todas las llamadas (y las acciones del temporizador) van por el
 * hilo principal.
 */
public final class ControladorStroop {

    private static final long NANOS_POR_MS = 1_000_000L;

    private final List<EstimuloStroop> secuencia;
    private final boolean esPractica;
    private final ConfigStroop config;
    private final Temporizador temporizador;
    private final Consumer<FaseStroop> alCambiarFase;

    private final List<ResultadoEnsayoStroop> resultados = new ArrayList<>();
    private FaseStroop fase = FaseStroop.Preparada.INSTANCIA;
    private boolean detenido = false;
    /** Instante en que se dibujó el estímulo actual; null mientras no se haya dibujado. */
    @Nullable private Long inicioNanos = null;
    @Nullable private Temporizador.Cancelable pendiente = null;

    public ControladorStroop(List<EstimuloStroop> secuencia, boolean esPractica, ConfigStroop config,
                             Temporizador temporizador, Consumer<FaseStroop> alCambiarFase) {
        this.secuencia = Collections.unmodifiableList(new ArrayList<>(secuencia));
        this.esPractica = esPractica;
        this.config = Objects.requireNonNull(config);
        this.temporizador = Objects.requireNonNull(temporizador);
        this.alCambiarFase = Objects.requireNonNull(alCambiarFase);
    }

    public int getTotalEnsayos() {
        return secuencia.size();
    }

    public boolean esPractica() {
        return esPractica;
    }

    public FaseStroop getFase() {
        return fase;
    }

    public List<ResultadoEnsayoStroop> getResultados() {
        return Collections.unmodifiableList(new ArrayList<>(resultados));
    }

    /** Arranca la sesión. Solo se puede llamar una vez. */
    public void empezar() {
        if (!(fase instanceof FaseStroop.Preparada) || detenido) {
            throw new IllegalStateException("La sesión ya se ha empezado");
        }
        if (secuencia.isEmpty()) {
            cambiarFase(FaseStroop.Terminada.INSTANCIA);
        } else {
            mostrarFijacion(0);
        }
    }

    /** Para la sesión (interrupción). A partir de aquí se ignora todo. */
    public void detener() {
        detenido = true;
        cancelarPendiente();
    }

    /** La pantalla avisa de que el estímulo [indice] se ha dibujado en el instante dado. */
    public void estimuloMostrado(int indice, long instanteNanos) {
        if (detenido || inicioNanos != null) return;
        if (!(fase instanceof FaseStroop.Estimulo) || ((FaseStroop.Estimulo) fase).getIndice() != indice) return;
        inicioNanos = instanteNanos;
        // El límite de respuesta cuenta desde que el estímulo está de verdad en pantalla.
        pendiente = temporizador.programar(config.limiteRespuestaMs, () -> cerrarEnsayo(null, 0L));
    }

    /**
     * Toque en un botón de color. Se ignora si no hay estímulo dibujado, si el toque es anterior
     * a que se dibujara, o si ya hubo respuesta en este ensayo (solo cuenta la primera).
     */
    public void responder(ColorStroop color, long instanteNanos) {
        if (detenido || inicioNanos == null || !(fase instanceof FaseStroop.Estimulo)) return;
        if (instanteNanos < inicioNanos) return;
        cancelarPendiente();
        cerrarEnsayo(color, instanteNanos);
    }

    private void mostrarFijacion(int indice) {
        cambiarFase(new FaseStroop.Fijacion(indice));
        pendiente = temporizador.programar(config.fijacionMs, () -> mostrarEstimulo(indice));
    }

    private void mostrarEstimulo(int indice) {
        if (detenido) return;
        inicioNanos = null;
        cambiarFase(new FaseStroop.Estimulo(indice, secuencia.get(indice)));
    }

    /** Cierra el ensayo actual con una respuesta, o como omisión si color es null. */
    private void cerrarEnsayo(@Nullable ColorStroop color, long instanteNanos) {
        if (detenido || inicioNanos == null) return;
        int indice = ((FaseStroop.Estimulo) fase).getIndice();
        Long tiempoMs = color == null ? null : (instanteNanos - inicioNanos) / NANOS_POR_MS;
        inicioNanos = null;
        ResultadoEnsayoStroop resultado =
                new ResultadoEnsayoStroop(indice + 1, secuencia.get(indice), color, tiempoMs);
        resultados.add(resultado);

        if (esPractica) {
            cambiarFase(new FaseStroop.Feedback(indice, resultado));
            pendiente = temporizador.programar(config.feedbackMs, () -> siguiente(indice));
        } else {
            siguiente(indice);
        }
    }

    private void siguiente(int indiceActual) {
        if (detenido) return;
        if (indiceActual + 1 < secuencia.size()) {
            mostrarFijacion(indiceActual + 1);
        } else {
            pendiente = null;
            cambiarFase(FaseStroop.Terminada.INSTANCIA);
        }
    }

    private void cambiarFase(FaseStroop nueva) {
        fase = nueva;
        alCambiarFase.accept(nueva);
    }

    private void cancelarPendiente() {
        if (pendiente != null) {
            pendiente.cancelar();
            pendiente = null;
        }
    }
}
