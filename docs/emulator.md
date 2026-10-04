# Run in an Android emulator

Requires the [local build setup](build.md), Android Emulator and `system-images;android-36;google_apis;x86_64` in `.tooling/android-sdk`, with Windows Hypervisor Platform available. From the repository root:

```powershell
./scripts/build.ps1
./scripts/emulator.ps1
```

The launcher creates or reuses `TripTrack_API_36` on port 5556, with 3 GB RAM and four virtual CPU cores. It installs the existing APK, preserves app data and opens TripTrack. Rebuild after code changes.

Software graphics is the default because automatic graphics exited on this PC. CPU acceleration remains enabled. To try GPU rendering later, close the emulator and use `-Graphics auto`.

Check the current scaffold:

- **Rides:** inspect the empty history.
- **Settings:** change each timing independently; valid values are 1–1440 minutes.
- Save, close and reopen the app to check local preference persistence.

Verified on API 36: launch, both screens and preference persistence after force-stop/relaunch. Defaults were restored afterward. First boot briefly showed a System UI timeout, then recovered.

Ride recording, GPS/sensor collection, Waze capture/OCR, stop detection, route analysis and cloud sync are not implemented.

For future tests, **More → Location** supports [route playback and GPX/KML import](https://developer.android.com/studio/run/emulator-extended-controls). TripTrack cannot record simulated locations yet. Real-road GPS quality and Waze/device behavior still require phone testing.
