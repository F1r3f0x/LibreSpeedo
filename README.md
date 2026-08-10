# LibreSpeedo

LibreSpeedo is an open-source, privacy-respecting speedometer and trip logger for Android. Built strictly without proprietary Google Play Services, it is 100% compatible with F-Droid and relies entirely on Android's native location APIs.

## Features (Prototype)
- **Native Fused Location**: Leverages Android 12+'s native `FUSED_PROVIDER` for high accuracy without Google Play Services.
- **Smart Fused Compass**: Fuses hardware sensors (accelerometer & magnetic field) with GPS bearing. Includes tilt-compensation for upright dashboards and a low-pass filter to eliminate jitter.
- **Modular Dashboard**: A fully customizable interface where you can add, remove, resize, and drag-and-drop widgets. The layout adapts dynamically using a Staggered Grid. Current widgets include:
  - **Speedometer**: Huge, high-contrast speed display with customizable units (km/h, mph, m/s).
  - **Compass**: Real-time heading needle that correctly points North.
  - **Position**: Raw GPS coordinates.
  - **Debug Panel**: Monitor exactly which hardware provider is active, alongside altitude, accuracy metrics, and raw sensor data (accel, gyro, mag).
  - **Hello World**: A simple placeholder for future feature expansion!
- **Dashboard Management**: A dedicated Edit Mode prevents accidental swipes, while a Reset Dashboard Layout button restores defaults instantly.

## Architecture & Technical Details
See the [docs/architecture.md](docs/architecture.md) file for a deep dive into the native Location fallback logic, Kotlin Coroutines Flow implementation, and custom Material 3 theming.

## Prerequisites
To compile and run this project, you will need:
- **Android Studio** (Koala or newer recommended)
- **Java Development Kit (JDK) 17** (Usually bundled with modern Android Studio)
- An Android Emulator or physical device running **Android 8.0 (API 26)** or higher.

## How to Open and Compile the Project

### 1. Opening the Project in Android Studio
1. Launch **Android Studio**.
2. On the welcome screen, click **Open** (or go to `File > Open...` if a project is already open).
3. Navigate to the folder where you cloned or downloaded this repository (e.g., `LibreSpeedo`) and select it.
4. Click **OK**. Android Studio will begin indexing the files and downloading Gradle dependencies. Wait for the background tasks to finish (check the bottom progress bar).

### 2. Building and Compiling (Command Line)
If you prefer building via the terminal, navigate to the root of the project and run:
```bash
./gradlew assembleDebug
```
The compiled APK will be output to `app/build/outputs/apk/debug/app-debug.apk`.

### 3. Deploying to Your Device
1. Enable **Developer Options** and **USB Debugging** on your Android phone.
2. Plug your phone into your computer via USB and accept the debugging prompt on your phone's screen.
3. In Android Studio, ensure your device is selected in the deployment dropdown next to the green "Run" button at the top toolbar.
4. Click the green **Run 'app'** button (or press `Shift + F10`). Android Studio will compile, install, and launch LibreSpeedo on your device!

## Credits & Copyright
- **Logo**: Designed by Tamar Labin Correa (tmlabin99@gmail.com).
