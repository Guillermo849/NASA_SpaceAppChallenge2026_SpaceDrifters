package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas;

import androidx.annotation.Nullable;

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Lo que el motor necesita saber de un astronauta para evaluar las reglas. */
public final class ContextoReglas {

    /** Una sesión válida de misión (sin práctica, sin línea base, no interrumpida) ya evaluada. */
    public static final class Sesion {
        private final long sesionId;
        private final String prueba;
        private final LocalDate fecha;
        private final EstadoCognitivo estado;
        private final String metrica;
        @Nullable private final Double valor;

        public Sesion(long sesionId, String prueba, LocalDate fecha, EstadoCognitivo estado,
                      String metrica, @Nullable Double valor) {
            this.sesionId = sesionId;
            this.prueba = Objects.requireNonNull(prueba);
            this.fecha = Objects.requireNonNull(fecha);
            this.estado = Objects.requireNonNull(estado);
            this.metrica = Objects.requireNonNull(metrica);
            this.valor = valor;
        }

        public long getSesionId() { return sesionId; }
        public String getPrueba() { return prueba; }
        public LocalDate getFecha() { return fecha; }
        public EstadoCognitivo getEstado() { return estado; }
        public String getMetrica() { return metrica; }
        @Nullable public Double getValor() { return valor; }
    }

    /** Horas dormidas una noche (registro diario de ese día). null = sin datos. */
    public static final class Noche {
        private final LocalDate fecha;
        @Nullable private final Double horas;

        public Noche(LocalDate fecha, @Nullable Double horas) {
            this.fecha = Objects.requireNonNull(fecha);
            this.horas = horas;
        }

        public LocalDate getFecha() { return fecha; }
        @Nullable public Double getHoras() { return horas; }
    }

    private final LocalDate hoy;
    private final List<Sesion> sesiones;
    private final List<Noche> noches;
    @Nullable private final Double suenoHabitual;
    private final Set<String> sintomasHoy;

    /**
     * @param sesiones de todas las pruebas, de la más reciente a la más antigua
     * @param noches registros de sueño (en cualquier orden)
     * @param sintomasHoy síntomas del registro diario de hoy, por nombre
     */
    public ContextoReglas(LocalDate hoy, List<Sesion> sesiones, List<Noche> noches,
                          @Nullable Double suenoHabitual, Set<String> sintomasHoy) {
        this.hoy = Objects.requireNonNull(hoy);
        this.sesiones = Collections.unmodifiableList(sesiones);
        this.noches = Collections.unmodifiableList(noches);
        this.suenoHabitual = suenoHabitual;
        this.sintomasHoy = Collections.unmodifiableSet(new LinkedHashSet<>(sintomasHoy));
    }

    public LocalDate getHoy() { return hoy; }
    public List<Sesion> getSesiones() { return sesiones; }
    public List<Noche> getNoches() { return noches; }
    @Nullable public Double getSuenoHabitual() { return suenoHabitual; }
    public Set<String> getSintomasHoy() { return sintomasHoy; }
}
