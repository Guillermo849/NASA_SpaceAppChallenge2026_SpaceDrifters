package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores

/*
 * Textos del módulo fisiológico. assets/protocolos.json usa códigos (nodos, acciones, resultados);
 * aquí se traducen a strings.xml. Un código sin texto se muestra tal cual (nunca se pierde).
 */

@StringRes
fun nombreProtocolo(id: String): Int? = when (id) {
    "trombosis-yugular" -> R.string.protocolo_trombosis_yugular
    "hemodinamico" -> R.string.protocolo_hemodinamico
    "cognitivo-sueno" -> R.string.protocolo_cognitivo_sueno
    "cardiorrespiratorio" -> R.string.protocolo_cardiorrespiratorio
    "emergencia" -> R.string.protocolo_emergencia
    "ambiental" -> R.string.protocolo_ambiental
    else -> null
}

@StringRes
fun nombrePruebaClinica(tipo: TipoPruebaClinica): Int = when (tipo) {
    TipoPruebaClinica.ECO_DOPPLER_YUGULAR -> R.string.prueba_eco_doppler_yugular
    TipoPruebaClinica.ECOCARDIO -> R.string.prueba_ecocardio
    TipoPruebaClinica.ECG -> R.string.prueba_ecg
    TipoPruebaClinica.DIMERO_D -> R.string.prueba_dimero_d
    TipoPruebaClinica.HEMOGRAMA -> R.string.prueba_hemograma
    TipoPruebaClinica.COAGULACION -> R.string.prueba_coagulacion
    TipoPruebaClinica.REPETICION_CONSTANTES -> R.string.prueba_repeticion_constantes
    TipoPruebaClinica.BIOQUIMICA -> R.string.prueba_bioquimica
}

/** Opción cerrada de un resultado (ver InterpretePruebas.opciones). */
@StringRes
fun nombreOpcionResultado(opcion: String): Int? = when (opcion) {
    "NORMAL" -> R.string.opcion_normal
    "ESTASIS" -> R.string.opcion_estasis
    "RETROGRADO" -> R.string.opcion_retrogrado
    "TROMBO" -> R.string.opcion_trombo
    "ALTERADO" -> R.string.opcion_alterado
    "ALTERADA" -> R.string.opcion_alterada
    "ELEVADO" -> R.string.opcion_elevado
    "MEJOR" -> R.string.opcion_mejor
    "IGUAL_O_PEOR" -> R.string.opcion_igual_o_peor
    "DESHIDRATACION" -> R.string.opcion_deshidratacion
    else -> null
}

@StringRes
fun nombreNivel(nivel: Int): Int = when (nivel) {
    1 -> R.string.nivel_1
    2 -> R.string.nivel_2
    3 -> R.string.nivel_3
    4 -> R.string.nivel_4
    else -> R.string.nivel_5
}

/** Color de un nivel de gravedad; siempre va con su número y su nombre. */
@Composable
fun colorNivel(nivel: Int): Color {
    val c = LocalEstadoColores.current
    return when {
        nivel <= 1 -> c.normal
        nivel <= 3 -> c.vigilar
        else -> c.alerta
    }
}

