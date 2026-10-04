# Navigation integration

Historical research: 2026-10-04. Reverify access, billing and terms before implementation.

The user selected Navigation Connect's 60-second baseline, then requested alternatives and all-source comparison. Its final necessity remains subject to the prototype. This cadence does not control phone sampling or OCR.

- [Setup](https://developers.google.com/maps/documentation/navigation/connect/setup-api?setupProd=enable) requires backend integration, app verification and user consent around launched navigation sessions. It is not an offline feed.
- [Configuration](https://developers.google.com/maps/documentation/navigation/connect/create-trip): five-second updates require Enterprise usage. No exact public price was verified; Navigation SDK pricing belongs to a different product.
- [API response](https://developers.google.com/maps/documentation/navigation/connect/reference/rest/v1/projects.trips): location, timestamps and trip progress; no instantaneous driver-speed/top-speed field. Traffic categories and itinerary stops are not rider measurements.
- [Terms §11.8](https://cloud.google.com/maps-platform/terms/maps-service-terms): prior research found a 30-day cache restriction for specified location, distance, duration, time and ETA values, including local/database copies.
- [Android speed](https://developer.android.com/reference/android/location/Location#getSpeed()) needs no five-second API purchase. Requested cadence is best effort; passive location is an experiment, not access to Waze's internal data.

Open: retain this integration or use alternatives; backend delivery; source-specific deletion. Faster updates stay in [later review](../later/index.md).
