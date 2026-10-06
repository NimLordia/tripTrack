package com.triptrack.app.capture

import android.graphics.Bitmap
import android.graphics.Rect
import com.triptrack.app.model.WazeTextElement
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FrameTextRecognizer : AutoCloseable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): RecognizedFrameText = withContext(NonCancellable) {
        // ML Kit's Task cannot cancel recognition. Wait for its completion even
        // after capture cancellation so finally may safely release the bitmap.
        suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { text ->
                    try {
                        val blocks = JSONArray()
                        val recognizedElements = mutableListOf<WazeTextElement>()
                        for (block in text.textBlocks) {
                            val lines = JSONArray()
                            for (line in block.lines) {
                                val elements = JSONArray()
                                for (element in line.elements) {
                                    elements.put(textGeometry(element.text, element.boundingBox))
                                    element.boundingBox?.let { bounds ->
                                        recognizedElements += WazeTextElement(element.text,
                                            bounds.left, bounds.top, bounds.right, bounds.bottom)
                                    }
                                }
                                lines.put(textGeometry(line.text, line.boundingBox).put("elements", elements))
                            }
                            blocks.put(textGeometry(block.text, block.boundingBox).put("lines", lines))
                        }
                        continuation.resume(RecognizedFrameText(text.text, blocks.toString(),
                            recognizedElements.toList()))
                    } catch (failure: Exception) {
                        continuation.resumeWithException(failure)
                    }
                }
                .addOnFailureListener { failure -> continuation.resumeWithException(failure) }
                .addOnCanceledListener { continuation.cancel() }
        }
    }

    override fun close() = recognizer.close()

    private fun textGeometry(text: String, bounds: Rect?): JSONObject = JSONObject()
        .put("text", text)
        .put("boundingBox", bounds?.let {
            JSONObject().put("left", it.left).put("top", it.top)
                .put("right", it.right).put("bottom", it.bottom)
        } ?: JSONObject.NULL)
}

internal data class RecognizedFrameText(
    val text: String,
    val textBlocksJson: String,
    val elements: List<WazeTextElement>,
)
