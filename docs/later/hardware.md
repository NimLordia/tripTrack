# External hardware research

Later only; no selection or purchase. Add timestamped external readings to the shared source timeline. Historical official USD prices checked 2026-10-04; recheck availability and destination prices. Shipping, taxes and import costs excluded.

| Candidate | Price | Capabilities and work |
| --- | ---: | --- |
| [RaceBox Mini S](https://www.racebox.pro/products/racebox-mini-s) | $289 | GPS, accelerometer, gyro, Bluetooth, battery, storage; evaluate secure bike mounting |
| [RaceBox Micro](https://www.racebox.pro/products/racebox-micro) | $129 | GPS, motion sensors, Bluetooth, storage; supply protected power, weatherproofing and mount |
| Custom recorder | $107.35 | Named parts below; assembly, firmware and integration untested |

[Mini S specifications](https://www.racebox.pro/products/racebox-mini/tech-specs): 25 Hz GPS, stated battery over 20 hours, splash resistance and mounting thread. Storage: approximately 130 minutes at 25 Hz or 325 minutes at 10 Hz; long trips need streaming/offloading. Store then listed dispatch after October 9, 2026.

[RaceBox protocol](https://www.racebox.pro/products/mini-micro-protocol-documentation) exposes speed/location, accuracy, acceleration and rotation up to 25 combined messages/second; internal sensor frequency is separate.

[Micro manual](https://www.racebox.pro/info/racebox-micro-user-manual): 3.5–16 V input, lower voltage such as 5 V preferred thermally. Its supplied plastic case is [not waterproof](https://www.racebox.pro/info/faq).

Research recommendation: Mini S as benchmark; Micro for installation-cost comparison; custom hardware if needed capabilities are missing.

| Custom parts | USD |
| --- | ---: |
| [ESP32 Feather V2, Bluetooth/charger](https://www.adafruit.com/product/5400) | 19.95 |
| [GPS FeatherWing, antenna, up to 10 Hz](https://www.adafruit.com/product/3133) | 24.95 |
| [ISM330DHCX accelerometer/gyro](https://www.adafruit.com/product/4502) | 19.95 |
| [MicroSD interface](https://www.adafruit.com/product/2922) | 8.95 |
| [512 MB microSD](https://www.adafruit.com/product/5252) | 4.95 |
| [Protected 1200 mAh LiPo](https://www.adafruit.com/product/258) | 9.95 |
| [IP65 enclosure/cable glands](https://www.adafruit.com/product/3931) | 9.95 |
| [Two stacking-header sets](https://www.adafruit.com/product/2830) | 2.50 |
| [Motion-sensor cable](https://www.adafruit.com/product/4401) | 1.25 |
| [USB programming/charging cable](https://www.adafruit.com/product/4474) | 4.95 |
| **Subtotal** | **107.35** |

Excludes consumables, internal fasteners, bike mount/tether and extra power hardware. Validate compatibility, stacking clearance, enclosure fit, vibration and battery endurance before purchasing. This is a research BOM, not a tested design.
