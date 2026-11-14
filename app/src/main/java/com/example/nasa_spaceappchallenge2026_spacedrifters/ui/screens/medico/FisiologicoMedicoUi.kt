package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EventoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.MedidaFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ProtocoloActivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TensionFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.UmbralesFisiologicos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.MeasurementType
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.NivelGravedad

/** Constantes de la pestaña, en este orden. */
enum class MetricaConstante(val clave: String) { FC("FC"), PAS("PAS"), SPO2("SPO2"), FR("FR") }

/** Una constante recibida en la Tierra, con su línea base y la franja que marcan los umbrales. */
data class SerieConstanteUi(
    val metrica: MetricaConstante,
    /** (instante, valor), del más antiguo al más reciente. */
    val puntos: List<Pair<Long, Double>>,
    val base: Double?,
    /** {bajo, alto} alrededor de la base, o null. */
    val franja: DoubleArray?,
) {
    val ultimo: Double? get() = puntos.lastOrNull()?.second
    fun fuera(v: Double): Boolean = franja?.let { v < it[0] || v > it[1] } ?: false
}

/** Un elemento de la línea de tiempo de un protocolo. */
sealed interface ItemTiempoUi {
    val instante: Long

    /** Cambio (o confirmación) de nivel. */
    data class CambioNivel(
        override val instante: Long,
        val nodo: String,
        val nivelAntes: Int,
        val nivelDespues: Int,
        val motivo: String,
        val confirmadoPor: String?,
    ) : ItemTiempoUi

    data class PruebaPedida(override val instante: Long, val tipo: TipoPruebaClinica) : ItemTiempoUi

    data class PruebaHecha(override val instante: Long, val prueba: PruebaClinica) : ItemTiempoUi
}

/** Un protocolo tal y como lo ve el médico (solo lo que ya ha llegado a la Tierra). */
data class ProtocoloMedicoUi(
    val protocolo: ProtocoloActivo,
    /** Nivel según lo recibido (puede ir por detrás del de a bordo). */
    val nivel: Int,
    val nivelPropuesto: Int?,
    val cerrado: Boolean,
    val enEmergencia: Boolean,
    val tiempo: List<ItemTiempoUi>,
    /** Eventos y resultados que todavía vienen de camino. */
    val enCamino: Int,
    /** Pruebas pedidas sin resultado (a bordo o ya hecho pero de camino). */
    val pendientes: List<PruebaClinica>,
)

/** Una prueba con su resultado anterior, para comparar (eco doppler, ecocardio). */
data class ComparacionUi(val tipo: TipoPruebaClinica, val ultima: PruebaClinica, val anterior: PruebaClinica?)

data class FisiologicoMedicoUi(
    val cargando: Boolean = true,
    /** Nivel más alto de sus protocolos abiertos (lo recibido); null = ninguno abierto. */
    val nivelActual: Int? = null,
    val protocolos: List<ProtocoloMedicoUi> = emptyList(),
    val series: List<SerieConstanteUi> = emptyList(),
    val comparaciones: List<ComparacionUi> = emptyList(),
)

/** Días que forman la línea base de las constantes (los primeros de la serie). */
const val DIAS_LINEA_BASE_CONSTANTES = UmbralesFisiologicos.DIAS_LINEA_BASE

/**
 * Construye la pestaña "Fisiológico" con lo que ya ha llegado a la Tierra en [ahora]: eventos con
 * llegaTierraEn ≤ ahora, resultados recibidos, constantes con instante + retraso ≤ ahora. Las
 * acciones del médico se guardan con llegaTierraEn = su hora, así que se ven al momento.
 */
