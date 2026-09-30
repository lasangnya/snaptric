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

    private fun showSheet(detectedValue: String, onSave: (Double, Long) -> Unit = { _, _ -> }) {
        composeTestRule.setContent {
            SnaptricTheme {
                ConfirmReadingSheet(
                    detectedValue = detectedValue,
                    properties = listOf(PropertyEntity(id = 1, name = "Home", address = null)),
                    selectedPropertyId = 1,
                    utilities = listOf(
                        UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
                    ),
                    history = history,
                    onPropertySelected = {},
                    onUtilitySelected = {},
                    onDismiss = {},
                    onSave = onSave,
                    capturedBitmap = null
                )
            }
        }
    }

    @Test
    fun missedDecimal_showsWarningAndAppliesSuggestion() {
        var saved: Double? = null
        showSheet(detectedValue = "12904") { value, _ -> saved = value }

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
}
