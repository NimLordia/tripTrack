package com.triptrack.app.capture

import java.nio.ByteBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class RgbaPixelsTest {
    @Test fun preservesAllDisplayEdgesWithoutIncludingRowPadding() {
        // Three actual pixels per row, four padding bytes between rows. The
        // producer need not expose padding after the last actual pixel.
        val bytes = byteArrayOf(
            1, 2, 3, -1, 4, 5, 6, -1, 7, 8, 9, -1, 99, 99, 99, 99,
            10, 11, 12, -1, 13, 14, 15, -1, 16, 17, 18, -1,
        )
        val first = IntArray(3)
        val last = IntArray(3)
        val buffer = ByteBuffer.wrap(bytes)
        readRgbaRow(buffer, 0, 3, 16, 4, first)
        readRgbaRow(buffer, 1, 3, 16, 4, last)
        assertArrayEquals(intArrayOf(0xff010203.toInt(), 0xff040506.toInt(), 0xff070809.toInt()), first)
        assertArrayEquals(intArrayOf(0xff0a0b0c.toInt(), 0xff0d0e0f.toInt(), 0xff101112.toInt()), last)
    }

    @Test fun respectsPixelStrideWithoutSelectingARegion() {
        val bytes = byteArrayOf(1, 2, 3, -1, 99, 99, 4, 5, 6, -1, 99, 99)
        val pixels = IntArray(2)
        readRgbaRow(ByteBuffer.wrap(bytes), 0, 2, 12, 6, pixels)
        assertArrayEquals(intArrayOf(0xff010203.toInt(), 0xff040506.toInt()), pixels)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnIncompleteLastPixel() {
        readRgbaRow(ByteBuffer.wrap(ByteArray(7)), 0, 2, 8, 4, IntArray(2))
    }
}
