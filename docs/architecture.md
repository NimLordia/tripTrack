# Scaffold architecture

Chosen for this scaffold: Kotlin, Compose, Room and Firebase. One Android module keeps builds simple; package boundaries keep tasks focused.

| Area | Source package | Responsibility |
| --- | --- | --- |
| Models | `model` | Settings, telemetry identity/units and accepted speed selection |
| Local data | `data/local` | Room entities, DAOs and schema |
| Composition | `data/AppContainer` | Database and repositories |
| Cloud | `sync` | Firebase readiness; synchronization is pending |
| Interface | `ui` | ViewModel, screens and theme |

UI observes Room-backed state through the ViewModel. Collectors will append telemetry separately from user stop edits and computed results. No collector, quality classifier, route engine or upload worker is implemented yet.

`TelemetrySample` identifies source, channel/unit, observed and received timestamps, quality, nullable value, accuracy and evidence reference. Providers must validate clock mappings and source-specific accuracy semantics. A missing value stays missing.

`recordedTopSpeedMetersPerSecond` selects the maximum already-accepted speed. It does not establish sensor accuracy or perform physics filtering.

Settings accept 1–1440 minutes in this scaffold. This UI range is an implementation choice, not a settled product limit. Review database migrations when schemas change; never discard saved rides to avoid migration work.

System backup and device transfer are disabled in the scaffold. Explicit cloud sync and its retention rules remain pending.
