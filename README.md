# LibreSpeedo

LibreSpeedo is an experimental Android speedometer and local trip logger built with Kotlin and Jetpack Compose. It uses Android's native location, sensor, and Bluetooth APIs without proprietary Google Play Services.

**This is a toy project with mainly AI-assisted code, created to explore AI capabilities in an unfamiliar domain. Measurements and maneuver detection are experimental.**

## Features

- **Customizable dashboard:** add, remove, resize, and rearrange widgets in a two-column grid using Edit Mode. Layout and appearance settings are saved locally.
- **Digital and analog speedometers:** km/h, mph, and m/s; Standard or 7-segment digital readouts; Classic, Sport, Minimal, and Cat analog styles.
- **Digital and analog compasses:** filtered, tilt-compensated hardware heading with GPS bearing while moving. Position and debug widgets expose coordinates, provider, accuracy, and sensor readings.
- **Chronometer:** start/pause, reset, and laps, independent of trip recording.
- **Trip logging:** manual Start/Finish, or automatic start after sustained movement with a selected paired Bluetooth car. Recording runs while LibreSpeedo is visible.
- **Trip history:** duration, speed, distance, stopped time, motion metrics, coverage, speed/G profiles, and detected maneuvers. Delete individual trips or configure retention.
- **Local data controls:** optional route storage, a standalone SQLite database export, OLED theme, Keep Screen On, and dashboard status indicators.

The dashboard also includes a Hello World demonstration widget. A driving rating, background recording, and an Android Auto interface are not implemented. Native providers depend on the device and OS; this project does not claim F-Droid publication or certification.

## Get started

Install a debug build, grant location access, and enable Android location services. Use **Start trip** on the dashboard for manual logging. For automatic logging, pair your car in Android settings, then enable **Automatic trip logging** and **Choose paired car** in LibreSpeedo settings. Trip settings currently may require a full app restart to reach the recorder; the [user guide](docs/user-guide.md) explains this limitation.

**Keep LibreSpeedo visible during recording.** Leaving the app or locking the screen ends a trip as interrupted; rotation and navigation inside the app preserve it. See the [user guide](docs/user-guide.md) for setup, measurement limits, and data controls.

## Build

Use an Android Studio installation compatible with the project's Android Gradle plugin, an Android SDK containing **API 37**, and a device/emulator running **Android 8.0 (API 26)** or newer. The checked-in CI workflow selects **JDK 17**. Dependency and wrapper versions are pinned in the repository; see the [development guide](docs/development.md) for details.

Open the repository root in Android Studio and let Gradle sync, or run from the root:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it through Android Studio's Run action or, with Android SDK platform-tools and an authorized device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Documentation

Start with the [documentation index](docs/README.md):

- [User guide](docs/user-guide.md): dashboard, recording, history, permissions, and privacy.
- [Architecture](docs/architecture.md): data flow, lifecycle, calculations, and storage.
- [Development guide](docs/development.md): setup, builds, tests, and source map.
- [Trip logger status and roadmap](docs/trip-logger-plan.md): implemented scope and remaining design work.
- [Code analysis review](docs/code-analysis-review.md): historical inspection findings and validation.

## License and credits

LibreSpeedo is licensed under **GPL-3.0-or-later**; see [LICENSE](LICENSE). The app includes a copy in its license screen.

- **Author:** Patricio Labin Correa (f1r3f0x).
- **Logo:** Tamar Labin Correa (tmlabin99@gmail.com).
