# LibreSpeedo Architecture

LibreSpeedo is built with modern Android development practices, emphasizing F-Droid compatibility (no proprietary Google Play Services), Kotlin Coroutines, and Jetpack Compose.

## App Architecture Diagram
```mermaid
graph TD
    subgraph "Hardware & OS"
        LM[Android LocationManager]
        SP[SharedPreferences]
    end

    subgraph "Data Layer"
        LC[LocationClient]
        SR[SettingsRepository]
    end

    subgraph "Presentation Layer"
        VM[SpeedometerViewModel]
    end

    subgraph "UI Layer (Jetpack Compose)"
        MA[MainActivity]
        Theme[LibreSpeedoTheme]
        SS[SpeedometerScreen]
        SetS[SettingsScreen]
    end

    %% Data Flow
    LM -->|Native GPS/Fused Updates| LC
    LC -->|Flow&lt;Location&gt;| VM
    VM -->|StateFlow&lt;SpeedometerUiState&gt;| SS

    %% Settings Flow
    SP <-->|Read/Write| SR
    SR -->|StateFlow&lt;Boolean&gt;| MA
    SR -->|StateFlow&lt;Boolean&gt;| Theme
    SR -->|StateFlow&lt;Boolean&gt;| SetS

    %% UI Hierarchy
    MA --> Theme
    Theme --> SS
    Theme --> SetS
```

## 1. Location Layer (Native Fused Provider)
To maintain the app's Libre status, we do not use the proprietary `FusedLocationProviderClient` from `com.google.android.gms`. Instead, `LocationClient` relies entirely on the native `android.location.LocationManager`.

### Provider Fallback Mechanism
When tracking begins, the `LocationClient` checks hardware capabilities and API levels to request the most accurate and battery-efficient provider:
1. **Native Fused Provider (`LocationManager.FUSED_PROVIDER`)**: Available on Android 12 (API 31) and above. It automatically fuses GPS, Wi-Fi, and sensors without Google Play Services.
2. **GPS Provider (`LocationManager.GPS_PROVIDER`)**: Fallback for devices below API 31 or if the fused provider is disabled. Relies purely on GNSS satellites.
3. **Network Provider (`LocationManager.NETWORK_PROVIDER`)**: Final fallback utilizing cell-towers and Wi-Fi networks if GPS is unavailable.

Location updates are wrapped and emitted as a continuous Kotlin `Flow<Location>`.

## 2. Presentation Layer
* **`SpeedometerViewModel`**: Subscribes to the `LocationClient`'s Flow. It parses raw location metrics (converting speed from m/s to km/h, extracting bearing and altitude), handles runtime exceptions, and exposes a single `SpeedometerUiState` state flow.
* **`SpeedometerScreen`**: A declarative Jetpack Compose UI that blindly renders the `SpeedometerUiState`. It includes a compass, large speed text, raw coordinates, and a high-contrast debug panel for monitoring sensor accuracy.

## 3. UI Theming & Branding
LibreSpeedo utilizes a custom Material 3 Dark Theme mapping:
* **TealPrimary (`#236077`)**: Used for primary accents.
* **AmberAccent (`#F29938`)**: High-visibility warning/accent color.
* **SlateBackground (`#1E2A35`)**: The core background tone designed to reduce glare at night.
* **SurfaceDark (`#2B3945`)**: Used for elevated cards and panels (assigned to `surface` and `surfaceVariant`).
* **TextWhite (`#F1F5F9`)**: Crisp typography color (`onSurface`, `onBackground`).

Dynamic coloring (Android 12+) is explicitly disabled (`dynamicColor = false`) to enforce this high-contrast brand aesthetic regardless of the user's system wallpaper.
