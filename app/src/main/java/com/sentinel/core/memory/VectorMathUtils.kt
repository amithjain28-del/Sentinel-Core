package com.sentinel.core.memory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sentinel.core.memory.entity.DocumentEmbedding
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object VectorMathUtils {

    suspend fun findTopMatches(
        queryVector: FloatArray,
        documents: List<DocumentEmbedding>,
        topK: Int = 3
    ): List<Pair<DocumentEmbedding, Float>> = withContext(Dispatchers.Default) {
        documents.map { doc ->
            val docVector = byteArrayToFloatArray(doc.embedding)
            val score = cosineSimilarity(queryVector, docVector)
            Pair(doc, score)
        }
        .filter { !it.second.isNaN() }
        .sortedByDescending { it.second }
        .take(topK)
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0.0f

        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f

        for (i in v1.indices) {
            val a = v1[i]
            val b = v2[i]
            dotProduct += a * b
            norm1 += a * a
            norm2 += b * b
        }

        val denominator = sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())
        return if (denominator == 0.0) 0.0f else (dotProduct / denominator).toFloat()
    }

    fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floatArray.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floatArray) {
            buffer.putFloat(f)
        }
        return buffer.array()
    }

    fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN)
        val floatArray = FloatArray(byteArray.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = buffer.float
        }
        return floatArray
    }
}
