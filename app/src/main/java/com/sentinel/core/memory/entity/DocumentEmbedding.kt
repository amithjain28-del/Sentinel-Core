package com.sentinel.core.memory.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_embeddings")
data class DocumentEmbedding(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val filename: String,
    val fileType: String, // e.g. "image/jpeg" or "application/pdf"
    val timestamp: Long = System.currentTimeMillis(),
    val textContent: String = "",
    val embedding: ByteArray // Stored as ByteArray representation of FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentEmbedding

        if (id != other.id) return false
        if (uri != other.uri) return false
        if (filename != other.filename) return false
        if (fileType != other.fileType) return false
        if (timestamp != other.timestamp) return false
        if (textContent != other.textContent) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + uri.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + textContent.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
