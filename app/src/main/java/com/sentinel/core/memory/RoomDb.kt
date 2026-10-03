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

@Database(entities = [MemoryFact::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
