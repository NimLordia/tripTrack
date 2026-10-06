# First recorder test

Build with [the baseline helper](build.md), then install `app/build/outputs/apk/debug/app-debug.apk` on the [target phone](features/recording.md). The prototype uses bundled Latin-script text recognition; phone and Waze behavior still need validation.

1. Open TripTrack and allow usage access so it can check whether Waze is displayed.
2. Start recording, allow location access and grant full-display screen capture.
3. Open Waze in portrait. Run a short test, then return to TripTrack and inspect the current ride's reading counts.
4. Lock the phone. Unlock it and check that phone samples continued while Waze capture has a gap. Resume Waze capture with fresh consent in the same ride.
5. Stop recording. Open the saved ride and inspect phone sources, original values, observation/receipt times, complete-frame recognized text, candidate fields, processing delay and gaps.
6. Close and reopen TripTrack; confirm the recorded data remains available.

Verify that temporary frames are deleted after each cycle and after cancellation/failure. With USB debugging, `adb shell run-as com.triptrack.app ls cache/triptrack-capture` should show no abandoned frame files after stopping. No video or screenshot history is retained.

Compare candidate speed, maneuver distance and the arrival/duration/distance panel against the [supplied Waze example](features/waze-layout.md). Confirm the phone status clock is excluded from arrival time. Hebrew names and icon meanings need additional extraction support.

Readings are unvalidated. Credible top speed, stop filtering/reminders, route analysis and cloud sync remain pending. Measure accuracy, cadence, battery and heat before longer tests.
