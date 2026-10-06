# Recording

Direction: record all useful available sources independently: Android location, phone motion sensors, Waze capture and navigation API data. Availability and source priority require validation. See [capture](capture.md) and [navigation](../integrations/navigation.md) for their constraints.

Preserve original values, source identity, measurement time, capture/receipt time and available accuracy information. Keep derived results and edits separate. Align clocks and account for source delay; OCR values use the source frame's time, not recognition completion. Phone samples, screen frames, OCR, API delivery and uploads have separate frequencies.

Preserve collected data through app switches, permission loss, capture cancellation and process interruption. Represent missing/stale readings as unavailable, never zero speed or repeated stale values. Test actual recovery behavior on the phone.

Keep collecting phone location and motion data while the screen is locked. Screen-capture interruptions affect only the Waze source and create gaps; they do not pause or finish the ride. Resume Waze capture with fresh consent when required. Lock is not a measured zero-speed sample.

Target test device: Samsung Galaxy S25+ running Android 16. Sensor inventory and mounting remain unverified.

Open: source priorities and effective sampling rates. Local recordings must not depend on a cloud round trip.
