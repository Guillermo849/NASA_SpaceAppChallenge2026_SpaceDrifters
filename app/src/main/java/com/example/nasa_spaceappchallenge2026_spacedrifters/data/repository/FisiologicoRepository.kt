package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import androidx.room.withTransaction
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EventoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ProtocoloActivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.HemogramasNasa
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Cambio
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.MotorProtocolos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.NivelGravedad
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Nodo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.ResultadoProtocolo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/** Una emergencia sin atender de un astronauta (puede venir de varios protocolos a la vez). */
data class Emergencia(
    val astronautaId: Long,
    val nombre: String?,
    /** Signos de alarma o datos que la provocaron, sin repetir: "DOLOR_TORACICO", "SINCOPE"... */
    val motivos: List<String>,
    val desde: Long?,
    /** Cuándo le llega el aviso al médico en la Tierra. */
    val llegaTierraEn: Long?,
)

/** Un paso del árbol por el que ha pasado un protocolo (el último es el actual). */
data class PasoProtocolo(
    val nodo: String,
    val tipo: Nodo.Tipo?,
    val nivel: Int?,
    val actual: Boolean,
)

/** Un protocolo abierto con lo que necesita la oficial médico: pasos, acciones y pruebas pendientes. */
data class ProtocoloEnCurso(
    val protocolo: ProtocoloActivo,
    val nombre: String?,
    val pasos: List<PasoProtocolo>,
    /** Acciones vigentes (códigos de assets/protocolos.json). */
    val acciones: List<String>,
    val pendientes: List<PruebaClinica>,
)

/** Lo que necesita el formulario de resultado de una prueba. */
data class PruebaParaResultado(
    val prueba: PruebaClinica,
    val nombre: String?,
    /** Opciones cerradas; vacía para el ecocardio (VS y GC en números). */
    val opciones: List<String>,
    /** Para un hemograma: las muestras reales de NASA LSDS-7 de su tripulante, en orden; si no, vacía. */
    val muestrasNasa: List<HemogramasNasa.Muestra> = emptyList(),
)

/**
 * Protocolos fisiológicos: guarda lo que decide [MotorProtocolos] (nodo, nivel, historial y pruebas
 * pendientes) en Room. Los resultados de las pruebas se traducen a hechos con [InterpretePruebas].
 */
