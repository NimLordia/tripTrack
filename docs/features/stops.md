# Stops and reminders

Detect stops and preserve start/end boundaries. Let users select timeline strips or route sections and include/exclude stops from analysis.

Working heuristic: exclude stops shorter than five minutes from adjusted time. Duration does not establish that a stop was a traffic light. Adjustable stop filtering is the working design; exact controls remain open.

Three independent settings:

| Setting | Initial value | Status |
| --- | --- | --- |
| Short-stop exclusion threshold | 5 minutes | Working filter design |
| Inactivity reminder threshold | 5 minutes | User-configurable requirement |
| Reminder snooze | 5 minutes | User-configurable requirement |

Changing one must not silently change the others. Missing/stale data does not prove stationary time.

Proposed editing behavior: retain raw recordings; manual choices override automatic filtering; preserve detected boundaries separately from corrections; recalculate statistics.

Proposed reminder actions: Finish ride, Keep recording, Remind me later. Behavior when ignored or movement resumes remains open.

Open: boundary editing, movement thresholds, poor-reception behavior and notification behavior. Test slow movement and known short/long stops. Richer stop classification and wider-window inference remain [later experiments](../later/index.md).
