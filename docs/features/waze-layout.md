# Waze layout evidence

The owner supplied a portrait Waze screenshot on 2026-10-06. These values were read visually; actual phone OCR remains untested.

| Visible field | Example |
| --- | --- |
| Current speed | 0 km/h |
| Distance to next maneuver | 10 m |
| Maneuver icons | Right turn, then left turn |
| Arrival time | 13:34 |
| Remaining duration | 1:13 h, meaning 73 minutes |
| Remaining distance | 138 km |
| Phone status clock | 12:21 |

English controls coexist with Hebrew street names, including שמריהו לוין and אליהו ספיר. The prototype's [ML Kit Latin recognizer](https://developers.google.com/ml-kit/vision/text-recognition/v2/languages) does not provide Hebrew-script support. Hebrew names and icon meanings require additional extraction work before frame deletion; saved text cannot recover those pixels later.

The parser associates numbers and units using geometry produced by complete-frame OCR. It requires a coherent arrival/duration/distance panel and rejects ambiguous associations. Keep the phone clock, map distances and speed-limit numbers separate from navigation readings. A displayed zero is a candidate speed observation; missing speed remains unavailable.

Tests use manually transcribed text and approximate coordinates at multiple portrait sizes. They verify parser behavior; [phone validation](capture-validation.md) must establish actual OCR accuracy, layout variations and icon/name support. Frame handling follows [capture](capture.md).
