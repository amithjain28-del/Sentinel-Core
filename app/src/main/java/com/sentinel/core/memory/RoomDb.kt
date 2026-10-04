package com.sentinel.core.memory

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase

@Entity(tableName = "memory_facts")
data class MemoryFact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val factKey: String,
    val factValue: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MemoryDao {
    @Insert
    suspend fun insertFact(fact: MemoryFact)

    @Query("SELECT * FROM memory_facts WHERE factKey LIKE '%' || :query || '%' OR factValue LIKE '%' || :query || '%'")
    suspend fun searchMemory(query: String): List<MemoryFact>
}

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

@Dao
interface AgentMemoryDao {
    @Insert
    suspend fun insertDocument(doc: DocumentEmbedding)

    @Query("SELECT * FROM document_embeddings WHERE uri = :uri")
    suspend fun getDocumentByUri(uri: String): DocumentEmbedding?

    @Query("SELECT * FROM document_embeddings")
    suspend fun getAllDocuments(): List<DocumentEmbedding>
}

@Database(entities = [MemoryFact::class, DocumentEmbedding::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun agentMemoryDao(): AgentMemoryDao
}
