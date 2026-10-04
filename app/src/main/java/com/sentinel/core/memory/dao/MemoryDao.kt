package com.sentinel.core.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sentinel.core.memory.entity.MemoryFact

@Dao
interface MemoryDao {
    @Insert
    suspend fun insertFact(fact: MemoryFact)

    @Query("SELECT * FROM memory_facts WHERE factKey LIKE '%' || :query || '%' OR factValue LIKE '%' || :query || '%'")
    suspend fun searchMemory(query: String): List<MemoryFact>
}
