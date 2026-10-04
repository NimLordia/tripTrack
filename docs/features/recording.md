# Recording

Direction: record all useful available sources independently: Android location, phone motion sensors, Waze capture and navigation API data. Availability and source priority require validation. See [capture](capture.md) and [navigation](../integrations/navigation.md) for their constraints.

Preserve original values, source identity, measurement time, capture/receipt time and available accuracy information. Keep derived results and edits separate. Align clocks and account for source delay; OCR values use the source frame's time, not recognition completion. Phone samples, screen frames, OCR, API delivery and uploads have separate frequencies.

Preserve collected data through app switches, permission loss, capture cancellation and process interruption. Represent missing/stale readings as unavailable, never zero speed or repeated stale values. Test actual recovery behavior on the phone.

Screen lock means the ride enters a stopped product state in V1; it is not a measured zero-speed sample.

Open: pause versus finish on lock, unlock/resume behavior, time accounting during lock, non-screen collectors during lock, source priorities and effective sampling rates. Phone model, Android version, sensor inventory and mounting are unknown. Local recordings must not depend on a cloud round trip.
