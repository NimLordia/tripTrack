package com.triptrack.app.capture

import java.io.File
import java.io.IOException

/** Owns only generated PNGs directly inside cache/triptrack-capture. */
internal class TemporaryFrameStore(cacheDirectory: File) {
    private val directory = ownedDirectory(cacheDirectory)

    fun create(): File = synchronized(lock) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Cannot create temporary capture directory")
        }
        File.createTempFile("frame-", ".png", directory).also {
            activeFiles.add(it.canonicalPath)
        }
    }

    fun delete(frame: File) = synchronized(lock) {
        check(frame.canonicalFile.parentFile == directory)
        check(frame.name.startsWith("frame-") && frame.extension == "png")
        try {
            if (frame.exists() && !frame.delete()) {
                throw IOException("Cannot delete temporary capture frame")
            }
        } finally {
            // A failed deletion is abandoned, not actively used. The next
            // recovery must retry it rather than skip it as an active file.
            activeFiles.remove(frame.canonicalPath)
        }
        Unit
    }

    companion object {
        private val lock = Any()
        private val activeFiles = mutableSetOf<String>()

        private fun ownedDirectory(cacheDirectory: File): File {
            val cache = cacheDirectory.canonicalFile
            val expected = File(cache, "triptrack-capture").absoluteFile
            val owned = expected.canonicalFile
            check(owned == expected && owned.parentFile == cache) {
                "Capture cache must be the owned directory directly inside app cache"
            }
            return owned
        }

        /** Also called on process startup, when no previous in-memory owner survives. */
        fun cleanup(cacheDirectory: File) = synchronized(lock) {
            val directory = ownedDirectory(cacheDirectory)
            directory.listFiles()?.forEach { frame ->
                if (frame.isFile && frame.name.startsWith("frame-") && frame.extension == "png" &&
                    frame.canonicalFile.parentFile == directory &&
                    frame.canonicalPath !in activeFiles && !frame.delete()) {
                    throw IOException("Cannot remove interrupted temporary capture frame")
                }
            }
        }
    }
}
