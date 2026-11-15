package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt;

import androidx.annotation.Nullable;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Temporizador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Lógica y tiempos de una sesión PVT, sin nada de Android.
 *
 * Protocolo:
 *   ISI aleatorio (isiMinMs – isiMaxMs) → aparece el contador (ms)
 *   → el astronauta pulsa (TR) o agota el tiempo (lapso)
 *   → (práctica) feedback
 *   → siguiente ensayo, hasta agotar duracionSesionMs.
 *
 * Los instantes se miden en nanosegundos de reloj monótono (System.nanoTime).
 * estimuloMostrado() se llama desde el frame de composición en que el contador
 * aparece por primera vez; responder() recibe el instante del evento táctil.
 * Ambos deben usar el mismo reloj para que la resta sea correcta.
 *
 * No es seguro entre hilos: todas las llamadas van por el hilo principal.
 */
public final class ControladorPvt {

    private static final long NANOS_POR_MS = 1_000_000L;

    private final ConfigPvt config;
    private final boolean esPractica;
    private final Random random;
    private final Temporizador temporizador;
    private final Consumer<FasePvt> alCambiarFase;

    private final List<ResultadoEnsayoPvt> resultados = new ArrayList<>();
    private FasePvt fase = FasePvt.Preparada.INSTANCIA;
    private boolean detenido = false;

    /** Instante en nanosegundos en que se dibujó el estímulo actual; null = no visible aún. */
    @Nullable private Long inicioEstimuloNanos = null;

    @Nullable private Temporizador.Cancelable pendiente = null;

    /** Instante (epoch ms) en que empezó la sesión. */
    private long inicioSesionMs = 0;

    /** Número del ensayo actual (empieza en 1). */
    private int ensayoActual = 0;

    /** true mientras estamos dentro de la ventana de respuesta al estímulo. */
    private boolean esperandoRespuesta = false;

    public ControladorPvt(ConfigPvt config, boolean esPractica, Random random,
                          Temporizador temporizador, Consumer<FasePvt> alCambiarFase) {
        this.config = Objects.requireNonNull(config);
        this.esPractica = esPractica;
        this.random = Objects.requireNonNull(random);
        this.temporizador = Objects.requireNonNull(temporizador);
        this.alCambiarFase = Objects.requireNonNull(alCambiarFase);
    }

    public boolean esPractica() { return esPractica; }

    public FasePvt getFase() { return fase; }

    public List<ResultadoEnsayoPvt> getResultados() {
        return Collections.unmodifiableList(new ArrayList<>(resultados));
    }

    /** Arranca la sesión. Solo se puede llamar una vez. */
    public void empezar() {
        if (!(fase instanceof FasePvt.Preparada) || detenido) {
            throw new IllegalStateException("La sesión ya se ha empezado");
        }
        inicioSesionMs = System.currentTimeMillis();
        iniciarIsi();
    }

    /** Para la sesión (interrupción). A partir de aquí se ignora todo. */
    public void detener() {
        detenido = true;
        cancelarPendiente();
    }

    /**
     * El marco de composición avisa de que el estímulo del ensayo [numero] se dibujó.
     * A partir de este instante empieza a contar el TR.
     */
    public void estimuloMostrado(int numero, long instanteNanos) {
        if (detenido || inicioEstimuloNanos != null || !esperandoRespuesta) return;
        if (!(fase instanceof FasePvt.Estimulo) || ((FasePvt.Estimulo) fase).getNumero() != numero) return;
        inicioEstimuloNanos = instanteNanos;
        // El límite de respuesta cuenta desde que el estímulo está en pantalla.
        cancelarPendiente();
        pendiente = temporizador.programar(config.trLapsoMs, () -> cerrarComoLapso());
    }

