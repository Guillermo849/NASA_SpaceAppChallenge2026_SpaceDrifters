package com.example.nasa_spaceappchallenge2026_spacedrifters.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AlertDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AlertThresholdDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AppUserDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AstronautDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.BloodPressureDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ClinicalParameterDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ClinicalResultDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ClinicalTestDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ContinuousMeasurementDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.EcgRecordingDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.HourlySummaryDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.MissionDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertThresholdEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AppUserEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AstronautEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.BloodPressureEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ClinicalParameterEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ClinicalResultEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ClinicalTestEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ContinuousMeasurementEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.EcgRecordingEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.HourlySummaryEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.MissionEntity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Única base de datos de la app (astronauta y médico comparten los mismos datos).
 * Escrita en Kotlin para que el editor de Android Studio resuelva RoomDatabase desde el código
 * Kotlin (con la clase en Java la marcaba en rojo, aunque compilara). Entidades y DAOs del equipo
 * siguen en Java (entity/, dao/); desde Java se usa igual: AppDatabase.get(context), AppDatabase.io.
 *
 * Pruebas cognitivas (Stroop, N-Back): tablas sesion_prueba, ensayo_stroop, ensayo_nback,
 * resumen_sesion y linea_base_cognitiva (DatosCognitivos.kt, CognitivoDao). Guardan cada ensayo,
 * el retraso con la Tierra (llegaTierraEn), la línea base y el estado frente a ella.
 * Registro diario (sueño, somnolencia, síntomas): registro_diario (DatosTripulacion.kt).
 * Fisiológico (protocolos de decisión): protocolo_activo, evento_protocolo, prueba_clinica y
 * balance_hidrico (DatosFisiologicos.kt, FisiologicoDao).
 * Esas tablas usan astronautaId = astronaut.id.
 *
 * Prototipo: no hay migraciones. Si cambias una entidad, sube `version` y Room borra y
 * recrea la base de datos al arrancar (los datos de ejemplo se vuelven a cargar).
 */
@Database(
    entities = [
        AppUserEntity::class,
        MissionEntity::class,
        AstronautEntity::class,
        ContinuousMeasurementEntity::class,
        EcgRecordingEntity::class,
        HourlySummaryEntity::class,
        BloodPressureEntity::class,
        ClinicalParameterEntity::class,
        ClinicalTestEntity::class,
        ClinicalResultEntity::class,
        AlertThresholdEntity::class,
        AlertEntity::class,
        // Pruebas cognitivas
        SesionPrueba::class,
        EnsayoStroop::class,
        EnsayoNBack::class,
        ResumenSesion::class,
        LineaBaseCognitiva::class,
        // Registro diario
        RegistroDiario::class,
        // Médico: pruebas extra y notas
        PruebaProgramada::class,
        NotaMedica::class,
        // Fisiológico: protocolos, su historial, pruebas a bordo y balance hídrico
        ProtocoloActivo::class,
        EventoProtocolo::class,
        PruebaClinica::class,
        BalanceHidrico::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appUserDao(): AppUserDao
    abstract fun missionDao(): MissionDao
    abstract fun astronautDao(): AstronautDao
    abstract fun continuousMeasurementDao(): ContinuousMeasurementDao
    abstract fun ecgRecordingDao(): EcgRecordingDao
    abstract fun hourlySummaryDao(): HourlySummaryDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun clinicalParameterDao(): ClinicalParameterDao
    abstract fun clinicalTestDao(): ClinicalTestDao
    abstract fun clinicalResultDao(): ClinicalResultDao
    abstract fun alertThresholdDao(): AlertThresholdDao
    abstract fun alertDao(): AlertDao
    abstract fun cognitivoDao(): CognitivoDao
    abstract fun tripulacionDao(): TripulacionDao
    abstract fun alertasDao(): AlertasDao
    abstract fun medicoDao(): MedicoDao
    abstract fun fisiologicoDao(): FisiologicoDao

    companion object {
        /** Hilos para escribir en la base de datos fuera del hilo principal (código Java del equipo). */
        @JvmField
        val io: ExecutorService = Executors.newFixedThreadPool(2)

        @Volatile
        private var instancia: AppDatabase? = null

        @JvmStatic
        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "astronaut_health.db")
                    // Solo desarrollo: si cambia la versión del esquema, borra y recrea la base de datos.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
