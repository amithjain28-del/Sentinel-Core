package com.sentinel.core.memory.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_facts")
data class MemoryFact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val factKey: String,
    val factValue: String,
    val timestamp: Long = System.currentTimeMillis()
)
