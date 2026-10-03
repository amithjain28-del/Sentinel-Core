package com.sentinel.core

import android.app.Application
import androidx.room.Room
import com.sentinel.core.memory.AppDatabase
import com.sentinel.core.memory.MIGRATION_1_2

class SentinelApp : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "sentinel-memory-db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }
}
