# Development guide

## Setup and pinned configuration

Open the repository root in Android Studio and let Gradle sync. Install Android SDK platform 37 and platform-tools; use a device/emulator with API 26 or newer. Configure the SDK in Android Studio or in an untracked `local.properties` file (`sdk.dir=/absolute/path/to/Android/Sdk`). Do not commit machine-specific paths.

Use the checked-in Gradle wrapper rather than a system Gradle installation. Android Studio must support the pinned Android Gradle plugin; the old Koala recommendation is no longer sufficient guidance. The CI workflow selects Temurin JDK 17; this documentation review's local shell uses JDK 21. Java source/target compatibility is 11, which is distinct from the JDK running Gradle. Match the runtime to the pinned Gradle/AGP requirements and keep CI aligned when updating them.

| Setting | Repository value |
| --- | --- |
| Application ID / namespace | `com.plabin.librespeedo` |
| Minimum / compile / target SDK | 26 / 37 / 37 |
| App version name / code | `1.0` / `1` |
| Android Gradle plugin | 9.4.1 |
| Gradle wrapper | 9.8.2 |
| Kotlin Compose plugin | 2.4.21 |
| KSP | 2.3.6 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| Navigation Compose | 2.10.2 |
| Robolectric | 4.17 |

Values come from `app/build.gradle.kts`, `gradle/libs.versions.toml`, and `gradle/wrapper/gradle-wrapper.properties`; these are configured versions, not a claim about the latest available releases. Initial builds require access to the configured Google, Maven Central, Gradle plugin, and wrapper repositories. `BuildConfig.GIT_COMMIT` records the short Git revision when available and falls back to `unknown`.

## Build and install

From the repository root:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat`. The `adb` step requires SDK platform-tools and an authorized device or running emulator; Android Studio's Run action is an alternative. Release builds are not configured here as a publication/signing workflow.

## Verification

Run checks relevant to your change:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Unit tests use JUnit, Robolectric, Compose UI testing, Room testing, and coroutine test support. Android resources are enabled; `app/src/test/resources/robolectric.properties` selects SDK 34 for Robolectric by default. That test SDK differs intentionally from compile/target SDK 37.

For device tests, connect an authorized device/emulator and run:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Instrumentation sources cover Room migration scenarios, Settings, Keep Screen On, and themes. The checked-in migration tests exercise selected upgrades; they are not proof of every upgrade path or device behavior.

| Output | Location |
| --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Unit test HTML report | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Unit test XML results | `app/build/test-results/testDebugUnitTest/` |
| Lint HTML report | `app/build/reports/lint-results-debug.html` |
| Connected test reports | `app/build/reports/androidTests/connected/` |

The [code analysis review](code-analysis-review.md) records earlier results. Those results are historical, not checks automatically rerun by this documentation update.

## Continuous integration

`.github/workflows/android.yml` runs for pushes and pull requests targeting `master`. It checks out source, selects Temurin JDK 17, builds with `./gradlew assembleDebug`, and uploads `librespeedo-debug-apk`. It currently does not run unit tests, lint, or device tests as explicit checks. Reproduce those separately before relying on their results.

## Source map

Paths below are relative to `app/src/main/java/com/plabin/librespeedo/`.

| Area | Files / responsibility |
| --- | --- |
| Entry and lifecycle | `MainActivity.kt`: permission requests, navigation, visible-app tracking, export picker |
| Preferences | `data/SettingsRepository.kt`, speedometer style enums |
| Native inputs | `location/LocationClient.kt`, `sensors/SensorClient.kt`, `sensors/MotionClient.kt` |
| Dashboard state | `ui/SpeedometerViewModel.kt`, `ui/ChronometerState.kt` |
| Dashboard and widgets | `ui/SpeedometerScreen.kt`, `ui/DashboardStatusBar.kt`, `ui/components/` |
| Other screens | Settings, trip history/detail, about, and license in `ui/` |
| Trip calculations | `trips/TripMetrics.kt`, `MotionMetrics.kt`, `DrivingEventDetector.kt` |
| Persistence and export | `trips/TripDatabase.kt`, `MotionChunkCodec.kt`, `TripDatabaseExporter.kt` |
| Car connection | `trips/BluetoothConnectionClient.kt` |
| Theme | `ui/theme/` |

Local tests are under `app/src/test/`; device tests are under `app/src/androidTest/`. Exported Room schemas are under `app/schemas/` (currently snapshots 2, 3, and 5). Original artwork is under `assets/`. `ai-docs/` is ignored local material, not published project documentation.

## Changing behavior

Keep location, sensor, and Bluetooth integrations on native/public APIs. Preserve the separation between compass snapshots and timestamped trip motion. For metric or persistence changes, verify gaps, duplicate timestamps, coverage, checkpoint atomicity, interrupted recovery, and disconnect-boundary trimming. Update Room schema/migrations and exported schemas together when the database changes.

Device validation remains necessary for provider switching, missing permissions/sensors, already-connected cars, disconnect/reconnect, rotation, backgrounding/screen lock, mounting/orientation quality, and database export. Wired/wireless Android Auto connections can be useful test scenarios even though an Android Auto app interface and background recording are not implemented.

Keep the [user guide](user-guide.md), [architecture](architecture.md), and [trip roadmap](trip-logger-plan.md) aligned with behavior. Preserve the GPL license copies and artwork credits. Documentation-only edits generally need link/format and source-consistency checks rather than a full Android test run.
