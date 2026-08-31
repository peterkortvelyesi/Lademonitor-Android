# Lademonitor – Android

**Language:** English | [Deutsch](README.md)

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

Android app (Kotlin + Jetpack Compose) for [Lademonitor](https://github.com/iDomi94/Lademonitor-Server) –
the Android counterpart to the iOS app. It ports the SwiftUI app's
architecture and features 1:1: an **offline-capable local store** (Room)
plus a **server mode with bidirectional synchronization**.

## Features (same as the iOS app)

- **Two modes:** "Local only on this device" (all data in a local Room
  database) or "Connect to your own server" (login/registration, token
  stored encrypted, ongoing synchronization with the server).
- **Dashboard** with metrics and charts (donut charts per provider, AC/DC
  split, cost/kWh/consumption per month) – computed locally so it isn't
  empty even offline.
- **Charging sessions**: view, create, edit, confirm (needs_review),
  delete (swipe gesture). Consumption display (kWh/100km) using the same
  fallback chain as the iOS app.
- **Map** (OpenStreetMap via osmdroid, no API key needed) with all charging
  locations (incl. matching radius) and charging sessions; tapping opens
  detail/edit.
- **Vehicles, providers, charging locations** management; address search
  (in local mode via OSM Nominatim, in server mode via the server proxy)
  and "current location".
- **Time-range filter** (presets + custom range), global across dashboard,
  charging sessions, and map.

## Requirement

For server mode, a running
[Lademonitor-Server](https://github.com/iDomi94/Lademonitor-Server). On
first launch, enter the server address (domain, `https://` is added
automatically) as well as username/password. "Local only" mode works
without a server.

## Build & run

1. Open the `Lademonitor-Android/` folder in **Android Studio**
   (Giraffe/Koala or newer, with Android SDK 35).
2. Wait for the Gradle sync (downloads AGP 8.6, Kotlin 2.0, Compose BOM,
   Room, osmdroid, …). If Android Studio suggests newer plugin versions,
   you can accept them.
3. Run on a device/emulator (Android 8.0 / API 26 or newer) (Run ▶).

Alternatively via the command line (Android SDK required; `local.properties`
with `sdk.dir=` is created automatically by Android Studio):

```bash
./gradlew assembleDebug
```

The resulting APK is then located under `app/build/outputs/apk/debug/`.

## Project structure

```
app/src/main/java/com/dominiqueherbrigpersonalteam/lademonitor/
  data/
    model/       - DTOs, enums, payloads (counterpart to Models.swift)
    remote/      - ApiClient (OkHttp), Moshi setup, server date format
    local/       - Room entities, DAOs, database (counterpart to LocalModels/LocalStore)
    settings/    - AppSettings (mode/server URL), TokenStore (encrypted)
    session/     - SessionManager (auth state)
    net/         - NetworkMonitor (reachability)
    location/    - CurrentLocationProvider (GPS without Google Play Services)
    repo/        - AppRepository, LocalDataStore, SyncService,
                   LocalStats-/LocalConsumptionCalculator, LocalGeocoder
  ui/
    theme/       - Compose theme (light/dark)
    auth/        - Mode selection, login/registration
    dashboard/   - Dashboard + custom-drawn charts
    sessions/    - List, create/edit, detail
    map/         - osmdroid map + mini map in detail view
    settings/    - Settings, vehicles/providers/locations, connection
    filter/      - Global time-range filter
    common/      - Formatting, reusable building blocks
```

## Differences from the iOS app (intentional)

- **Map:** OpenStreetMap (osmdroid) instead of Apple Maps – no API key
  needed. Charging sessions are shown as individual markers; the iOS app's
  map clustering is not (yet) ported.
- **Local address search:** instead of Apple `MKLocalSearch`, local mode
  queries OSM Nominatim directly (the same source the server proxies).
- **Token storage:** Android `EncryptedSharedPreferences` instead of the
  iOS Keychain.

## License

AGPL-3.0 – matching the server and iOS repos.
