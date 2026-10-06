package com.triptrack.app.capture

import java.nio.ByteBuffer

/** Reads every visible pixel in a row; row/pixel padding is not display content. */
internal fun readRgbaRow(
    buffer: ByteBuffer,
    row: Int,
    width: Int,
    rowStride: Int,
    pixelStride: Int,
    output: IntArray,
) {
    require(row >= 0 && width > 0 && output.size >= width)
    require(pixelStride >= 4 && rowStride.toLong() >= (width - 1L) * pixelStride + 4L)
    val rowStart = row.toLong() * rowStride
    val lastByte = rowStart + (width - 1L) * pixelStride + 3L
    require(lastByte < buffer.limit()) { "Incomplete full-screen RGBA row" }
    for (x in 0 until width) {
        val offset = (rowStart + x.toLong() * pixelStride).toInt()
        val red = buffer.get(offset).toInt() and 0xff
        val green = buffer.get(offset + 1).toInt() and 0xff
        val blue = buffer.get(offset + 2).toInt() and 0xff
        val alpha = buffer.get(offset + 3).toInt() and 0xff
        output[x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