class FisiologicoRepository(
    private val db: AppDatabase,
    private val motor: MotorProtocolos,
    private val interprete: InterpretePruebas,
    private val retrasoMisionMs: Long,
    private val esperarDatos: suspend () -> Unit = {},
    /** Hemogramas reales de NASA que corresponden a un astronauta de la demo (vacío si no hay). */
    private val hemogramasNasa: (Long) -> List<HemogramasNasa.Muestra> = { emptyList() },
) {
    private val dao get() = db.fisiologicoDao()

    /** Signos de alarma que se comprueban antes que cualquier protocolo (assets/protocolos.json). */
    val signosAlarma: List<String> get() = motor.signosAlarma

    // ---- Lectura ----

    /** Emergencias sin atender de toda la tripulación, la más reciente primero. */
    fun emergencias(): Flow<List<Emergencia>> = dao.emergencias().map { filas ->
        filas.groupBy { it.astronautaId }.map { (id, deEste) ->
            Emergencia(
                astronautaId = id,
                nombre = deEste.first().nombre,
                motivos = deEste.flatMap { it.motivo.orEmpty().split('+') }.filter { it.isNotBlank() }.distinct(),
                desde = deEste.mapNotNull { it.desde }.minOrNull(),
                llegaTierraEn = deEste.mapNotNull { it.llegaTierraEn }.minOrNull(),
            )
        }
    }

    /** Protocolos abiertos de toda la tripulación (sin las emergencias directas), el más grave primero. */
    fun enCurso(): Flow<List<ProtocoloEnCurso>> = combine(
        dao.abiertos(),
        dao.pendientes(),
        db.tripulacionDao().astronautas(),
    ) { protocolos, pendientes, tripulacion ->
        val nombres = tripulacion.associate { it.id to it.nombre }
        protocolos.filter { it.protocoloId != PROTOCOLO_EMERGENCIA }.map { p ->
            ProtocoloEnCurso(
                protocolo = p,
                nombre = nombres[p.astronautaId],
                pasos = pasos(p),
                acciones = acciones(p),
                pendientes = pendientes.filter { it.protocoloActivoId == p.id },
            )
        }
    }

    suspend fun pruebaParaResultado(pruebaId: Long): PruebaParaResultado? {
        esperarDatos()
        val prueba = dao.prueba(pruebaId) ?: return null
        val nombre = db.tripulacionDao().astronauta(prueba.astronautaId).first()?.nombre
        val muestras = if (prueba.tipo == TipoPruebaClinica.HEMOGRAMA) hemogramasNasa(prueba.astronautaId) else emptyList()
        return PruebaParaResultado(prueba, nombre, InterpretePruebas.opciones(prueba.tipo.name), muestras)
    }

    /** La oficial médico de a bordo (quien introduce los resultados), o null si no hay. */
    suspend fun oficialMedicoId(): Long? {
        esperarDatos()
        return db.tripulacionDao().astronautas().first().firstOrNull { it.cargo == CargoTripulante.OFICIAL_MEDICO }?.id
    }

    // ---- Protocolos ----

    /**
     * Datos nuevos de un astronauta (síntomas, constantes frente a su base...): sigue sus protocolos
     * abiertos y abre los que se activen. Las pruebas que pidan quedan pendientes.
     */
    suspend fun evaluar(astronautaId: Long, nuevos: Hechos, ahora: Long = System.currentTimeMillis()): List<ResultadoProtocolo> {
        esperarDatos()
        return db.withTransaction {
            val abiertos = dao.abiertosAhora(astronautaId)
            motor.protocolos.mapNotNull { protocolo ->
                val abierto = abiertos.firstOrNull { it.protocoloId == protocolo.id }
                if (abierto != null) {
                    avanzarYGuardar(abierto, nuevos, ahora)
                } else {
                    val r = motor.iniciar(protocolo.id, nuevos) ?: return@mapNotNull null
                    val id = dao.insertarProtocolo(
                        ProtocoloActivo(
                            astronautaId = astronautaId,
                            protocoloId = protocolo.id,
                            nodoActual = r.nodo,
                            nivel = r.nivel,
                            iniciadoEn = ahora,
                            actualizadoEn = ahora,
                            estado = r.estado,
                            nivelPropuesto = r.nivelPropuesto,
                            hechos = aJson(nuevos),
                            recorrido = r.recorrido.joinToString(","),
                        ),
                    )
                    dao.insertarEventos(r.cambios.map { evento(id, it, ahora) })
                    pedirPruebas(id, astronautaId, r.pruebasPendientes, ahora)
                    r
                }
            }
        }
    }

    /**
     * La oficial médico introduce el resultado de una prueba: queda hecha y el protocolo que la pidió
     * sigue desde el nodo en el que esperaba.
     */
    suspend fun guardarResultado(
        pruebaId: Long,
        valores: Map<String, Any>,
        realizadaPor: Long?,
        ahora: Long = System.currentTimeMillis(),
    ): ResultadoProtocolo? {
        esperarDatos()
        return db.withTransaction {
            val prueba = requireNotNull(dao.prueba(pruebaId)) { "No existe la prueba $pruebaId" }
            dao.actualizarPrueba(
                prueba.copy(
                    realizadaEn = ahora,
                    resultado = aJson(valores),
                    realizadaPor = realizadaPor,
                    llegaTierraEn = ahora + retrasoMisionMs,
                ),
            )
            val protocolo = prueba.protocoloActivoId?.let { dao.protocolo(it) }
            if (protocolo == null || protocolo.estado == EstadoProtocolo.CERRADO) null
            else avanzarYGuardar(protocolo, Hechos(), ahora)
        }
    }

    private suspend fun avanzarYGuardar(p: ProtocoloActivo, nuevos: Hechos, ahora: Long): ResultadoProtocolo {
        val guardados = deJson(p.hechos).con(nuevos)
        val todos = guardados.copia().con(hechosDePruebas(p))
        val r = motor.avanzar(p.protocoloId, p.nodoActual, p.nivel, todos)
        // El recorrido empieza en el nodo actual, que ya estaba guardado.
        val recorrido = p.pasosRecorridos + r.recorrido.drop(if (p.pasosRecorridos.lastOrNull() == r.recorrido.firstOrNull()) 1 else 0)
        dao.actualizarProtocolo(
            p.copy(
                nodoActual = r.nodo,
                nivel = r.nivel,
                estado = r.estado,
                nivelPropuesto = r.nivelPropuesto,
                actualizadoEn = ahora,
                hechos = aJson(guardados),
                recorrido = recorrido.joinToString(","),
            ),
        )
        dao.insertarEventos(r.cambios.map { evento(p.id, it, ahora) })
        pedirPruebas(p.id, p.astronautaId, r.pruebasPendientes, ahora)
        return r
    }

    /** Hechos de las pruebas ya hechas dentro de este protocolo (comparadas con el historial). */
    private suspend fun hechosDePruebas(p: ProtocoloActivo): Hechos {
        fun aResultado(pr: PruebaClinica) = InterpretePruebas.Resultado(
            pr.tipo.name,
            pr.resultado?.let { deJson(it).comoMapa() } ?: emptyMap(),
            pr.realizadaEn ?: 0L,
        )
        val delProtocolo = dao.pruebasDeProtocolo(p.id).filter { it.realizadaEn != null }.map(::aResultado)
        val historial = dao.realizadasDe(p.astronautaId).map(::aResultado)
        return interprete.hechos(delProtocolo, historial)
    }

    /** Crea las pruebas que pide el protocolo, si no están ya pendientes. */
    private suspend fun pedirPruebas(protocoloActivoId: Long, astronautaId: Long, tipos: List<String>, ahora: Long) {
        if (tipos.isEmpty()) return
        val yaPendientes = dao.pruebasDeProtocolo(protocoloActivoId).filter { it.realizadaEn == null }.map { it.tipo.name }.toSet()
        for (tipo in tipos) {
            if (tipo in yaPendientes) continue
            dao.insertarPrueba(
                PruebaClinica(
                    astronautaId = astronautaId,
                    protocoloActivoId = protocoloActivoId,
                    tipo = TipoPruebaClinica.valueOf(tipo),
                    pedidaEn = ahora,
                ),
            )
        }
    }

    private fun pasos(p: ProtocoloActivo): List<PasoProtocolo> {
        val nodos = runCatching { motor.protocolo(p.protocoloId).nodos }.getOrNull().orEmpty()
        val recorrido = p.pasosRecorridos.ifEmpty { listOf(p.nodoActual) }
        return recorrido.mapIndexed { i, id ->
            val n = nodos[id]
            PasoProtocolo(
                nodo = id,
                tipo = n?.tipo,
                nivel = n?.nivel ?: if (id == MotorProtocolos.NODO_EMERGENCIA) NIVEL_5 else null,
                actual = i == recorrido.lastIndex,
            )
        }
    }

    /** Acciones vigentes: las del último nodo del recorrido que tiene. */
    private fun acciones(p: ProtocoloActivo): List<String> {
        if (p.nodoActual == MotorProtocolos.NODO_EMERGENCIA) return listOf(MotorProtocolos.ACCION_EMERGENCIA)
        val nodos = runCatching { motor.protocolo(p.protocoloId).nodos }.getOrNull() ?: return emptyList()
        return p.pasosRecorridos.asReversed().firstNotNullOfOrNull { id -> nodos[id]?.acciones?.takeIf { it.isNotEmpty() } }.orEmpty()
    }

    // ---- Emergencias ----

    /**
     * El astronauta tiene uno o varios signos de alarma: todos sus protocolos abiertos pasan a
     * nivel 5 (el motor mira los signos de alarma antes que nada). Si no tenía ninguno abierto, se
     * abre uno de emergencia directa. No espera a la Tierra: el aviso sale con la máxima prioridad.
     */
    suspend fun activarEmergencia(astronautaId: Long, signos: Set<String>, ahora: Long = System.currentTimeMillis()) {
        require(signos.isNotEmpty()) { "Falta el signo de alarma" }
        esperarDatos()
        val hechos = Hechos().apply { signos.forEach { con(it, true) } }
        require(motor.signosAlarmaPresentes(hechos).isNotEmpty()) { "No es un signo de alarma: $signos" }

        db.withTransaction {
            val abiertos = dao.abiertosAhora(astronautaId)
            for (p in abiertos) {
                if (p.protocoloId == PROTOCOLO_EMERGENCIA) {
                    // Ya estaba en emergencia directa: se añade el signo nuevo al historial.
                    dao.insertarEventos(listOf(evento(p.id, Cambio(MotorProtocolos.NODO_EMERGENCIA, p.nivel, NIVEL_5, signos.joinToString("+"), null), ahora)))
                    dao.actualizarProtocolo(p.copy(actualizadoEn = ahora))
                    continue
                }
                avanzarYGuardar(p, hechos, ahora)
            }
            if (abiertos.isEmpty()) {
                val id = dao.insertarProtocolo(
                    ProtocoloActivo(
                        astronautaId = astronautaId,
                        protocoloId = PROTOCOLO_EMERGENCIA,
                        nodoActual = MotorProtocolos.NODO_EMERGENCIA,
                        nivel = NIVEL_5,
                        iniciadoEn = ahora,
                        actualizadoEn = ahora,
                        estado = EstadoProtocolo.ABIERTO,
                        recorrido = MotorProtocolos.NODO_EMERGENCIA,
                    ),
                )
                val motivo = motor.signosAlarmaPresentes(hechos).joinToString("+")
                dao.insertarEventos(
                    listOf(evento(id, Cambio(MotorProtocolos.NODO_EMERGENCIA, MotorProtocolos.NIVEL_INICIAL, NIVEL_5, motivo, null), ahora)),
                )
            }
        }
    }

    /**
     * La tripulación ha atendido la emergencia de un astronauta: sus protocolos en emergencia se
     * cierran y queda en el historial quién lo ha confirmado. Bajar de nivel 5 siempre es manual.
     */
    suspend fun marcarAtendida(astronautaId: Long, confirmadoPor: String, ahora: Long = System.currentTimeMillis()) {
        db.withTransaction {
            for (p in dao.abiertosAhora(astronautaId).filter { it.nodoActual == MotorProtocolos.NODO_EMERGENCIA }) {
                val cambio = MotorProtocolos.confirmarNivel(p.nodoActual, p.nivel, p.nivel, MOTIVO_ATENDIDA, confirmadoPor)
                dao.insertarEventos(listOf(evento(p.id, cambio, ahora)))
                dao.actualizarProtocolo(p.copy(estado = EstadoProtocolo.CERRADO, nivelPropuesto = null, actualizadoEn = ahora))
            }
        }
    }

    private fun evento(protocoloActivoId: Long, c: Cambio, ahora: Long) = EventoProtocolo(
        protocoloActivoId = protocoloActivoId,
        fechaHora = ahora,
        nodo = c.nodo,
        nivelAntes = c.nivelAntes,
        nivelDespues = c.nivelDespues,
        motivo = c.motivo,
        confirmadoPor = c.confirmadoPor,
        llegaTierraEn = ahora + retrasoMisionMs,
    )

    companion object {
        /** protocoloId de una emergencia sin protocolo previo (botón "Tengo un síntoma de alarma"). */
        const val PROTOCOLO_EMERGENCIA = "emergencia"
        const val MOTIVO_ATENDIDA = "EMERGENCIA_ATENDIDA"
        /** Quién confirma desde la pantalla de emergencia (todavía no hay inicio de sesión). */
        const val CONFIRMA_TRIPULACION = "TRIPULACION"
        private val NIVEL_5 = NivelGravedad.EMERGENCIA.numero

        // ---- Hechos y resultados <-> JSON (solo booleanos, números y textos) ----

        fun aJson(hechos: Hechos): String = aJson(hechos.comoMapa())

        fun aJson(valores: Map<String, Any?>): String = JsonObject(
            valores.mapNotNull { (k, v) ->
                val e: JsonElement = when (v) {
                    is Boolean -> JsonPrimitive(v)
                    is Number -> JsonPrimitive(v)
                    null -> return@mapNotNull null
                    else -> JsonPrimitive(v.toString())
                }
                k to e
            }.toMap(),
        ).toString()

        fun deJson(texto: String): Hechos {
            val h = Hechos()
            val objeto = runCatching { Json.parseToJsonElement(texto).jsonObject }.getOrNull() ?: return h
            for ((k, v) in objeto) {
                val p = v as? JsonPrimitive ?: continue
                val valor: Any = when {
                    p.isString -> p.content
                    p.booleanOrNull != null -> p.booleanOrNull!!
                    p.doubleOrNull != null -> p.doubleOrNull!!
                    else -> p.content
                }
                h.con(k, valor)
            }
            return h
        }
    }
}
