package com.sentinel.core.memory

import com.sentinel.core.memory.entity.DocumentEmbedding
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorMathUtilsTest {

    @Test
    fun testFloatArrayToByteArrayAndBack() {
        val original = floatArrayOf(0.1f, -0.5f, 3.14f, 0.0f)
        val byteArray = VectorMathUtils.floatArrayToByteArray(original)
        val result = VectorMathUtils.byteArrayToFloatArray(byteArray)

        assertTrue(original.contentEquals(result))
    }

    @Test
    fun testCosineSimilarityIdenticalVectors() = runBlocking {
        val query = floatArrayOf(1.0f, 0.0f, 0.0f)
        val doc1 = DocumentEmbedding(
            id = 1,
            uri = "uri1",
            filename = "file1",
            fileType = "type",
            embedding = VectorMathUtils.floatArrayToByteArray(floatArrayOf(1.0f, 0.0f, 0.0f))
        )

        val matches = VectorMathUtils.findTopMatches(query, listOf(doc1), 1)
        assertEquals(1, matches.size)
        assertEquals(1.0f, matches[0].second, 0.001f)
    }

    @Test
    fun testCosineSimilarityOrthogonalVectors() = runBlocking {
        val query = floatArrayOf(1.0f, 0.0f, 0.0f)
        val doc1 = DocumentEmbedding(
            id = 1,
            uri = "uri1",
            filename = "file1",
            fileType = "type",
            embedding = VectorMathUtils.floatArrayToByteArray(floatArrayOf(0.0f, 1.0f, 0.0f))
        )

        val matches = VectorMathUtils.findTopMatches(query, listOf(doc1), 1)
        assertEquals(1, matches.size)
        assertEquals(0.0f, matches[0].second, 0.001f)
    }
}
