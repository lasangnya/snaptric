package com.snaptric.feature.capture.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.core.domain.insights.MeterMatch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfirmReadingSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val day = 24L * 60 * 60 * 1000
    private val now = System.currentTimeMillis()

    // About 14 kWh a day for the last 30 days; the latest reading was 2 days ago.
    private val history = listOf(
        ReadingEntity(id = 1, utilityId = 1, value = 842.4, timestamp = now - 32 * day, source = "Manual"),
        ReadingEntity(id = 2, utilityId = 1, value = 1262.4, timestamp = now - 2 * day, source = "MLKit")
    )

    private fun showSheet(
        detectedValue: String,
        uncertainDigits: Set<Int> = emptySet(),
        meterMatch: MeterMatch? = null,
        utilities: List<UtilityEntity> = listOf(
            UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
        ),
        preselectedUtilityId: Long? = null,
        onSave: (Double, Long) -> Unit = { _, _ -> }
    ) {
        composeTestRule.setContent {
            SnaptricTheme {
                ConfirmReadingSheet(
                    detectedValue = detectedValue,
                    properties = listOf(PropertyEntity(id = 1, name = "Home", address = null)),
                    selectedPropertyId = 1,
                    utilities = utilities,
                    history = history,
                    onPropertySelected = {},
                    onUtilitySelected = {},
                    onDismiss = {},
                    onSave = onSave,
                    capturedBitmap = null,
                    uncertainDigits = uncertainDigits,
                    meterMatch = meterMatch,
                    preselectedUtilityId = preselectedUtilityId
                )
            }
        }
    }

    @Test
    fun missedDecimal_showsWarningAndAppliesSuggestion() {
        var saved: Double? = null
        showSheet(detectedValue = "12904", onSave = { value, _ -> saved = value })

        composeTestRule.onNodeWithText("This looks off").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Use 1290.4").performScrollTo().performClick()

        // The fix clears the warning, and the reading saves with the corrected value.
        composeTestRule.onNodeWithText("Save reading").performScrollTo().performClick()
        assertEquals(1290.4, saved!!, 0.0001)
    }

    @Test
    fun normalReading_showsNoWarning() {
        showSheet(detectedValue = "1290.4")

        composeTestRule.onNodeWithText("Save reading").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("This looks off").assertDoesNotExist()
    }

    @Test
    fun uncertainDigitsAndSerialMatch_areExplained() {
        val meter = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0, serialNumber = "20481733")
        showSheet(
            detectedValue = "1290.4",
            uncertainDigits = setOf(2),
            meterMatch = MeterMatch(meter, MeterMatch.Reason.SERIAL_NUMBER)
        )

        composeTestRule.onNodeWithText("Check the highlighted digit.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Matched by serial number …1733").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun meterStartedFrom_isSelectedOverTheFirstMeter() {
        val gas = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.GAS, unit = "m³", initialReading = 0.0)
        val electric = UtilityEntity(id = 2, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
        var savedTo: Long? = null
        showSheet(
            detectedValue = "1290.4",
            utilities = listOf(gas, electric),
            preselectedUtilityId = 2,
            onSave = { _, utilityId -> savedTo = utilityId }
        )

        composeTestRule.onNodeWithText("Save reading").performScrollTo().performClick()
        assertEquals(2L, savedTo)
    }
}
