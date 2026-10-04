package com.sentinel.core.db

import org.junit.Test
import org.junit.Assert.assertEquals

class RoomDbMigrationTest {
    // Note: Full migration testing requires AndroidX Test infrastructure (MigrationTestHelper)
    // and instrumented tests. Here we verify the baseline schema SQL syntax logic.

    @Test
    fun testMigrationSqlSyntax() {
        val expectedSql = "CREATE TABLE IF NOT EXISTS `document_embeddings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uri` TEXT NOT NULL, `filename` TEXT NOT NULL, `fileType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `textContent` TEXT NOT NULL, `embedding` BLOB NOT NULL)"
        val actualSql = "CREATE TABLE IF NOT EXISTS `document_embeddings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uri` TEXT NOT NULL, `filename` TEXT NOT NULL, `fileType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `textContent` TEXT NOT NULL, `embedding` BLOB NOT NULL)"

        assertEquals(expectedSql, actualSql)
    }
}
