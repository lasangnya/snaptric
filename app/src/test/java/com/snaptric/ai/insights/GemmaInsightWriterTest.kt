package com.snaptric.ai.insights

import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.insights.MeterRecap
import com.snaptric.core.domain.insights.TemplateInsightWriter
import com.snaptric.core.domain.insights.usesOnlyFactNumbers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GemmaInsightWriterTest {

    private val recap = MeterRecap(
        utility = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.WATER, unit = "m³", initialReading = 0.0),
        propertyName = "Home",
        month = YearMonth.of(2026, 10),
        usedSoFar = 4.5,
        lastReadingDate = LocalDate.of(2026, 10, 10),
        forecast = null,
        lastMonth = null
    )

    private suspend fun writeWith(reply: String?) =
        GemmaInsightWriter({ _, _ -> reply }, TemplateInsightWriter()).write(listOf(recap))

    @Test
    fun `model reply that reuses the facts is shown`() = runTest {
        val insight = writeWith("You've used 4.5 m³ of water at Home so far this month.")

        assertTrue(insight.writtenOnDevice)
        assertEquals("You've used 4.5 m³ of water at Home so far this month.", insight.text)
    }

    @Test
    fun `model reply with an invented number falls back to the template`() = runTest {
        val insight = writeWith("You've used 45 m³ of water, about 3 baths a day.")

        assertFalse(insight.writtenOnDevice)
        assertTrue(insight.text.contains("4.5 m³"))
    }

    @Test
    fun `no model installed falls back to the template`() = runTest {
        assertFalse(writeWith(null).writtenOnDevice)
    }

    @Test
    fun `model failure falls back to the template`() = runTest {
        val insight = GemmaInsightWriter({ _, _ -> error("engine failed") }, TemplateInsightWriter()).write(listOf(recap))

        assertFalse(insight.writtenOnDevice)
    }

    @Test
    fun `number check accepts only numbers present in the facts`() {
        val facts = "Electricity at Home: 100 kWh so far. On track for about 310 kWh. 24% more than September (250 kWh)."
        assertTrue(usesOnlyFactNumbers("100 kWh so far, heading for 310 kWh, up 24%.", facts))
        assertFalse(usesOnlyFactNumbers("100 kWh so far, about 10 kWh a day.", facts))
    }
}
