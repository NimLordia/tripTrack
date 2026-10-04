# Ride analysis

Inspect useful collected telemetry along selected route sections or time intervals, beyond speed alone. Detailed views are for long stops or after a trip.

Working metric definitions; labels and uncertain-interval treatment remain open:

| Metric | Definition |
| --- | --- |
| Elapsed time | Recording finish minus start; pause semantics open |
| Moving/stopped time | Intervals supported by movement/stationary evidence |
| Adjusted time | Elapsed time minus selected stop exclusions |
| Distance | Recorded path distance after rejecting bad measurements |
| Overall average | Total distance / elapsed time |
| Moving average | Moving-interval distance / moving time |
| Adjusted average | Ride distance / adjusted time |
| Recorded top speed | Highest credible recorded speed after filtering |

Use physics and surrounding readings to flag spikes, position jumps and implausible changes. Preserve original evidence alongside filtered values. Validate peaks across available sources without silently averaging them. Source agreement can share underlying errors; special handling is explicitly deferred until observed.

Open: source priority, fusion, quality thresholds, charts, gaps in statistics and derived telemetry. [Stops](stops.md) owns exclusions; [recording](recording.md) owns timestamp alignment. Larger-dataset improvements are [later work](../later/index.md).
