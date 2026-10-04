package com.sentinel.core.memory

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sentinel.core.memory.dao.AgentMemoryDao
import com.sentinel.core.memory.dao.MemoryDao
import com.sentinel.core.memory.entity.DocumentEmbedding
import com.sentinel.core.memory.entity.MemoryFact

@Database(entities = [MemoryFact::class, DocumentEmbedding::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun agentMemoryDao(): AgentMemoryDao
}
