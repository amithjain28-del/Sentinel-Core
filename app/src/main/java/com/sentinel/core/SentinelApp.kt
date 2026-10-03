package com.sentinel.core

import android.app.Application
import androidx.room.Room
import com.sentinel.core.memory.AppDatabase

class SentinelApp : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "sentinel-memory-db"
        ).build()
    }
}
