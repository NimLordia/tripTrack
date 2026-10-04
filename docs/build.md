# Build and run

Requirements: JDK 17, Android SDK platform 36 and Build Tools 35.0.0. Dependency versions are pinned in `gradle/libs.versions.toml`; Gradle 8.13 uses the official checked wrapper.

Open the project in Android Studio. Install the SDK packages after reviewing Google's SDK license. Set `sdk.dir` in ignored `local.properties`, or configure `ANDROID_HOME`. Portable tools prepared on this machine live in ignored `.tooling/`; they are not part of the source project.

```powershell
./scripts/build.ps1
# Or with a configured JDK and SDK:
./gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The helper uses local tools when present without changing system settings. Output: `app/build/outputs/apk/debug/app-debug.apk`.

To run the current scaffold on this PC, follow the [emulator guide](emulator.md).

Firebase is optional for local startup. To configure it, register Android package `com.triptrack.app` and place your project configuration in ignored `app/google-services.json`. This enables configuration detection; authentication, synchronization and access rules still need implementation. No cloud resources are created by this scaffold.

Verification covers compilation, JVM domain tests and Android lint. Installing the APK and checking capture, sensors and background behavior on the user's phone are separate steps.
