# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app for the NASA Space Apps Challenge 2026, challenge "Create Health Monitoring Software for Astronauts on Space Missions" (team SpaceDrifters, 48 h hackathon, several people working in parallel). It is a prototype: not a medical device, all crew data is simulated. Respond to the user in Spanish (castellano). `README.md` (in Spanish) is the team-facing guide — keep it in sync when structure or conventions change.

Current state: only the shared skeleton exists (Gradle, theme, navigation, empty placeholder screens). `data/local`, `data/repository`, `domain/model` and `assets/` are empty (`.gitkeep`). Room and kotlinx.serialization are configured as dependencies but not used yet.

## Commands

Gradle wrapper (`./gradlew` in Bash, `.\gradlew` / `gradlew.bat` on Windows):

- Build: `./gradlew assembleDebug`
- Unit tests (JVM, `app/src/test`): `./gradlew testDebugUnitTest`
- Single test: `./gradlew testDebugUnitTest --tests "com.example.nasa_spaceappchallenge2026_spacedrifters.ExampleUnitTest.suma_esCorrecta"`
- Instrumented tests (`app/src/androidTest`, needs device/emulator): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lintDebug`

## Build setup (non-obvious bits)

- AGP 9.2.1 with **built-in Kotlin**: do not apply `org.jetbrains.kotlin.android`. The Kotlin version is set by the `kotlin` entry in `gradle/libs.versions.toml` (compose + serialization compiler plugins).
- `compileSdk` 37 is required by Compose 1.12 / Lifecycle 2.11; `targetSdk` 36, `minSdk` 26 (java.time without desugaring).
- Room uses KSP (`ksp(libs.androidx.room.compiler)`), not kapt.
- All versions live in `gradle/libs.versions.toml`; repositories only in `settings.gradle.kts` (`FAIL_ON_PROJECT_REPOS`).
- `.idea/` is git-ignored on purpose (machine-specific).

## Architecture

Package root `com.example.nasa_spaceappchallenge2026_spacedrifters`:

- Single Activity (`MainActivity`) → `SpaceDriftersTheme` → `ui/navigation/AppNavHost`.
- Navigation Compose with **type-safe routes** (`@Serializable` objects/data classes in `ui/navigation/Rutas.kt`). Screens never receive the `NavController`; they get lambdas (`onAtras`, `onMedico`, ...) wired in `AppNavHost`.
- MVVM (ViewModel + StateFlow) with **manual DI**: `SpaceDriftersApp` owns an `di/AppContainer` (create DB/repositories there with `by lazy`); every ViewModel is registered as an `initializer { ... }` in `di/AppViewModelProvider.Factory`, using `CreationExtras.app()` to reach the container. No Hilt.
- Layers: `data/local` (Room entities/DAOs/AppDatabase) → `data/repository` → `ui/screens/<rol|astronauta|medico>`. Pure logic without Android (e.g. the planned JSON-driven rule engine) goes in `domain/` so it can be unit-tested on the JVM.
- Every screen wraps its content in `ui/components/PantallaBase`, which provides the top bar (back button when `onAtras != null`) and the mandatory "Prototipo, no es un dispositivo médico · Datos simulados" footer (`AvisoPrototipo`).

## Conventions

- Code identifiers and UI text are in Spanish. All visible text goes in `res/values/strings.xml` (Spanish, default) **and** `res/values-en/strings.xml` (English, for the NASA submission).
- Always-dark "mission control" theme; palette only in `ui/theme/Color.kt`. Use `MaterialTheme.colorScheme` and, for health states, `LocalEstadoColores`. States (Normal / Vigilar / Alerta / Sin datos) must never be conveyed by color alone — always with text; missing data is "Sin datos", never shown as fine.
- Touch targets ≥ 48 dp; every icon has a `contentDescription` from strings. Icons are local vector drawables in `res/drawable/ic_*.xml` (no material-icons dependency).
