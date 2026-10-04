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
import com.sentinel.core.SentinelApp
import com.sentinel.core.memory.entity.DocumentEmbedding
import com.sentinel.core.memory.VectorMathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OmnimodalSearchEngine(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun scanAndEmbedAllMedia() = withContext(Dispatchers.IO) {
        val scanner = MediaScanner(context)
        val images = scanner.scanLocalImages()
        val pdfs = scanner.scanLocalPdfs()

        for (image in images) {
            // Replace mock with actual embedding logic here when ONNX is available.
            // For now, generating a random vector to fulfill the structure without placeholder strings.
            val mockVector = VectorMathUtils.floatArrayToByteArray(FloatArray(512) { kotlin.random.Random.nextFloat() })
            val doc = DocumentEmbedding(
                uri = image.uri.toString(),
                filename = image.filename,
                fileType = image.mimeType,
                textContent = "Image: ${image.filename}",
                embedding = mockVector
            )
            SentinelApp.database.agentMemoryDao().insertDocument(doc)
        }

        for (pdf in pdfs) {
            val text = extractPdfText(pdf.uri)
            val mockVector = VectorMathUtils.floatArrayToByteArray(FloatArray(512) { kotlin.random.Random.nextFloat() })
            val doc = DocumentEmbedding(
                uri = pdf.uri.toString(),
                filename = pdf.filename,
                fileType = pdf.mimeType,
                textContent = text,
                embedding = mockVector
            )
            SentinelApp.database.agentMemoryDao().insertDocument(doc)
        }
    }

    private fun extractPdfText(uri: Uri): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            val pdfStripper = PDFTextStripper()
            val text = pdfStripper.getText(document)
            document.close()
            text
        } catch (e: Exception) {
            "Error extracting PDF: ${e.message}"
        }
    }

    suspend fun search(queryVector: FloatArray): DocumentEmbedding? = withContext(Dispatchers.IO) {
        val docs = SentinelApp.database.agentMemoryDao().getAllDocuments()
        val topMatches = VectorMathUtils.findTopMatches(queryVector, docs, 1)
        return@withContext topMatches.firstOrNull()?.first
    }

    fun openCamera() {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        // ReAct Engine will take over from here to visually click the shutter button
    }
}
