package com.triptrack.app.capture

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image

internal fun Image.toFullScreenBitmap(): Bitmap {
    require(format == PixelFormat.RGBA_8888 && planes.size == 1)
    require(cropRect.left == 0 && cropRect.top == 0 &&
        cropRect.right == width && cropRect.bottom == height) {
        "A complete display frame is required"
    }
    val plane = planes.single()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val row = IntArray(width)
    try {
        for (y in 0 until height) {
            readRgbaRow(plane.buffer, y, width, plane.rowStride, plane.pixelStride, row)
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }
        return bitmap
    } catch (failure: Throwable) {
        bitmap.recycle()
        throw failure
    } finally {
        row.fill(0)
    }
}