fun construirFisiologico(
    protocolos: List<ProtocoloActivo>,
    eventos: List<EventoProtocolo>,
    pruebas: List<PruebaClinica>,
    medidas: List<MedidaFila>,
    tensiones: List<TensionFila>,
    umbrales: UmbralesFisiologicos,
    retrasoMs: Long,
    ahora: Long,
): FisiologicoMedicoUi {
    val llegado = { instante: Long -> instante + retrasoMs <= ahora }

    val visibles = protocolos.mapNotNull { p ->
            val suyos = eventos.filter { it.protocoloActivoId == p.id }
            val recibidos = suyos.filter { it.llegaTierraEn <= ahora }
            val susPruebas = pruebas.filter { it.protocoloActivoId == p.id }
            // Un protocolo aparece cuando llega su primer evento (o su apertura).
            if (recibidos.isEmpty() && !llegado(p.iniciadoEn)) return@mapNotNull null
            val nivel = recibidos.lastOrNull()?.nivelDespues ?: NivelGravedad.INFORMATIVO.numero
            val tiempo = buildList {
                recibidos.forEach { add(ItemTiempoUi.CambioNivel(it.fechaHora, it.nodo, it.nivelAntes, it.nivelDespues, it.motivo, it.confirmadoPor)) }
                susPruebas.filter { llegado(it.pedidaEn) }.forEach { add(ItemTiempoUi.PruebaPedida(it.pedidaEn, it.tipo)) }
                susPruebas.filter { it.realizadaEn != null && (it.llegaTierraEn ?: Long.MAX_VALUE) <= ahora }
                    .forEach { add(ItemTiempoUi.PruebaHecha(it.realizadaEn!!, it)) }
            }.sortedBy { it.instante }
            val resultadosEnCamino = susPruebas.count { it.realizadaEn != null && (it.llegaTierraEn ?: Long.MAX_VALUE) > ahora }
            ProtocoloMedicoUi(
                protocolo = p,
                nivel = nivel,
                // La bajada propuesta solo si ya ha llegado el evento que la provocó.
                nivelPropuesto = p.nivelPropuesto?.takeIf { suyos.size == recibidos.size },
                cerrado = p.estado == EstadoProtocolo.CERRADO && suyos.size == recibidos.size,
                enEmergencia = nivel >= NivelGravedad.EMERGENCIA.numero,
                tiempo = tiempo,
                enCamino = (suyos.size - recibidos.size) + resultadosEnCamino,
                pendientes = susPruebas.filter { it.realizadaEn == null || (it.llegaTierraEn ?: Long.MAX_VALUE) > ahora },
            )
        }

    val series = buildList {
        fun serie(m: MetricaConstante, puntos: List<Pair<Long, Double>>) {
            val recibidos = puntos.filter { llegado(it.first) }.sortedBy { it.first }
            val base = UmbralesFisiologicos.lineaBase(
                recibidos.map { it.first }.toLongArray(),
                recibidos.map { it.second }.toDoubleArray(),
                DIAS_LINEA_BASE_CONSTANTES * 86_400_000L,
            )
            add(SerieConstanteUi(m, recibidos, base, base?.let { umbrales.franja(m.clave, it) }))
        }
        serie(MetricaConstante.FC, medidas.filter { it.metrica == MeasurementType.HR.name }.map { it.instante to it.valor })
        serie(MetricaConstante.PAS, tensiones.mapNotNull { t -> t.sistolica?.let { t.instante to it.toDouble() } })
        serie(MetricaConstante.SPO2, medidas.filter { it.metrica == MeasurementType.SPO2.name }.map { it.instante to it.valor })
        serie(MetricaConstante.FR, medidas.filter { it.metrica == MeasurementType.RR.name }.map { it.instante to it.valor })
    }

    val comparaciones = listOf(TipoPruebaClinica.ECO_DOPPLER_YUGULAR, TipoPruebaClinica.ECOCARDIO).mapNotNull { tipo ->
        val hechas = pruebas.filter { it.tipo == tipo && it.realizadaEn != null && (it.llegaTierraEn ?: Long.MAX_VALUE) <= ahora }
            .sortedByDescending { it.realizadaEn }
        hechas.firstOrNull()?.let { ComparacionUi(tipo, it, hechas.getOrNull(1)) }
    }

    val ordenados = visibles.sortedWith(compareBy<ProtocoloMedicoUi> { it.cerrado }.thenByDescending { it.nivel })
    return FisiologicoMedicoUi(
        cargando = false,
        nivelActual = ordenados.filter { !it.cerrado }.maxOfOrNull { it.nivel },
        protocolos = ordenados,
        series = series,
        comparaciones = comparaciones,
    )
}
