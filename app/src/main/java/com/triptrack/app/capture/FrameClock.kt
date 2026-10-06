package com.triptrack.app.capture

internal data class FrameClockSnapshot(
    val monotonicNanos: Long,
    val elapsedRealtimeNanos: Long,
    val epochMillis: Long,
)

internal data class FrameTime(
    val epochMillis: Long,
    val elapsedRealtimeNanos: Long,
)

/**
 * AOSP VirtualDisplaySurface supplies systemTime() / CLOCK_MONOTONIC timestamps.
 * That clock excludes suspend, unlike elapsedRealtimeNanos. Map both clocks at
 * receipt, retaining the frame's delta; never replace the frame time with receipt.
 * Image's public contract does not guarantee an OEM producer's clock. Reject
 * implausible timestamps and retain the original timestamp for device validation.
 * Primary Android 16 source references:
 * android.googlesource.com/platform/frameworks/native/+/refs/heads/android16-release/services/surfaceflinger/DisplayHardware/VirtualDisplaySurface.cpp
 * android.googlesource.com/platform/system/core/+/refs/heads/android16-release/libutils/include/utils/Timers.h
 * android.googlesource.com/platform/libcore/+/refs/heads/android16-release/ojluni/src/main/native/System.c
 */
internal fun mapFrameTime(
    sourceTimestampNanos: Long,
    snapshot: FrameClockSnapshot,
): FrameTime? {
    if (sourceTimestampNanos <= 0 || sourceTimestampNanos > snapshot.monotonicNanos) return null
    val ageNanos = snapshot.monotonicNanos - sourceTimestampNanos
    // This bounds stale/foreign-clock frames; it is not a requested capture rate.
    if (ageNanos > 5_000_000_000L || ageNanos >= snapshot.elapsedRealtimeNanos) return null
    return FrameTime(
        epochMillis = snapshot.epochMillis - ageNanos / 1_000_000L,
        elapsedRealtimeNanos = snapshot.elapsedRealtimeNanos - ageNanos,
    )
}
