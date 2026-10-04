# Waze capture

Let the user move and resize a rectangle in settings, save its coordinates, then record and analyze cropped video from that fixed region. Recalibration remains available. The user will provide the actual Waze layout. Recognized speed values and supporting still crops can accompany video; numbers alone do not replace the video requirement.

V1 supports portrait only. Unsupported landscape content must not become valid readings. Automatic tracking of a moving speedometer is not required.

Capture the app/display through MediaProjection, then crop internally. An optional outline is a placement aid; Android controls cross-app window order, so arbitrary overlay levels cannot override system windows. A permanent outline is unnecessary.

If Waze becomes unavailable, record a gap and validate content before resuming. Never analyze another app's pixels as Waze. Follow [recording](recording.md) for ride state.

Historical platform finding, 2026-10-04: Android 15 QPR1+ ends projection on lock; terminated sessions need fresh consent. Recheck [capture lifecycle](https://developer.android.com/media/grow/media-projection#resource-recovery) when implementing.

Open: encoding, live versus later OCR, retention and blank-versus-marked gaps. [Validation](capture-validation.md) owns test design.
