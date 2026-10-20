package com.snaptric.feature.properties.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.theme.SnaptricTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertyDetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState_showsNoMetersMessage() {
        composeTestRule.setContent {
            SnaptricTheme {
                PropertyDetailContent(
                    utilities = emptyList(),
                    onBack = {},
                    onAddUtility = { _, _, _, _ -> },
                    onUtilityClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No Meters Added").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add your first meter to start capturing readings.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add your first meter").assertIsDisplayed()
    }

    @Test
    fun withUtilities_showsCards() {
        val utilities = listOf(
            UtilityEntity(
                id = 1, propertyId = 1,
                type = UtilityType.ELECTRICITY, unit = "kWh",
                initialReading = 1200.0, name = "Main Meter"
            ),
            UtilityEntity(
                id = 2, propertyId = 1,
                type = UtilityType.GAS, unit = "m3",
                initialReading = 45.0, name = null
            ),
            UtilityEntity(
                id = 3, propertyId = 1,
                type = UtilityType.WATER, unit = "Liters",
                initialReading = 8900.0, name = "Garden"
            )
        )

        composeTestRule.setContent {
            SnaptricTheme {
                PropertyDetailContent(
                    utilities = utilities,
                    onBack = {},
                    onAddUtility = { _, _, _, _ -> },
                    onUtilityClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Main Meter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unit: kWh").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unit: m3").assertIsDisplayed()
    }

    private data class NewMeter(val type: UtilityType, val unit: String, val initialReading: Double)

    private fun openAddMeterDialog(onAdd: (NewMeter) -> Unit) {
        composeTestRule.setContent {
            SnaptricTheme {
                PropertyDetailContent(
                    utilities = emptyList(),
                    onBack = {},
                    onAddUtility = { type, unit, reading, _ -> onAdd(NewMeter(type, unit, reading)) },
                    onUtilityClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Add your first meter").performClick()
    }

    @Test
    fun addMeter_readsADecimalCommaInsteadOfSavingZero() {
        var added: NewMeter? = null
        openAddMeterDialog { added = it }

        composeTestRule.onNodeWithText("Initial reading").performTextInput("1234,5")
        composeTestRule.onNodeWithText("Save Meter").performClick()

        assertEquals(1234.5, added!!.initialReading, 0.0)
    }

    @Test
    fun addMeter_cantBeSavedWithoutANumber() {
        openAddMeterDialog {}

        composeTestRule.onNodeWithText("Initial reading").performTextInput("abc")
        composeTestRule.onNodeWithText("Save Meter").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Initial reading").performTextInput(".")
        composeTestRule.onNodeWithText("Save Meter").assertIsNotEnabled()
    }

    @Test
    fun addMeter_switchingTypeStartsOnThatTypesUsualUnit() {
        var added: NewMeter? = null
        openAddMeterDialog { added = it }

        // Electricity starts on kWh, which gas also offers; gas should still start on m³.
        composeTestRule.onNodeWithText("Gas").performClick()
        composeTestRule.onNodeWithText("Initial reading").performTextInput("1000")
        composeTestRule.onNodeWithText("Save Meter").performClick()

        assertEquals(NewMeter(UtilityType.GAS, "m³", 1000.0), added)
    }
}
