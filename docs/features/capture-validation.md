# Capture validation

Establish a credible V1 top-speed source on the actual phone by comparing phone speed, Waze display readings and API progress. Inspect candidate peaks and delays; a recorded maximum is not a guarantee about an unobserved peak.

Measure Waze's visible speed-change cadence, then tune capture and OCR with enough margin to avoid missed changes. No guaranteed fixed rate was established. Approximately 30 fps for short diagnostics is a proposal, not a permanent requirement. Unchanged rounded digits do not reveal Waze's internal refresh frequency.

Measure frame spacing, missed frames, extraction accuracy, complete-cycle latency, CPU use, battery, heat and temporary-frame storage. Verify full-screen coverage and extraction of relevant displayed Waze information without crops. Confirm readings use the source frame's time and each frame is deleted before the next cycle.

Exercise app switching/return, lock/unlock, user cancellation, permission loss, analysis failure, process interruption and unsupported landscape use. Validate saved readings, gap handling and temporary-frame cleanup after each.

The user observes roughly 8–12 km/h between bike speedometer and GPS. Compare across speeds; do not hardcode a correction or assume either source is exact. Use automatic recording and later review; hard-acceleration testing belongs on a closed course without watching the phone.