/** "3 · Valoración" con su punto de color. */
@Composable
fun MarcaNivel(nivel: Int, modifier: Modifier = Modifier) {
    val color = colorNivel(nivel)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(
            stringResource(R.string.nivel_con_numero, nivel, stringResource(nombreNivel(nivel))),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

/** Paso (nodo) de un protocolo, en lenguaje sencillo. */
@StringRes
fun nombreNodo(id: String): Int? = when (id) {
    "EMERGENCIA" -> R.string.nodo_emergencia
    // Protocolo 1: trombosis venosa yugular
    "p1-cefalea" -> R.string.nodo_p1_cefalea
    "p1-doppler" -> R.string.nodo_p1_doppler
    "p1-resultado-doppler" -> R.string.nodo_p1_resultado_doppler
    "p1-normal" -> R.string.nodo_p1_normal
    "p1-trombo" -> R.string.nodo_p1_trombo
    "p1-flujo-alterado" -> R.string.nodo_p1_flujo_alterado
    "p1-resultado-analitica" -> R.string.nodo_p1_resultado_analitica
    "p1-empeora" -> R.string.nodo_p1_empeora
    "p1-seguimiento" -> R.string.nodo_p1_seguimiento
    // Protocolo 2: alteraciones hemodinámicas
    "p2-rama" -> R.string.nodo_p2_rama
    "p2a-sintomas" -> R.string.nodo_p2a_sintomas
    "p2a-sin-sintomas" -> R.string.nodo_p2a_sin_sintomas
    "p2a-repetir" -> R.string.nodo_p2a_repetir
    "p2a-tras-repetir" -> R.string.nodo_p2a_tras_repetir
    "p2a-deshidratacion" -> R.string.nodo_p2a_deshidratacion
    "p2a-rehidratar" -> R.string.nodo_p2a_rehidratar
    "p2a-mejora" -> R.string.nodo_p2a_mejora
    "p2a-ecocardio" -> R.string.nodo_p2a_ecocardio
    "p2a-resultado-ecocardio" -> R.string.nodo_p2a_resultado_ecocardio
    "p2a-alteracion" -> R.string.nodo_p2a_alteracion
    "p2a-seguimiento" -> R.string.nodo_p2a_seguimiento
    "p2b-contexto" -> R.string.nodo_p2b_contexto
    "p2b-esperable" -> R.string.nodo_p2b_esperable
    "p2b-tras-reposo" -> R.string.nodo_p2b_tras_reposo
    "p2b-normal" -> R.string.nodo_p2b_normal
    "p2b-ecg" -> R.string.nodo_p2b_ecg
    "p2b-resultado-ecg" -> R.string.nodo_p2b_resultado_ecg
    "p2b-ecg-alterado" -> R.string.nodo_p2b_ecg_alterado
    "p2b-seguimiento" -> R.string.nodo_p2b_seguimiento
    // Protocolo 3: deterioro cognitivo y sueño
    "p3-sueno" -> R.string.nodo_p3_sueno
    "p3-sueno-bajo" -> R.string.nodo_p3_sueno_bajo
    "p3-sueno-vigilar" -> R.string.nodo_p3_sueno_vigilar
    "p3-sueno-alerta" -> R.string.nodo_p3_sueno_alerta
    "p3-estres" -> R.string.nodo_p3_estres
    "p3-estres-alto" -> R.string.nodo_p3_estres_alto
    "p3-sin-causa" -> R.string.nodo_p3_sin_causa
    // Protocolo 4: alteraciones cardiorrespiratorias
    "p4-sintomas" -> R.string.nodo_p4_sintomas
    "p4-repetir" -> R.string.nodo_p4_repetir
    "p4-tras-repetir" -> R.string.nodo_p4_tras_repetir
    "p4-sensor" -> R.string.nodo_p4_sensor
    "p4-persistente" -> R.string.nodo_p4_persistente
    "p4-alerta" -> R.string.nodo_p4_alerta
    "p4-emergencia" -> R.string.nodo_p4_emergencia
    else -> null
}

/** Acción de un protocolo, como instrucción. */
@StringRes
fun nombreAccion(codigo: String): Int? = when (codigo) {
    "VIGILAR_EDEMA" -> R.string.accion_vigilar_edema
    "REGISTRAR_SINTOMAS" -> R.string.accion_registrar_sintomas
    "HACER_ECO_DOPPLER_YUGULAR" -> R.string.accion_hacer_eco_doppler_yugular
    "NOTIFICAR_MEDICO" -> R.string.accion_notificar_medico
    "NOTIFICAR_MEDICO_PRIORIDAD" -> R.string.accion_notificar_medico_prioridad
    "REPETIR_VIGILANCIA_48H" -> R.string.accion_repetir_vigilancia_48h
    "PROTOCOLO_TROMBOSIS" -> R.string.accion_protocolo_trombosis
    "COMPARAR_ECO_DOPPLER_ANTERIOR" -> R.string.accion_comparar_eco_doppler_anterior
    "PEDIR_ANALITICA" -> R.string.accion_pedir_analitica
    "VALORACION_MEDICO" -> R.string.accion_valoracion_medico
    "RESPONDER_SINTOMAS" -> R.string.accion_responder_sintomas
    "REPETIR_MEDICION" -> R.string.accion_repetir_medicion
    "VIGILAR_MAS_A_MENUDO" -> R.string.accion_vigilar_mas_a_menudo
    "REVISAR_BALANCE_HIDRICO_24H" -> R.string.accion_revisar_balance_hidrico_24h
    "REPETIR_MEDICION_REPOSO_5_MIN" -> R.string.accion_repetir_medicion_reposo_5_min
    "PEDIR_BIOQUIMICA" -> R.string.accion_pedir_bioquimica
    "REHIDRATACION_ORAL" -> R.string.accion_rehidratacion_oral
    "HACER_ECOCARDIO" -> R.string.accion_hacer_ecocardio
    "POSIBLE_ALTERACION_HEMODINAMICA" -> R.string.accion_posible_alteracion_hemodinamica
    "INDICAR_EJERCICIO_O_ESTRES" -> R.string.accion_indicar_ejercicio_o_estres
    "REPETIR_EN_REPOSO_15_MIN" -> R.string.accion_repetir_en_reposo_15_min
    "SEGUIMIENTO_RUTINARIO" -> R.string.accion_seguimiento_rutinario
    "HACER_ECG" -> R.string.accion_hacer_ecg
    "DESCANSAR" -> R.string.accion_descansar
    "REPETIR_PRUEBAS_COGNITIVAS" -> R.string.accion_repetir_pruebas_cognitivas
    "RESPONDER_CUESTIONARIO_ESTRES" -> R.string.accion_responder_cuestionario_estres
    "GESTIONAR_ESTRES" -> R.string.accion_gestionar_estres
    "REPETIR_MEDICION_SPO2" -> R.string.accion_repetir_medicion_spo2
    "REVISAR_SENSOR" -> R.string.accion_revisar_sensor
    "MEDIR_PA" -> R.string.accion_medir_pa
    "MEDIR_FC" -> R.string.accion_medir_fc
    "PROTOCOLO_EMERGENCIA" -> R.string.accion_protocolo_emergencia
    "REVISAR_CO2" -> R.string.accion_revisar_co2
    "REVISAR_O2" -> R.string.accion_revisar_o2
    "REVISAR_PRESION_CABINA" -> R.string.accion_revisar_presion_cabina
    "REVISAR_TEMPERATURA_CABINA" -> R.string.accion_revisar_temperatura_cabina
    "REVISAR_SOPORTE_VITAL" -> R.string.accion_revisar_soporte_vital
    else -> null
}

/** Texto de un código, o el propio código si aún no tiene traducción. */
@Composable
fun textoDe(@StringRes id: Int?, codigo: String): String = id?.let { stringResource(it) } ?: codigo

/** Dato (hecho) que hace avanzar o subir un protocolo, o motivo de un cambio hecho a mano. */
@StringRes
fun nombreHecho(codigo: String): Int? = when (codigo) {
    "EDEMA_FACIAL_CERVICAL" -> R.string.hecho_edema_facial_cervical
    "CEFALEA" -> R.string.hecho_cefalea
    "FC_ALTA" -> R.string.hecho_fc_alta
    "PAS_BAJA" -> R.string.hecho_pas_baja
    "PAS_ALTA" -> R.string.hecho_pas_alta
    "SPO2_BAJA" -> R.string.hecho_spo2_baja
    "TAQUIPNEA" -> R.string.hecho_taquipnea
    "DISNEA" -> R.string.signo_disnea
    "MAREO" -> R.string.hecho_mareo
    "FATIGA" -> R.string.hecho_fatiga
    "PRESINCOPE" -> R.string.hecho_presincope
    "ANALITICA_ALTERADA" -> R.string.hecho_analitica_alterada
    "ECO_DOPPLER_PEOR_QUE_ANTERIOR" -> R.string.hecho_eco_doppler_peor
    "REPETICION_ALTERADA" -> R.string.hecho_repeticion_alterada
    "BIOQUIMICA_ALTERADA" -> R.string.hecho_bioquimica_alterada
    "DESHIDRATACION_ANALITICA" -> R.string.hecho_deshidratacion_analitica
    "VS_BAJO" -> R.string.hecho_vs_bajo
    "GC_BAJO" -> R.string.hecho_gc_bajo
    "ECG_ALTERADO" -> R.string.hecho_ecg_alterado
    "SUENO_BAJO" -> R.string.hecho_sueno_bajo
    "NBACK_BAJO" -> R.string.hecho_nback_bajo
    "TIEMPO_REACCION_ALTO" -> R.string.hecho_tiempo_reaccion_alto
    "COGNITIVO_ALERTA" -> R.string.hecho_cognitivo_alerta
    "ESTRES_ALTO" -> R.string.hecho_estres_alto
    "EJERCICIO_ULTIMA_HORA" -> R.string.hecho_ejercicio_ultima_hora
    "ESTRES_ULTIMA_HORA" -> R.string.hecho_estres_ultima_hora
    "MAREO_INTENSO" -> R.string.signo_mareo_intenso
    "DETERIORO_COGNITIVO_BRUSCO" -> R.string.signo_deterioro_cognitivo_brusco
    "DOLOR_TORACICO" -> R.string.signo_dolor_toracico
    "SINCOPE" -> R.string.signo_sincope
    "DISNEA_GRAVE" -> R.string.signo_disnea_grave
    "ALTERACION_LENGUAJE" -> R.string.signo_alteracion_lenguaje
    "CONFIRMADO_POR_MEDICO" -> R.string.hecho_confirmado_por_medico
    "BAJADO_POR_MEDICO" -> R.string.hecho_bajado_por_medico
    "EMERGENCIA_ATENDIDA" -> R.string.hecho_emergencia_atendida
    else -> null
}

/**
 * "Dato que lo provocó" de un evento, en texto: "FC_ALTA+PAS_BAJA" → "FC alta, PA sistólica baja";
 * "ECO_DOPPLER_YUGULAR=RETROGRADO" → "Eco doppler yugular: flujo retrógrado"; "NO:CEFALEA" → "Sin: cefalea".
 */
@Composable
fun textoMotivo(motivo: String): String {
    // "NO:" va delante de toda la lista de hechos que no se cumplen.
    if (motivo.startsWith("NO:")) return stringResource(R.string.motivo_no, textoHechos(motivo.removePrefix("NO:").split('+')))
    val partes = motivo.split('+').filter { it.isNotBlank() }.map { parte ->
        when {
            '=' in parte -> {
                val (hecho, valor) = parte.split('=', limit = 2)
                val nombre = TipoPruebaClinica.entries.firstOrNull { it.name == hecho }?.let { stringResource(nombrePruebaClinica(it)) }
                    ?: textoDe(nombreHecho(hecho), hecho)
                "$nombre: " + textoDe(nombreOpcionResultado(valor), valor)
            }
            else -> TipoPruebaClinica.entries.firstOrNull { it.name == parte }?.let { stringResource(nombrePruebaClinica(it)) }
                ?: textoDe(nombreHecho(parte), parte)
        }
    }
    return partes.joinToString(", ")
}

@Composable
private fun textoHechos(codigos: List<String>): String = codigos.map { textoDe(nombreHecho(it), it) }.joinToString(", ")

/** Parámetro de una analítica de NASA: hemograma LSDS-7 o bioquímica LSDS-8 (columna sin "_value…"). */
@StringRes
fun nombreParametroAnalitica(codigo: String): Int? = when (codigo) {
    // Bioquímica (LSDS-8)
    "albumin" -> R.string.bq_p_albumina
    "albumin_to_globulin_ratio" -> R.string.bq_p_albumina_globulina
    "alkaline_phosphatase" -> R.string.bq_p_fosfatasa_alcalina
    "alt" -> R.string.bq_p_alt
    "ast" -> R.string.bq_p_ast
    "total_bilirubin" -> R.string.bq_p_bilirrubina
    "bun_to_creatinine_ratio" -> R.string.bq_p_bun_creatinina
    "calcium" -> R.string.bq_p_calcio
    "carbon_dioxide" -> R.string.bq_p_bicarbonato
    "chloride" -> R.string.bq_p_cloro
    "creatinine" -> R.string.bq_p_creatinina
    "globulin" -> R.string.bq_p_globulina
    "glucose" -> R.string.bq_p_glucosa
    "potassium" -> R.string.bq_p_potasio
    "total_protein" -> R.string.bq_p_proteinas
    "sodium" -> R.string.bq_p_sodio
    "urea_nitrogen_bun" -> R.string.bq_p_urea
    // Hemograma (LSDS-7)
    "absolute_basophils" -> R.string.hemo_p_basofilos_abs
    "absolute_eosinophils" -> R.string.hemo_p_eosinofilos_abs
    "absolute_lymphocytes" -> R.string.hemo_p_linfocitos_abs
    "absolute_monocytes" -> R.string.hemo_p_monocitos_abs
    "absolute_neutrophils" -> R.string.hemo_p_neutrofilos_abs
    "basophils" -> R.string.hemo_p_basofilos
    "eosinophils" -> R.string.hemo_p_eosinofilos
    "hematocrit" -> R.string.hemo_p_hematocrito
    "hemoglobin" -> R.string.hemo_p_hemoglobina
    "lymphocytes" -> R.string.hemo_p_linfocitos
    "mch" -> R.string.hemo_p_hcm
    "mchc" -> R.string.hemo_p_chcm
    "mcv" -> R.string.hemo_p_vcm
    "monocytes" -> R.string.hemo_p_monocitos
    "mpv" -> R.string.hemo_p_vpm
    "neutrophils" -> R.string.hemo_p_neutrofilos
    "platelet_count" -> R.string.hemo_p_plaquetas
    "rdw" -> R.string.hemo_p_ade
    "red_blood_cell_count" -> R.string.hemo_p_hematies
    "white_blood_cell_count" -> R.string.hemo_p_leucocitos
    else -> null
}

/**
 * Unidad de un parámetro de una analítica de NASA. La hemoglobina viene marcada como "percent" en el
 * archivo, pero sus valores (11–17) y su rango son g/dl. Los cocientes no llevan unidad.
 */
@StringRes
fun unidadAnalitica(codigo: String, unidad: String): Int = when {
    codigo == "hemoglobin" -> R.string.hemo_u_g_dl
    unidad == "gram_per_deciliter" -> R.string.hemo_u_g_dl
    unidad == "milligram_per_deciliter" -> R.string.bq_u_mg_dl
    unidad == "millimol_per_liter" -> R.string.bq_u_mmol_l
    unidad == "units_per_liter" -> R.string.bq_u_u_l
    unidad == "cells_per_microliter" -> R.string.hemo_u_por_ul
    unidad == "percent" -> R.string.hemo_u_porcentaje
    unidad == "picogram" -> R.string.hemo_u_pg
    unidad == "grams_per_deciliter" -> R.string.hemo_u_g_dl
    unidad == "femtoliter" -> R.string.hemo_u_fl
    unidad == "thousand_per_microliter" -> R.string.hemo_u_miles_ul
    unidad == "million_per_microliter" -> R.string.hemo_u_millones_ul
    else -> R.string.hemo_u_vacia
}
