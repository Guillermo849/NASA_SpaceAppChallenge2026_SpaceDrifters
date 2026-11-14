package com.example.nasa_spaceappchallenge2026_spacedrifters.di

import android.content.Context
import android.util.Log
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigMision
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ReglasJson
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.BienestarRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.ejemplo.CargadorDatosEjemplo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.MedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ProtocolosJson
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoMedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.UmbralesFisiologicos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.AnaliticasNasa
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.TripulacionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Inyección de dependencias manual (sin Hilt).
 *
 * Aquí se crean, una sola vez, la base de datos, los repositorios y cualquier otro
 * objeto compartido. Los ViewModels los obtienen a través de [AppViewModelProvider].
 *
 * Ejemplo de cómo añadir algo nuevo:
 * ```
 * val tripulacionRepository by lazy { TripulacionRepository(database.astronautaDao()) }
 * ```
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    /**
     * Ámbito de toda la app, para trabajos que tienen que terminar aunque se cierre la pantalla
     * (p. ej., guardar una prueba interrumpida al salir de la app).
     */
    val ambitoAplicacion: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }

    val userDao get() = database.appUserDao()

    /** assets/config_mision.json: retraso con la Tierra, umbrales de estado... */
    val configMision: ConfigMision by lazy { ConfigMision.leer(appContext) }

    val cognitivoRepository: CognitivoRepository by lazy {
        CognitivoRepository(database, configMision.aConfigCognitiva(), esperarDatos = { datosEjemplo.await() })
    }

    val tripulacionRepository: TripulacionRepository by lazy {
        TripulacionRepository(database, esperarDatos = { datosEjemplo.await() })
    }

    /** Bienestar psicológico: check-in diario y herramientas usadas. */
    val bienestarRepository: BienestarRepository by lazy {
        BienestarRepository(database, esperarDatos = { datosEjemplo.await() })
    }

    /** assets/reglas.json: reglas de alerta (tendencias, sueño, síntomas...). */
    val reglas: List<Regla> by lazy { ReglasJson.leer(appContext) }

    val alertasRepository: AlertasRepository by lazy {
        AlertasRepository(
            database,
            reglas,
            retrasoMisionMs = configMision.aConfigCognitiva().retrasoMisionMs,
            esperarDatos = { datosEjemplo.await() },
        )
    }

    val medicoRepository: MedicoRepository by lazy {
        MedicoRepository(database, cognitivoRepository, esperarDatos = { datosEjemplo.await() })
    }

    /** assets/protocolos.json: protocolos fisiológicos en árbol, signos de alarma y protocolo ambiental. */
    val protocolos: ProtocolosJson by lazy { ProtocolosJson.leer(appContext) }

    /** assets/config_umbrales.json: cuándo FC, PA, SpO2, FR, VS y GC se consideran alterados. */
    val umbrales: UmbralesFisiologicos by lazy { ConfigUmbrales.leer(appContext) }

    /** Pestaña "Fisiológico" del médico: protocolos, historial, pruebas y constantes recibidas. */
    val fisiologicoMedicoRepository: FisiologicoMedicoRepository by lazy {
        FisiologicoMedicoRepository(
            database,
            retrasoMisionMs = configMision.aConfigCognitiva().retrasoMisionMs,
            esperarDatos = { datosEjemplo.await() },
        )
    }

    /**
     * Analíticas reales de NASA (OSDR, misión Inspiration4): hemogramas (LSDS-7) y bioquímicas
     * (LSDS-8). Si un archivo falta o no se puede leer, la app sigue sin él (se introduce a mano).
     */
    val hemogramasNasa: List<AnaliticasNasa.Muestra> by lazy { leerAnaliticaNasa(ARCHIVO_HEMOGRAMAS_NASA) }
    val bioquimicasNasa: List<AnaliticasNasa.Muestra> by lazy { leerAnaliticaNasa(ARCHIVO_BIOQUIMICAS_NASA) }

    private fun leerAnaliticaNasa(archivo: String): List<AnaliticasNasa.Muestra> = runCatching {
        AnaliticasNasa.leer(
            appContext.assets.open(archivo).bufferedReader().use { it.readText() },
            configMision.parametrosNasaExcluidos,
        )
    }.onFailure { Log.e("AnaliticasNasa", "No se ha podido leer $archivo", it) }
        .getOrDefault(emptyList())

    /**
     * Las muestras reales (hemograma o bioquímica) del tripulante NASA que corresponde a un
     * astronauta de la demo (config_mision.json, "tripulanteNasa"); vacía para otras pruebas.
     */
    fun analiticasNasaDe(tipo: TipoPruebaClinica, astronautaId: Long): List<AnaliticasNasa.Muestra> {
        val todas = when (tipo) {
            TipoPruebaClinica.HEMOGRAMA -> hemogramasNasa
            TipoPruebaClinica.BIOQUIMICA -> bioquimicasNasa
            else -> return emptyList()
        }
        return configMision.tripulanteNasa[astronautaId.toString()]?.let { AnaliticasNasa.de(todas, it) }.orEmpty()
    }

    val fisiologicoRepository: FisiologicoRepository by lazy {
        FisiologicoRepository(
            database,
            protocolos.motor(),
            InterpretePruebas(umbrales),
            retrasoMisionMs = configMision.aConfigCognitiva().retrasoMisionMs,
            esperarDatos = { datosEjemplo.await() },
            analiticasNasa = ::analiticasNasaDe,
        )
    }

    /**
     * Cargador de assets/datos_ejemplo.json. Su repositorio fisiológico no espera a [datosEjemplo]
     * (si no, la carga se esperaría a sí misma).
     */
    fun cargadorDatosEjemplo() = CargadorDatosEjemplo(
        database,
        cognitivoRepository,
        alertasRepository,
        FisiologicoRepository(
            database,
            protocolos.motor(),
            InterpretePruebas(umbrales),
            retrasoMisionMs = configMision.aConfigCognitiva().retrasoMisionMs,
        ),
    )

    /**
     * Carga de assets/datos_ejemplo.json si la base de datos está vacía. Se lanza al arrancar
     * ([iniciar]); nunca falla hacia fuera (si algo va mal, se registra y la app sigue sin datos).
     */
    val datosEjemplo: Deferred<Unit> = ambitoAplicacion.async(start = CoroutineStart.LAZY) {
        // Archivo de la base de datos anterior (antes de unificarla en AppDatabase): ya no se usa.
        appContext.deleteDatabase(BASE_DATOS_ANTIGUA)
        runCatching {
            val json = appContext.assets.open(CargadorDatosEjemplo.ARCHIVO).bufferedReader().use { it.readText() }
            cargadorDatosEjemplo().cargarSiVacia(json)
        }.onFailure { Log.e("DatosEjemplo", "No se han podido cargar los datos de ejemplo", it) }
        Unit
    }

    /**
     * Astronauta que usa la app: lo fija el inicio de sesión (el astronaut enlazado a su app_user).
     * Por defecto, Maya Okafor (id 1 en los datos de ejemplo). Los ViewModels lo leen al crearse,
     * es decir, después de entrar.
     */
    @Volatile
    var astronautaActualId: Long = 1L

    /** Arranca lo que tiene que estar listo cuanto antes. Lo llama la Application. */
    fun iniciar() {
        datosEjemplo.start()
    }

    private companion object {
        const val BASE_DATOS_ANTIGUA = "spacedrifters.db"
        const val ARCHIVO_HEMOGRAMAS_NASA = "LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv"
        const val ARCHIVO_BIOQUIMICAS_NASA = "LSDS-8_Comprehensive_Metabolic_Panel_CMP_TRANSFORMED.csv"
    }
}
