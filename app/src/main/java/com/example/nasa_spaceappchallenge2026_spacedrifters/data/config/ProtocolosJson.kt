package com.example.nasa_spaceappchallenge2026_spacedrifters.data.config

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Condicion
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.DetectorAmbiental
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.MotorProtocolos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Nodo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Protocolo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Formato de assets/protocolos.json: signos de alarma comunes, los protocolos en árbol (1–4) y el
 * protocolo ambiental (5). Los protocolos se cambian ahí, no en el código.
 */
@Serializable
data class ProtocolosJson(
    val signosAlarma: List<String>,
    val protocolos: List<ProtocoloJson>,
    val ambiental: AmbientalJson,
) {
    fun motor() = MotorProtocolos(protocolos.map { it.aProtocolo() }, signosAlarma)

    fun detectorAmbiental() = DetectorAmbiental(ambiental.aConfig())

    companion object {
        const val ARCHIVO = "protocolos.json"
        private val json = Json { ignoreUnknownKeys = true }

        @JvmStatic
        fun desdeTexto(texto: String): ProtocolosJson = json.decodeFromString(texto)

        fun leer(context: Context): ProtocolosJson =
            context.assets.open(ARCHIVO).bufferedReader().use { desdeTexto(it.readText()) }
    }
}

@Serializable
data class ProtocoloJson(
    val id: String,
    val activacion: CondicionJson,
    val emergencia: CondicionJson? = null,
    val inicio: String,
    val nodos: List<NodoJson>,
) {
    fun aProtocolo() = Protocolo(id, activacion.aCondicion(), emergencia?.aCondicion(), inicio, nodos.map { it.aNodo() })
}

@Serializable
data class NodoJson(
    val id: String,
    val tipo: Nodo.Tipo,
    val condicion: CondicionJson? = null,
    val si: String? = null,
    val no: String? = null,
    val pruebas: List<String> = emptyList(),
    val siguiente: String? = null,
    val ramas: List<RamaJson> = emptyList(),
    val otro: String? = null,
    val nivel: Int? = null,
    val acciones: List<String> = emptyList(),
    val cerrar: Boolean = false,
) {
    fun aNodo() = Nodo(
        id, tipo, condicion?.aCondicion(), si, no, pruebas, siguiente,
        ramas.map { Nodo.Rama(it.condicion.aCondicion(), it.siguiente) }, otro, nivel, acciones, cerrar,
    )
}

@Serializable
data class RamaJson(val condicion: CondicionJson, val siguiente: String)

/** Ver la documentación de [Condicion]: hecho (+ es / mayorQue / menorQue), todos, alguno o no. */
@Serializable
data class CondicionJson(
    val hecho: String? = null,
    val es: List<String> = emptyList(),
    val mayorQue: Double? = null,
    val menorQue: Double? = null,
    val todos: List<CondicionJson>? = null,
    val alguno: List<CondicionJson>? = null,
    val no: CondicionJson? = null,
) {
    fun aCondicion(): Condicion = when {
        todos != null -> Condicion.todos(todos.map { it.aCondicion() })
        alguno != null -> Condicion.alguno(alguno.map { it.aCondicion() })
        no != null -> Condicion.no(no.aCondicion())
        hecho != null -> Condicion.hecho(hecho, es, mayorQue, menorQue)
        else -> throw IllegalArgumentException("Condición vacía en protocolos.json")
    }
}

@Serializable
data class AmbientalJson(
    val id: String,
    val ventanaHoras: Long,
    val minAstronautas: Int,
    val nivel: Int,
    val alteraciones: List<String>,
    val acciones: List<String>,
) {
    fun aConfig() = DetectorAmbiental.Config(
        id, ventanaHoras * 3_600_000L, minAstronautas, nivel, alteraciones.toSet(), acciones,
    )
}
