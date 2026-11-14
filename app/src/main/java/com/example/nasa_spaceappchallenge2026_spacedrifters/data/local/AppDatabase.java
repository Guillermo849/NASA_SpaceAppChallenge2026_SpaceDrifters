package com.example.nasa_spaceappchallenge2026_spacedrifters.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AppUserDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.MissionDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AstronautDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ContinuousMeasurementDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.EcgRecordingDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.HourlySummaryDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.BloodPressureDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ClinicalParameterDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ClinicalTestDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.ClinicalResultDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AlertThresholdDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AlertDao;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AppUserEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.MissionEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AstronautEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ContinuousMeasurementEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.EcgRecordingEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.HourlySummaryEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.BloodPressureEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ClinicalParameterEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ClinicalTestEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ClinicalResultEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertThresholdEntity;
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertEntity;

/**
 * Única base de datos de la app (astronauta y médico comparten los mismos datos).
 *
 * Pruebas cognitivas (Stroop, N-Back): tablas sesion_prueba, ensayo_stroop, ensayo_nback,
 * resumen_sesion y linea_base_cognitiva (DatosCognitivos.kt, CognitivoDao). Guardan cada ensayo,
 * el retraso con la Tierra (llegaTierraEn), la línea base y el estado frente a ella.
 * Registro diario (sueño, somnolencia, síntomas): registro_diario (DatosTripulacion.kt).
 * Esas tablas usan astronautaId = astronaut.id.
 *
 * Prototipo: no hay migraciones. Si cambias una entidad, sube `version` y Room borra y
 * recrea la base de datos al arrancar (los datos de ejemplo se vuelven a cargar).
 */
@Database(
    entities = {
        AppUserEntity.class,
        MissionEntity.class,
        AstronautEntity.class,
        ContinuousMeasurementEntity.class,
        EcgRecordingEntity.class,
        HourlySummaryEntity.class,
        BloodPressureEntity.class,
        ClinicalParameterEntity.class,
        ClinicalTestEntity.class,
        ClinicalResultEntity.class,
        AlertThresholdEntity.class,
        AlertEntity.class,
        // Pruebas cognitivas
        SesionPrueba.class,
        EnsayoStroop.class,
        EnsayoNBack.class,
        ResumenSesion.class,
        LineaBaseCognitiva.class,
        // Registro diario
        RegistroDiario.class
    },
    version = 3,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppUserDao appUserDao();
    public abstract MissionDao missionDao();
    public abstract AstronautDao astronautDao();
    public abstract ContinuousMeasurementDao continuousMeasurementDao();
    public abstract EcgRecordingDao ecgRecordingDao();
    public abstract HourlySummaryDao hourlySummaryDao();
    public abstract BloodPressureDao bloodPressureDao();
    public abstract ClinicalParameterDao clinicalParameterDao();
    public abstract ClinicalTestDao clinicalTestDao();
    public abstract ClinicalResultDao clinicalResultDao();
    public abstract AlertThresholdDao alertThresholdDao();
    public abstract AlertDao alertDao();
    public abstract CognitivoDao cognitivoDao();
    public abstract TripulacionDao tripulacionDao();

    private static volatile AppDatabase INSTANCE;

    /** Background threads used to write to the database off the main thread. */
    public static final ExecutorService io = Executors.newFixedThreadPool(2);

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "astronaut_health.db")
                            // Development only: if the schema version changes, wipe and recreate the DB.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
