package com.snaptric.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.snaptric.core.database.SnaptricDatabase
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeterDaoTest {

    private lateinit var db: SnaptricDatabase
    private lateinit var dao: MeterDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, SnaptricDatabase::class.java).build()
        dao = db.meterDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertProperty_and_getAllProperties() = runTest {
        val home = PropertyEntity(name = "Home", address = "123 Main St", iconIdentifier = "home")
        val office = PropertyEntity(name = "Office", address = null, iconIdentifier = "business")

        dao.insertProperty(home)
        dao.insertProperty(office)

        dao.getAllProperties().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Home", result[0].name)
            assertEquals("Office", result[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllProperties_returnsEmptyWhenNoData() = runTest {
        dao.getAllProperties().test {
            assertEquals(emptyList<PropertyEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertUtility_and_getUtilitiesForProperty() = runTest {
        dao.insertProperty(PropertyEntity(name = "Home", address = null))
        val properties = dao.getAllProperties().first()
        val propertyId = properties.first().id

        val electricity = UtilityEntity(
            propertyId = propertyId,
            type = UtilityType.ELECTRICITY,
            unit = "kWh",
            initialReading = 100.0,
            name = "Main Meter"
        )
        val water = UtilityEntity(
            propertyId = propertyId,
            type = UtilityType.WATER,
            unit = "m3",
            initialReading = 50.0,
            name = null
        )

        dao.insertUtility(electricity)
        dao.insertUtility(water)

        dao.getUtilitiesForProperty(propertyId).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals(UtilityType.ELECTRICITY, result[0].type)
            assertEquals(UtilityType.WATER, result[1].type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getUtilitiesForProperty_returnsOnlyForThatProperty() = runBlocking {
        dao.insertProperty(PropertyEntity(name = "Home", address = null))
        dao.insertProperty(PropertyEntity(name = "Office", address = null))
        val allProps = dao.getAllProperties().first()
        val homeId = allProps.first { it.name == "Home" }.id
        val officeId = allProps.first { it.name == "Office" }.id

        dao.insertUtility(UtilityEntity(propertyId = homeId, type = UtilityType.GAS, unit = "m3", initialReading = 10.0))
        dao.insertUtility(UtilityEntity(propertyId = officeId, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 20.0))

        dao.getUtilitiesForProperty(homeId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(UtilityType.GAS, result.first().type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertReading_and_getReadingsForUtility_orderedByTimestampDesc() = runTest {
        dao.insertProperty(PropertyEntity(name = "Home", address = null))
        val propertyId = dao.getAllProperties().first().first().id
        dao.insertUtility(UtilityEntity(propertyId = propertyId, type = UtilityType.WATER, unit = "Liters", initialReading = 0.0))
        val utilityId = dao.getUtilitiesForProperty(propertyId).first().first().id

        val reading1 = ReadingEntity(utilityId = utilityId, value = 100.0, timestamp = 1000L, source = "Manual")
        val reading2 = ReadingEntity(utilityId = utilityId, value = 200.0, timestamp = 2000L, source = "MLKit")
        val reading3 = ReadingEntity(utilityId = utilityId, value = 150.0, timestamp = 1500L, source = "Gemma")

        dao.insertReading(reading1)
        dao.insertReading(reading2)
        dao.insertReading(reading3)

        dao.getReadingsForUtility(utilityId).test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals(200.0, result[0].value, 0.0) // newest first (timestamp 2000)
            assertEquals(150.0, result[1].value, 0.0) // timestamp 1500
            assertEquals(100.0, result[2].value, 0.0) // oldest (timestamp 1000)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllReadings_returnsAcrossAllUtilities() = runTest {
        dao.insertProperty(PropertyEntity(name = "Home", address = null))
        val propId = dao.getAllProperties().first().first().id
        dao.insertUtility(UtilityEntity(propertyId = propId, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0))
        dao.insertUtility(UtilityEntity(propertyId = propId, type = UtilityType.GAS, unit = "m3", initialReading = 0.0))
        val utils = dao.getUtilitiesForProperty(propId).first()
        val elecId = utils.first { it.type == UtilityType.ELECTRICITY }.id
        val gasId = utils.first { it.type == UtilityType.GAS }.id

        dao.insertReading(ReadingEntity(utilityId = elecId, value = 1234.0, timestamp = 3000L, source = "MLKit"))
        dao.insertReading(ReadingEntity(utilityId = gasId, value = 567.0, timestamp = 1000L, source = "Manual"))

        dao.getAllReadings().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cascadeDelete_removesUtilitiesAndReadings() = runBlocking {
        dao.insertProperty(PropertyEntity(name = "Temp", address = null))
        val propId = dao.getAllProperties().first().first().id
        dao.insertUtility(UtilityEntity(propertyId = propId, type = UtilityType.WATER, unit = "Liters", initialReading = 0.0))
        val utilId = dao.getUtilitiesForProperty(propId).first().first().id
        dao.insertReading(ReadingEntity(utilityId = utilId, value = 500.0, timestamp = 5000L, source = "Manual"))

        // Verify they exist
        assertEquals(1, dao.getUtilitiesForProperty(propId).first().size)
        assertEquals(1, dao.getReadingsForUtility(utilId).first().size)

        // Delete property — needs a delete method on DAO
        dao.deleteProperty(propId)

        // Verify cascade
        assertTrue(dao.getAllProperties().first().isEmpty())
        assertTrue(dao.getUtilitiesForProperty(propId).first().isEmpty())
    }
}
