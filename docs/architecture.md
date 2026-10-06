# Architecture

Chosen for this scaffold: Kotlin, Compose, Room and Firebase. One Android module keeps builds simple; package boundaries keep tasks focused.

| Area | Source package | Responsibility |
| --- | --- | --- |
| Models | `model` | Settings, telemetry identity/units and accepted speed selection |
| Local data | `data/local` | Room entities, DAOs and schema |
| Composition | `data/AppContainer` | Database and repositories |
| Cloud | `sync` | Firebase readiness; synchronization is pending |
| Interface | `ui` | ViewModel, screens and theme |
| Recording | `recording` | Foreground lifecycle, phone collectors and source gaps |
| Capture | `capture` | Complete-frame OCR, source clocks and temporary-frame cleanup |

UI observes Room-backed readings and recorder state through the ViewModel. Phone collectors run independently of screen capture. The capture engine saves one complete frame temporarily, recognizes all text, persists text/geometry and candidate fields, then deletes the frame. Usage access gates Waze foreground attribution. A stopped projection leaves phone recording active and requires fresh consent to resume capture.

`TelemetrySample` identifies source, channel/unit, observed and received timestamps, quality, nullable value, accuracy and evidence reference. Providers must validate clock mappings and source-specific accuracy semantics. A missing value stays missing.

`recordedTopSpeedMetersPerSecond` selects the maximum already-accepted speed. It does not establish sensor accuracy or perform physics filtering.

Prototype readings are unvalidated. Sensor accuracy stores SensorManager status; location accuracy uses channel-specific units. AOSP frame-clock mapping is retained with its raw clock diagnostics for phone validation. Quality classification, route analysis and cloud workers are pending.

Settings accept 1–1440 minutes in this scaffold. This UI range is an implementation choice, not a settled product limit. Review database migrations when schemas change; never discard saved rides to avoid migration work.

System backup and device transfer are disabled in the scaffold. Explicit cloud sync and its retention rules remain pending.
