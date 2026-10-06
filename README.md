# TripTrack

An Android motorcycle ride recorder in development, designed for reviewing a ride during a long stop or at the end of a trip, with minimal interaction while riding.

**Current status: recorder prototype; target-phone validation is pending.** See the [product scope](docs/product.md) for the intended V1 and the [recorder test guide](docs/recorder-test.md) for the first device run.

## Current state

Implemented in the prototype:

- Start/Stop controls, saved ride history and a reading inspector.
- Foreground phone location, accelerometer and gyroscope collection, independent of screen capture.
- Full-screen Waze frame processing with bundled English text recognition; extracted text, text positions and candidate fields are saved, then the temporary frame is deleted.
- Candidate speed, maneuver distance and arrival/duration/distance panel parsing based on the owner's portrait Waze example.
- Capture gaps and fresh-consent resume within the same ride; phone collection continues during screen lock.
- Locally saved, independent short-stop, inactivity-reminder and snooze preferences.
- Room storage for readings, capture results and gaps, with a migration preserving the original database.
- Optional Firebase configuration detection.

Actual OCR output, layout variations, source clocks, GPS quality, extraction cadence and locked-phone behavior still require device validation. Hebrew street names and visual icons shown in the [example](docs/features/waze-layout.md) require additional extraction support. Readings remain unvalidated; a credible top-speed classifier is pending. [Stop detection](docs/features/stops.md), [route analysis](docs/features/analysis.md), navigation API integration and [cloud synchronization](docs/features/storage.md) remain upcoming. Timing preferences are saved, but filtering and reminder behavior are pending.

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
