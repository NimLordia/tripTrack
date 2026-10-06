# Waze capture

While Waze is displayed, repeat this cycle: temporarily save one full-screen frame, immediately analyze relevant visible information beyond speed, persist the extracted readings, delete the frame, then capture the next. Retain the extracted data, source-frame timestamps and quality metadata. Waze's controls are English; the supplied example also contains Hebrew street names. [Layout evidence](waze-layout.md) describes visible fields and extraction limitations.

V1 supports portrait only. Unsupported landscape content must not become valid readings.

Capture the full display through MediaProjection. Analyze each complete frame without cropping or selecting a subregion. Frame timing follows [recording](recording.md). Waze video and frame images are not retained.

If Waze is no longer displayed or capture becomes unavailable, record a gap and validate Waze content before resuming. Never analyze another app's pixels as Waze. Follow [recording](recording.md) for ride state.

Clean up temporary frames on analysis failure, cancellation and recovery from interruption.

Historical platform finding, 2026-10-04: Android 15 QPR1+ ends projection on lock; terminated sessions need fresh consent. Recheck [capture lifecycle](https://developer.android.com/media/grow/media-projection#resource-recovery) when implementing.

Open: extraction fields and methods, frame cadence, unfinished-analysis handling and blank-versus-marked gaps. [Validation](capture-validation.md) owns test design.
