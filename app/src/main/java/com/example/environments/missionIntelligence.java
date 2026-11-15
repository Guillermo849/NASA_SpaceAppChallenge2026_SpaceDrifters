import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.io.IOException;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MissionIntelligence {

    static Map<String, Map<String, Object>> missionProfiles = Map.of(

        "orbit", Map.of(
            "gravity", 0.0,
            "radiation", "elevated",
            "isolation", "high",
            "environment", "closed",
            "hazards", List.of(
                "microgravity",
                "space_radiation",
                "isolation",
                "closed_environment"
            )
        ),

        "moon", Map.of(
            "gravity", 0.16,
            "radiation", "elevated",
            "isolation", "high",
            "environment", "closed",
            "hazards", List.of(
                "partial_gravity",
                "space_radiation",
                "isolation",
                "lunar_dust",
                "closed_environment"
            )
        ),

        "mars", Map.of(
            "gravity", 0.38,
            "radiation", "elevated",
            "isolation", "high",
            "environment", "closed",
            "hazards", List.of(
                "partial_gravity",
                "space_radiation",
                "isolation",
                "martian_dust",
                "closed_environment"
            )
        )
    );

    
    
public static Map<String, Object> getMissionProfile(String missionName) {
    return missionProfiles.get(missionName);
}

static Map<String, List<String>> hazardMapping = Map.of(

    "microgravity", List.of(
        "cardiovascular",
        "musculoskeletal",
        "neurovestibular",
        "immune"
    ),

    "partial_gravity", List.of(
        "musculoskeletal",
        "cardiovascular"
    ),

    "space_radiation", List.of(
        "radiation_exposure",
        "immune"
    ),

    "isolation", List.of(
        "psychological",
        "sleep",
        "immune"
    ),

    "closed_environment", List.of("respiratory"),

    "lunar_dust", List.of("respiratory"),

    "martian_dust", List.of("respiratory")
);


static Map<String, String> hazardDescriptions = Map.of(

    "microgravity",
    "Microgravity can alter immune cell function, cardiovascular regulation, muscle and bone health, and sensorimotor adaptation.",

    "partial_gravity",
    "Partial gravity changes the mechanical loading experienced by the body and may affect musculoskeletal and cardiovascular adaptation.",

    "space_radiation",
    "Space radiation can damage cellular DNA and mitochondria, potentially disrupting immune function and increasing oxidative stress.",

    "isolation",
    "Isolation and mission-related psychological stress may contribute to immune dysregulation, sleep disturbances and psychological strain.",

    "closed_environment",
    "Closed habitats require careful air quality monitoring because airborne contaminants can affect respiratory health.",

    "lunar_dust",
    "Exposure to lunar dust may irritate the respiratory system and presents a potential health risk during lunar surface operations.",

    "martian_dust",
    "Exposure to Martian dust may present respiratory health risks during surface exploration."
);


static Map<String, List<String>> systemParameters = Map.of(

    "cardiovascular", List.of(
        "heart_rate",
        "blood_pressure"
    ),

    "musculoskeletal", List.of(
        "exercise_duration",
        "muscle_strength"
    ),

    "respiratory", List.of(
        "oxygen_saturation",
        "respiratory_rate"
    ),

    "psychological", List.of(
        "stress_level",
        "mood"
    ),

    "sleep", List.of(
        "sleep_duration",
        "sleep_quality"
    ),

    "neurovestibular", List.of(
        "balance",
        "dizziness"
    ),

    "radiation_exposure", List.of(
        "cumulative_radiation_dose"
    ),

    "immune", List.of(
        "white_blood_cell_count",
        "lymphocyte_count",
        "inflammation_markers"
    )
);


@SuppressWarnings("unchecked")
public static List<String> getAffectedSystems(String missionName) {

    Map<String, Object> profile = getMissionProfile(missionName);

    if (profile == null) {
        return new ArrayList<>();
    }

    List<String> hazards = (List<String>) profile.get("hazards");

    Set<String> systems = new HashSet<>();

    for (String hazard : hazards) {
        systems.addAll(
            hazardMapping.getOrDefault(hazard, List.of())
        );
    }

    List<String> result = new ArrayList<>(systems);
    Collections.sort(result);

    return result;
}


public static List<String> getMonitoringParameters(String missionName) {

    List<String> systems = getAffectedSystems(missionName);

    Set<String> parameters = new HashSet<>();

    for (String system : systems) {
        parameters.addAll(
            systemParameters.getOrDefault(system, List.of())
        );
    }

    List<String> result = new ArrayList<>(parameters);
    Collections.sort(result);

    return result;
}


@SuppressWarnings("unchecked")
public static Map<String, Object> getMonitoringExplanations(String missionName) {

    Map<String, Object> profile = getMissionProfile(missionName);

    if (profile == null) {
        return new LinkedHashMap<>();
    }

    Map<String, Object> explanations = new LinkedHashMap<>();

    List<String> hazards = (List<String>) profile.get("hazards");

    for (String hazard : hazards) {

        List<String> systems =
            hazardMapping.getOrDefault(hazard, List.of());

        String description =
            hazardDescriptions.getOrDefault(
                hazard,
                "No description available."
            );

        Map<String, Object> hazardExplanation = new LinkedHashMap<>();
        Map<String, List<String>> affectedSystems = new LinkedHashMap<>();

        hazardExplanation.put("description", description);
        hazardExplanation.put("systems", affectedSystems);

        for (String system : systems) {

            List<String> parameters =
                systemParameters.getOrDefault(system, List.of());

            affectedSystems.put(system, parameters);
        }

        explanations.put(hazard, hazardExplanation);
    }

    return explanations;
}


public static Map<String, Object> generateMissionReport(String missionName) {

    Map<String, Object> profile = getMissionProfile(missionName);

    if (profile == null) {
        return null;
    }

    List<String> affectedSystems = getAffectedSystems(missionName);

    List<String> parameters = getMonitoringParameters(missionName);

    Map<String, Object> explanations =
        getMonitoringExplanations(missionName);

    Map<String, Object> report = new LinkedHashMap<>();

    report.put("mission", missionName);
    report.put("profile", profile);
    report.put("affected_systems", affectedSystems);
    report.put("monitoring_parameters", parameters);
    report.put("monitoring_explanations", explanations);

    return report;
}


public static boolean exportMissionReport(
    String missionName,
    String filename
) {

    Map<String, Object> report = generateMissionReport(missionName);

    if (report == null) {
        return false;
    }

    ObjectMapper mapper = new ObjectMapper();

    try {
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(Path.of(filename).toFile(), report);

        return true;

    } catch (IOException e) {
        System.err.println("Error exporting report: " + e.getMessage());
        return false;
    }
}


public static void main(String[] args) {


    List<String> missions = List.of("orbit", "moon", "mars");
 
 
    for (String mission : missions) {
 
 
        String filename = mission + "_report.json";
 
 
        boolean success = exportMissionReport(mission, filename);
 
 
        if (success) {
            System.out.println("Generated: " + filename);
        } else {
            System.out.println("Error generating: " + filename);
        }
    }
 }
 

}