package com.triptrack.app.capture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TemporaryFrameStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun recoveryDeletesOnlyAbandonedOwnedFrames() {
        val cache = temporaryFolder.newFolder("cache")
        val captureDirectory = File(cache, "triptrack-capture").apply { mkdir() }
        val abandoned = File(captureDirectory, "frame-interrupted.png").apply { writeText("pixels") }
        val unrelated = File(captureDirectory, "other.png").apply { writeText("other") }
        val nested = File(captureDirectory, "frame-directory.png").apply { mkdir() }
        val nestedContent = File(nested, "frame-nested.png").apply { writeText("other") }
        val outside = File(cache, "frame-outside.png").apply { writeText("other") }
        val store = TemporaryFrameStore(cache)
        val active = store.create()

        TemporaryFrameStore.cleanup(cache)

        assertFalse(abandoned.exists())
        assertTrue(active.exists())
        assertTrue(unrelated.exists())
        assertTrue(nestedContent.exists())
        assertTrue(outside.exists())
        store.delete(active)
        assertFalse(active.exists())
    }
}
