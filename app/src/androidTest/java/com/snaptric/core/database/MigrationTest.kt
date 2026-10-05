package com.snaptric.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that every schema version migrates to the current one without losing data.
 * Add a test here whenever the database version goes up.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SnaptricDatabase::class.java
    )

    @Test
    fun migrate1To2_keepsPropertiesMetersAndReadings() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO properties (id, name, address, iconIdentifier) VALUES (1, 'Home', NULL, 'home')")
            execSQL("INSERT INTO utility (id, propertyId, type, unit, initialReading, name) VALUES (1, 1, 'ELECTRICITY', 'kWh', 0.0, 'Main')")
            execSQL("INSERT INTO readings (id, utilityId, value, timestamp, source) VALUES (1, 1, 1262.4, 1000, 'MLKit')")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 2, true).close()

        // Open with Room itself to check the data through the app's own DAO.
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SnaptricDatabase::class.java,
            dbName
        ).addMigrations(MIGRATION_2_3).build()
        try {
            runBlocking {
                val dao = db.meterDao()
                assertEquals("Home", dao.getAllProperties().first().single().name)
                val meter = dao.getUtilitiesForProperty(1).first().single()
                assertEquals("Main", meter.name)
                assertNull(meter.serialNumber)
                assertEquals(1262.4, dao.getReadingsForUtility(1).first().single().value, 0.0)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate2To3_addsTariffsAndNormalizesUnits() {
        helper.createDatabase(dbName, 2).apply {
            execSQL("INSERT INTO properties (id, name, address, iconIdentifier) VALUES (1, 'Home', NULL, 'home')")
            execSQL("INSERT INTO utility (id, propertyId, type, unit, initialReading, name, serialNumber) VALUES (1, 1, 'GAS', 'm3', 0.0, NULL, NULL)")
            execSQL("INSERT INTO utility (id, propertyId, type, unit, initialReading, name, serialNumber) VALUES (2, 1, 'ELECTRICITY', '', 0.0, NULL, NULL)")
            execSQL("INSERT INTO utility (id, propertyId, type, unit, initialReading, name, serialNumber) VALUES (3, 1, 'WATER', 'Liters', 0.0, NULL, NULL)")
            execSQL("INSERT INTO utility (id, propertyId, type, unit, initialReading, name, serialNumber) VALUES (4, 1, 'GAS', 'units', 0.0, NULL, NULL)")
            execSQL("INSERT INTO readings (id, utilityId, value, timestamp, source) VALUES (1, 1, 152.5, 1000, 'MLKit')")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).close()

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SnaptricDatabase::class.java,
            dbName
        ).addMigrations(MIGRATION_2_3).build()
        try {
            runBlocking {
                val dao = db.meterDao()
                val units = dao.getAllUtilities().first().associate { it.id to it.unit }
                assertEquals(mapOf(1L to "m³", 2L to "kWh", 3L to "Liters", 4L to "units"), units)
                // Raw readings are untouched.
                assertEquals(152.5, dao.getReadingsForUtility(1).first().single().value, 0.0)
                assertEquals(emptyList<Any>(), dao.getAllTariffs().first())
            }
        } finally {
            db.close()
        }
    }
}
