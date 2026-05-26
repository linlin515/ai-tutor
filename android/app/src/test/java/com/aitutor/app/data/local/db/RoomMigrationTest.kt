package com.aitutor.app.data.local.db

import androidx.room.migration.Migration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated test to verify Room database schema versioning and migration integrity.
 *
 * These tests run at compile/unit level and catch version drift early.
 *
 * NOTE: Room's @Database annotation has RetentionPolicy.CLASS, so it is NOT
 * accessible via reflection in JVM unit tests. Version assertions use the
 * CURRENT_DB_VERSION constant — update it when bumping the database version.
 */
class RoomMigrationTest {

    companion object {
        /**
         * Must match @Database(version = ...) in AiTutorDatabase.kt.
         * Update this when adding new migrations.
         */
        private const val CURRENT_DB_VERSION = 6
    }

    // ── Version constants ──────────────────────────────────────────────

    /**
     * Verifies the current database version constant matches our expectation.
     * Bump this test when you bump the database version and add a Migration.
     */
    @Test
    fun databaseVersion_shouldBeExpected() {
        assertEquals(
            "Database version constant mismatch — add a new Migration and " +
                "update CURRENT_DB_VERSION",
            6,
            CURRENT_DB_VERSION
        )
    }

    // ── Migration integrity ────────────────────────────────────────────

    @Test
    fun migration1_2_shouldHaveCorrectRange() {
        assertMigrationRange(MIGRATION_1_2, 1, 2)
    }

    @Test
    fun migration2_3_shouldHaveCorrectRange() {
        assertMigrationRange(MIGRATION_2_3, 2, 3)
    }

    @Test
    fun migration3_4_shouldHaveCorrectRange() {
        assertMigrationRange(MIGRATION_3_4, 3, 4)
    }

    @Test
    fun migration4_5_shouldHaveCorrectRange() {
        assertMigrationRange(MIGRATION_4_5, 4, 5)
    }

    @Test
    fun migration5_6_shouldHaveCorrectRange() {
        assertMigrationRange(MIGRATION_5_6, 5, 6)
    }

    @Test
    fun allMigrations_shouldFormUnbrokenChain() {
        val migrations = listOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6
        ).sortedBy { it.startVersion }
        var expectedStart = 1
        for (m in migrations) {
            assertEquals(
                "Migration chain gap: expected startVersion=$expectedStart",
                expectedStart,
                m.startVersion
            )
            assertEquals(
                "Migration ${m.startVersion}→${m.endVersion} is a no-op",
                m.startVersion + 1,
                m.endVersion
            )
            expectedStart = m.endVersion
        }
        assertEquals(
            "Migration chain does not reach the current database version ($CURRENT_DB_VERSION). " +
                "Add a new Migration to cover the gap.",
            CURRENT_DB_VERSION,
            expectedStart
        )
    }

    @Test
    fun noMigration_shouldBeSkippable() {
        val migrations = listOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6
        )
        for (m in migrations) {
            assertTrue(
                "Migration ${m.startVersion}→${m.endVersion} is trivial " +
                    "(startVersion must be < endVersion)",
                m.startVersion < m.endVersion
            )
        }
    }

    // ── Export schema check ────────────────────────────────────────────

    @Test
    fun schemaJsonFiles_shouldExist() {
        val expectedFiles = (1..CURRENT_DB_VERSION).map { "$it.json" }
        val schemaDir = "schemas/com.aitutor.app.data.local.db.AiTutorDatabase/"
        for (file in expectedFiles) {
            val f = java.io.File(schemaDir + file)
            assertTrue(
                "Schema JSON $schemaDir$file not found. " +
                    "Run a build with exportSchema=true to generate it.",
                f.exists()
            )
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun assertMigrationRange(
        migration: Migration,
        expectedStart: Int,
        expectedEnd: Int
    ) {
        assertEquals(
            "MIGRATION_${expectedStart}_${expectedEnd} startVersion",
            expectedStart,
            migration.startVersion
        )
        assertEquals(
            "MIGRATION_${expectedStart}_${expectedEnd} endVersion",
            expectedEnd,
            migration.endVersion
        )
    }
}
