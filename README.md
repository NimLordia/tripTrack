# TripTrack

An Android motorcycle ride recorder in development, designed for reviewing a ride during a long stop or at the end of a trip, with minimal interaction while riding.

**Current status: scaffold. This build does not record rides yet.** See the [product scope](docs/product.md) for the intended V1.

## Current state

Available in the scaffold:

- Portrait Compose app shell with Rides and Settings screens and an empty ride history.
- Locally saved, independent short-stop, inactivity-reminder and snooze preferences.
- Room storage models for rides, telemetry, recording gaps and stop edits.
- Optional Firebase configuration detection.

Upcoming implementation areas are [phone recording](docs/features/recording.md), [Waze capture and OCR](docs/features/capture.md), [stop detection](docs/features/stops.md), [route analysis](docs/features/analysis.md) and [cloud synchronization](docs/features/storage.md). These linked specifications describe requirements, not completed features.

## Build and run

The app uses Kotlin, Jetpack Compose with Material 3, Room, coroutines and Firebase in one Android module. The configured minimum Android version is Android 8.0 (API 26).

Install **JDK 17**, **Android SDK platform 36** and **Build Tools 35.0.0**. Configure `sdk.dir` in ignored `local.properties`, or set `ANDROID_HOME`; see [build setup](docs/build.md). The Gradle 8.13 wrapper is included.

Clone the repository and run the Windows PowerShell helper:

```powershell
git clone https://github.com/NimLordia/tripTrack.git
cd tripTrack
./scripts/build.ps1
```

The helper runs the debug build, JVM unit tests and Android lint. With JDK and SDK configured, you can also invoke Gradle directly:

```powershell
./gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Open the project in Android Studio to run it, or follow the [PC emulator guide](docs/emulator.md). Firebase is optional for local startup; configuration details are in the build guide.

Build checks do not verify live recording, capture, sensors or background behavior on a phone.

## Repository guide

| Path | Contents |
| --- | --- |
| [`app/`](app/) | Android source, resources and tests |
| [`docs/`](docs/index.md) | Product scope and focused topic specifications |
| [`scripts/`](scripts/) | Build, emulator and context helpers |
| [`gradle/libs.versions.toml`](gradle/libs.versions.toml) | Pinned dependency versions |

Start with the [architecture](docs/architecture.md) and [context index](docs/index.md) when working on the code. Agent instructions live in [AGENTS.md](AGENTS.md).
