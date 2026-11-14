package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.ejemplo.CargadorDatosEjemplo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ResumenSesion
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Sintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ReglasJson
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ProtocolosJson
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.ConfigCognitiva
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Los datos de ejemplo de assets/ cargan bien y producen los casos de la demo. */
@RunWith(AndroidJUnit4::class)
class DatosEjemploTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: CognitivoRepository
    private lateinit var alertas: AlertasRepository
    private lateinit var fisiologico: FisiologicoRepository
    private val dao get() = db.cognitivoDao()

    private val zona = ZoneId.of("Europe/Madrid")
    // 14 de noviembre de 2026, 11:00 en Madrid.
    private val ahora = Instant.parse("2026-11-14T10:00:00Z").toEpochMilli()
    private val hoy = "2026-11-14"

    private val maya = 1L
    private val leo = 2L
    private val ane = 3L
    private val ilan = 4L

    @Before
    fun cargar() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        repo = CognitivoRepository(db, ConfigCognitiva())
        // Las mismas reglas que la app (assets/reglas.json) y el retraso de 18 min.
        alertas = AlertasRepository(db, ReglasJson.leer(contexto), retrasoMisionMs = ConfigCognitiva().retrasoMisionMs)
        val json = contexto.assets.open(CargadorDatosEjemplo.ARCHIVO).bufferedReader().use { it.readText() }
        fisiologico = FisiologicoRepository(
            db,
            ProtocolosJson.leer(contexto).motor(),
            InterpretePruebas(ConfigUmbrales.leer(contexto)),
            retrasoMisionMs = ConfigCognitiva().retrasoMisionMs,
        )
        assertTrue(CargadorDatosEjemplo(db, repo, alertas, fisiologico).cargarSiVacia(json, ahora, zona))
    }

    // ---- Alertas por reglas ----

    private suspend fun reglasActivas(id: Long) =
        db.alertasDao().deAstronauta(id).filter { it.status == "ACTIVE" }.map { it.ruleId }.toSet()

    @Test
    fun alertas_mayaFatigaYTendenciaNBack() = runBlocking {
        assertEquals(setOf("fatiga-cognitiva-combinada", "nback-tendencia"), reglasActivas(maya))
        val fatiga = db.alertasDao().deAstronauta(maya).single { it.ruleId == "fatiga-cognitiva-combinada" }
        assertEquals("HOY", fatiga.priority)
        assertEquals("NEURO", fatiga.source)
        assertEquals("REDUCIR_CARGA_TRABAJO,DORMIR_8_HORAS,REPETIR_PRUEBAS_MANANA", fatiga.steps)
        // Generada al terminar su última sesión (hace 5 min): llega con ella, dentro de 13 min.
        assertEquals(ahora - 5 * 60_000L + 18 * 60_000L, fatiga.arrivesEarthAt)
        // "Por qué ha saltado": la sesión de N-Back, las 3 noches y su base de sueño.
        val porQue = AlertasRepository.leerExplicacion(fatiga.explanation)!!
        assertEquals("NBACK", porQue.sesiones.single().prueba)
        assertEquals(listOf("2026-11-14", "2026-11-13", "2026-11-12"), porQue.noches.map { it.fecha })
        assertEquals(7.5, porQue.suenoHabitual!!, 1e-9)
        assertTrue(AlertasRepository.FUENTE_REGISTRO in porQue.fuentes)
    }

    @Test
    fun alertas_leoConcentracionYSintomas_asignadaALaOficialMedico() = runBlocking {
        assertEquals(setOf("concentracion-sintomas"), reglasActivas(leo))
        val alerta = db.alertasDao().deAstronauta(leo).single()
        assertEquals("AHORA", alerta.priority)
        assertEquals("CRITICAL", alerta.severity)
        assertEquals(ane, alerta.assignedAstronautId) // Ane es la oficial médico de a bordo
        assertTrue("la de Leo ya ha llegado a la Tierra", alerta.arrivesEarthAt!! <= ahora)
        val porQue = AlertasRepository.leerExplicacion(alerta.explanation)!!
        assertEquals(listOf("DOLOR_CABEZA"), porQue.sintomas)
        assertEquals("ALERTA", porQue.sesiones.single().estado)
        // A Ane le aparece en su panel como asignada.
        val deAne = db.alertasDao().activasPara(ane).first()
        assertEquals("Leo Martín", deAne.single().nombreAstronauta)
    }

    @Test
    fun alertas_aneEIlanSinAlertas_yNoSeRepiten() = runBlocking {
        assertTrue(reglasActivas(ane).isEmpty())
        assertTrue(reglasActivas(ilan).isEmpty())
        // Volver a evaluar no duplica las alertas activas.
        assertTrue(alertas.evaluar(maya, ahora, zona).isEmpty())
        assertEquals(2, db.alertasDao().deAstronauta(maya).size)
    }

    // ---- Protocolos fisiológicos de la demo ----

    @Test
    fun fisiologico_leoTrombosisConLaAnaliticaPendiente() = runBlocking {
        val p = db.fisiologicoDao().abiertosAhora(leo).single()
        assertEquals("trombosis-yugular", p.protocoloId)
        assertEquals("p1-flujo-alterado", p.nodoActual)
        assertEquals(3, p.nivel)
        assertEquals(EstadoProtocolo.ESPERANDO_PRUEBA, p.estado)
        val pruebas = db.fisiologicoDao().pruebasDeProtocolo(p.id)
        // El eco doppler (retrógrado) ya está hecho, por Ane; la analítica, pendiente.
        val doppler = pruebas.single { it.tipo == TipoPruebaClinica.ECO_DOPPLER_YUGULAR }
        assertEquals(ane, doppler.realizadaPor)
        assertTrue(doppler.resultado!!.contains("RETROGRADO"))
        assertEquals(
            setOf(TipoPruebaClinica.DIMERO_D, TipoPruebaClinica.HEMOGRAMA, TipoPruebaClinica.COAGULACION),
            pruebas.filter { it.realizadaEn == null }.map { it.tipo }.toSet(),
        )
        // Historial: 1→2 (edema), 2→3 (cefalea).
        assertEquals(listOf(2, 3), db.fisiologicoDao().eventosDe(p.id).first().map { it.nivelDespues })
        assertEquals(listOf("p1-cefalea", "p1-doppler", "p1-resultado-doppler", "p1-flujo-alterado"), p.pasosRecorridos)
    }

    @Test
    fun fisiologico_mayaHemodinamicoRepitiendoMediciones() = runBlocking {
        val p = db.fisiologicoDao().abiertosAhora(maya).single()
        assertEquals("hemodinamico", p.protocoloId)
        assertEquals("p2a-repetir", p.nodoActual)
        assertEquals(3, p.nivel)
        assertEquals(TipoPruebaClinica.REPETICION_CONSTANTES, db.fisiologicoDao().pruebasDeProtocolo(p.id).single().tipo)
        assertTrue(db.fisiologicoDao().balancesDesde(maya, "2000-01-01").all { it.balanceMl < 0 })
        assertTrue(db.fisiologicoDao().abiertosAhora(ane).isEmpty())
    }

    @Test
    fun fisiologico_alIntroducirLaAnaliticaElProtocoloSigue() = runBlocking {
        val p = db.fisiologicoDao().abiertosAhora(leo).single()
        val pendientes = db.fisiologicoDao().pruebasDeProtocolo(p.id).filter { it.realizadaEn == null }
        // Dímero D elevado: con eso basta para subir a nivel 4, pero el nodo espera las tres.
        val dimero = pendientes.single { it.tipo == TipoPruebaClinica.DIMERO_D }
        val r = fisiologico.guardarResultado(dimero.id, InterpretePruebas.resultadoCerrado("DIMERO_D", "ELEVADO"), ane, ahora)
        assertEquals("p1-flujo-alterado", r!!.nodo)
        for (otra in pendientes.filter { it.id != dimero.id }) {
            fisiologico.guardarResultado(otra.id, InterpretePruebas.resultadoCerrado(otra.tipo.name, "NORMAL"), ane, ahora)
        }
        val despues = db.fisiologicoDao().protocolo(p.id)!!
        assertEquals("p1-empeora", despues.nodoActual)
        assertEquals(4, despues.nivel)
        assertEquals("ANALITICA_ALTERADA", db.fisiologicoDao().eventosDe(p.id).first().last().motivo)
    }

    @After
    fun cerrar() = db.close()

    /** Los 14 resúmenes de misión (sin la línea base), del más antiguo al más reciente. */
    private suspend fun mision(id: Long, tipo: TipoPrueba): List<ResumenSesion> =
        dao.ultimosResumenes(id, tipo, 30).first().take(14).reversed()

    @Test
    fun cargaTripulacionRegistrosYSesiones() = runBlocking {
        assertEquals(4, db.tripulacionDao().astronautas().first().size)
        for (id in listOf(maya, leo, ane, ilan)) {
            assertEquals(14, db.tripulacionDao().registrosDesde(id, "2000-01-01").first().size)
            for (tipo in TipoPrueba.entries) {
                assertEquals("astronauta $id, $tipo", 17, dao.ultimosResumenes(id, tipo, 30).first().size)
                assertTrue(repo.haHechoPractica(id, tipo))
                val base = dao.lineaBaseDe(id, tipo, CognitivoRepository.metricaPrincipal(tipo))
                assertEquals(3, base!!.nSesiones)
            }
        }
        // Una segunda carga no duplica nada.
        assertFalse(CargadorDatosEjemplo(db, repo).cargarSiVacia("{}", ahora, zona))
    }

    @Test
    fun tripulacionEnLasTablasDelEquipo() = runBlocking {
        // astronaut + mission: nombre completo, cargo, sueño habitual y día 1 de misión.
        val m = db.tripulacionDao().astronauta(maya).first()!!
        assertEquals("Maya Okafor", m.nombre)
        assertEquals(CargoTripulante.COMANDANTE, m.cargo)
        assertEquals(7.5, m.horasSuenoHabituales!!, 1e-9)
        // Hoy (14 de noviembre) es el día 14: la misión empezó el 1 de noviembre.
        assertEquals(
            LocalDate.of(2026, 11, 1).atStartOfDay(zona).toInstant().toEpochMilli(),
            m.inicioMision,
        )
        assertEquals(CargoTripulante.OFICIAL_MEDICO, db.tripulacionDao().astronauta(ane).first()!!.cargo)

        // Un usuario con rol ASTRONAUT por tripulante (app_user).
        val usuarios = db.query("SELECT username, role FROM app_user ORDER BY username", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) }
        }
        assertEquals(4, usuarios.size)
        assertTrue(usuarios.all { it.second == "ASTRONAUT" })
        assertTrue("leo.martin" in usuarios.map { it.first })
    }

    @Test
    fun maya_nbackEmpeoraLosUltimosDiasYDuermePoco() = runBlocking {
        val nback = mision(maya, TipoPrueba.NBACK)
        assertTrue(nback.take(10).all { it.estado == "NORMAL" })
        nback.takeLast(3).forEach { assertTrue(it.estado, it.estado == "VIGILAR" || it.estado == "ALERTA") }
        assertTrue(mision(maya, TipoPrueba.STROOP).all { it.estado == "NORMAL" })

        val registros = db.tripulacionDao().registrosDesde(maya, "2000-01-01").first()
        assertTrue(registros.take(3).all { it.horasSueno!! < 7.5 })
    }

    @Test
    fun maya_suUltimaSesionSigueDeCamino() = runBlocking {
        val deCamino = dao.resumenesEnCamino(maya, ahora).first()
        assertEquals(1, deCamino.size)
        assertEquals(TipoPrueba.NBACK, deCamino.single().tipo)
        // El médico ve 16 de las 17 sesiones de N-Back de Maya.
        assertEquals(16, dao.resumenesRecibidos(maya, TipoPrueba.NBACK, ahora, 30).first().size)
    }

    @Test
    fun leo_stroopEnAlertaHoyConDolorDeCabeza() = runBlocking {
        val stroopHoy = mision(leo, TipoPrueba.STROOP).last()
        assertEquals("ALERTA", stroopHoy.estado)
        assertEquals(hoy, Instant.ofEpochMilli(stroopHoy.fechaEn).atZone(zona).toLocalDate().toString())
        assertTrue(stroopHoy.llegaTierraEn <= ahora)
        assertTrue(mision(leo, TipoPrueba.STROOP).dropLast(1).all { it.estado == "NORMAL" })
        assertTrue(Sintoma.DOLOR_CABEZA in db.tripulacionDao().registroDe(leo, hoy)!!.listaSintomas)
    }

    @Test
    fun aneEIlan_dentroDeSuRango() = runBlocking {
        for (id in listOf(ane, ilan)) {
            for (tipo in TipoPrueba.entries) {
                mision(id, tipo).forEach { assertEquals("astronauta $id, $tipo", "NORMAL", it.estado) }
            }
        }
    }
}
