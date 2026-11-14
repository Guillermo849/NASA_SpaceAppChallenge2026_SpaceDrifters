package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo;

/**
 * Configuración de la misión para el módulo cognitivo. En la app se lee de
 * assets/config_mision.json; los valores de aquí son solo los de por defecto (y los de los tests).
 */
public final class ConfigCognitiva {

    /** Retraso de las comunicaciones con la Tierra: llegaTierraEn = inicioEn + retraso. */
    public final long retrasoMisionMs;
    /** A partir de cuántas desviaciones (en la dirección mala) una sesión pasa a VIGILAR. */
    public final double umbralVigilar;
    /** A partir de cuántas desviaciones (en la dirección mala) una sesión pasa a ALERTA. */
    public final double umbralAlerta;
    /** Sesiones de línea base necesarias para calcularla. */
    public final int minSesionesLineaBase;

    public ConfigCognitiva() {
        this(18L * 60_000L, 1.0, 2.0, 3);
    }

    public ConfigCognitiva(long retrasoMisionMs, double umbralVigilar, double umbralAlerta, int minSesionesLineaBase) {
        if (umbralVigilar <= 0 || umbralAlerta < umbralVigilar) {
            throw new IllegalArgumentException("Umbrales incoherentes: vigilar " + umbralVigilar + ", alerta " + umbralAlerta);
        }
        if (minSesionesLineaBase < 2) {
            throw new IllegalArgumentException("Hacen falta al menos 2 sesiones para una desviación típica");
        }
        this.retrasoMisionMs = retrasoMisionMs;
        this.umbralVigilar = umbralVigilar;
        this.umbralAlerta = umbralAlerta;
        this.minSesionesLineaBase = minSesionesLineaBase;
    }
}
