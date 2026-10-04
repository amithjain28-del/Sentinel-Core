package com.sentinel.core.memory

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `document_embeddings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uri` TEXT NOT NULL, `filename` TEXT NOT NULL, `fileType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `textContent` TEXT NOT NULL, `embedding` BLOB NOT NULL)"
        )
    }
}