    /**
     * El astronauta pulsa. Se llama con el instante del evento táctil (nanos).
     * Puede ser:
     *   - Anticipación: se pulsó antes de que saliera el estímulo (esperandoRespuesta = false)
     *     o el TR es menor que trMinimoMs.
     *   - Respuesta válida: trMinimoMs ≤ TR < trLapsoMs.
     */
    public void responder(long instanteNanos) {
        if (detenido) return;

        // Pulso durante el ISI → anticipación.
        if (!esperandoRespuesta) {
            int numero = ensayoActual + 1;
            registrarAnticipacion(numero);
            return;
        }

        // Pulso antes de que el frame se haya dibujado → ignorar (no debería ocurrir).
        if (inicioEstimuloNanos == null) return;
        if (instanteNanos < inicioEstimuloNanos) return;

        cancelarPendiente();
        long trMs = (instanteNanos - inicioEstimuloNanos) / NANOS_POR_MS;

        if (trMs < config.trMinimoMs) {
            // TR demasiado corto: anticipación.
            registrarAnticipacion(((FasePvt.Estimulo) fase).getNumero());
        } else {
            // Respuesta válida.
            int numero = ((FasePvt.Estimulo) fase).getNumero();
            ResultadoEnsayoPvt resultado = new ResultadoEnsayoPvt(numero, trMs, false);
            cerrarEnsayo(resultado);
        }
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private void iniciarIsi() {
        if (detenido) return;
        // ¿Queda tiempo o se han completado los ensayos de práctica?
        if (sesionTerminada()) {
            cambiarFase(FasePvt.Terminada.INSTANCIA);
            return;
        }
        ensayoActual++;
        esperandoRespuesta = false;
        inicioEstimuloNanos = null;
        cambiarFase(new FasePvt.Isi(ensayoActual));

        long isiMs = isiAleatorio();
        pendiente = temporizador.programar(isiMs, () -> mostrarEstimulo());
    }

    private void mostrarEstimulo() {
        if (detenido) return;
        if (sesionTerminada()) {
            cambiarFase(FasePvt.Terminada.INSTANCIA);
            return;
        }
        esperandoRespuesta = true;
        // El controlador emite la fase Estimulo; el tiempo de inicio real se fijará en
        // estimuloMostrado() cuando el frame llegue a la pantalla.
        // Guardamos 0 como placeholder; la UI usará el instante real del frame.
        cambiarFase(new FasePvt.Estimulo(ensayoActual, 0L));
        // Tiempo máximo hasta lapso: se reemplaza en estimuloMostrado() para usar el instante real.
        pendiente = temporizador.programar(config.trLapsoMs + 200L, () -> {
            // Guardián por si estimuloMostrado no llegó: cerramos como lapso.
            if (esperandoRespuesta) cerrarComoLapso();
        });
    }

    private void cerrarComoLapso() {
        if (detenido || !esperandoRespuesta) return;
        int numero = (fase instanceof FasePvt.Estimulo) ? ((FasePvt.Estimulo) fase).getNumero() : ensayoActual;
        ResultadoEnsayoPvt resultado = new ResultadoEnsayoPvt(numero, null, false);
        cerrarEnsayo(resultado);
    }

    private void registrarAnticipacion(int numero) {
        ResultadoEnsayoPvt resultado = new ResultadoEnsayoPvt(numero, null, true);
        cerrarEnsayo(resultado);
    }

    private void cerrarEnsayo(ResultadoEnsayoPvt resultado) {
        if (detenido) return;
        esperandoRespuesta = false;
        inicioEstimuloNanos = null;
        resultados.add(resultado);

        if (esPractica) {
            cambiarFase(new FasePvt.Feedback(resultado));
            pendiente = temporizador.programar(config.feedbackMs, () -> {
                if (sesionTerminada()) {
                    cambiarFase(FasePvt.Terminada.INSTANCIA);
                } else {
                    iniciarIsi();
                }
            });
        } else {
            iniciarIsi();
        }
    }

    private boolean sesionTerminada() {
        if (esPractica) {
            if (config.ensayosPractica > 0 && resultados.size() >= config.ensayosPractica) {
                return true;
            }
            if (config.duracionPracticaMs > 0 && (System.currentTimeMillis() - inicioSesionMs >= config.duracionPracticaMs)) {
                return true;
            }
        }
        return System.currentTimeMillis() - inicioSesionMs >= config.duracionSesionMs;
    }

    private long isiAleatorio() {
        // En práctica usamos un intervalo más corto (1.5s - 3.5s) para familiarizarse rápido sin esperas.
        long min = esPractica ? Math.min(config.isiMinMs, 1_500L) : config.isiMinMs;
        long max = esPractica ? Math.min(config.isiMaxMs, 3_500L) : config.isiMaxMs;
        long rango = Math.max(1L, max - min);
        return min + (long) (random.nextDouble() * rango);
    }

    private void cambiarFase(FasePvt nueva) {
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
