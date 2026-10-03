package com.sentinel.core.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import kotlin.math.sqrt

// Mock structure for Neural Search Vector Entity
data class MediaEmbedding(
    val uri: String,
    val vector: FloatArray,
    val textContent: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaEmbedding

        if (uri != other.uri) return false
        if (!vector.contentEquals(other.vector)) return false
        if (textContent != other.textContent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + textContent.hashCode()
        return result
    }
}

class OmnimodalSearchEngine(private val context: Context) {

    private val embeddingsDb = mutableListOf<MediaEmbedding>()

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun scanAndEmbedAllMedia() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                // Here we would run the local ONNX CLIP model on each image
                // and store the resulting embedding vector in Room DB.
                // For now, we mock the embedding process to unblock execution.
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val imageUri = Uri.withAppendedPath(uri, id.toString())
                embeddingsDb.add(MediaEmbedding(imageUri.toString(), floatArrayOf(0.1f, 0.2f), "Image $id"))
            }
        }
    }

    fun extractPdfText(uri: Uri): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            val pdfStripper = PDFTextStripper()
            val text = pdfStripper.getText(document)
            document.close()

            embeddingsDb.add(MediaEmbedding(uri.toString(), floatArrayOf(0.5f, 0.5f), text))
            text
        } catch (e: Exception) {
            "Error extracting PDF: ${e.message}"
        }
    }

    fun search(queryVector: FloatArray): MediaEmbedding? {
        // In-memory Cosine Similarity
        var bestMatch: MediaEmbedding? = null
        var maxSimilarity = -1.0f

        for (embedding in embeddingsDb) {
            val sim = cosineSimilarity(queryVector, embedding.vector)
            if (sim > maxSimilarity) {
                maxSimilarity = sim
                bestMatch = embedding
            }
        }
        return bestMatch
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        return if (norm1 == 0.0f || norm2 == 0.0f) 0.0f else dotProduct / (sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())).toFloat()
    }

    fun openCamera() {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        // ReAct Engine will take over from here to visually click the shutter button
    }
}
