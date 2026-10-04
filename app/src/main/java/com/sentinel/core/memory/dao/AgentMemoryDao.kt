package com.sentinel.core.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sentinel.core.memory.entity.DocumentEmbedding

@Dao
interface AgentMemoryDao {
    @Insert
    suspend fun insertDocument(doc: DocumentEmbedding)

    @Query("SELECT * FROM document_embeddings WHERE uri = :uri")
    suspend fun getDocumentByUri(uri: String): DocumentEmbedding?

    @Query("SELECT * FROM document_embeddings")
    suspend fun getAllDocuments(): List<DocumentEmbedding>
}
