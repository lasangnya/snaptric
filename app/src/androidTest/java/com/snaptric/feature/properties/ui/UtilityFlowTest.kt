package com.snaptric.feature.properties.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.theme.SnaptricTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilityFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState_clickAddButton_opensDialog() {
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

        composeTestRule.onNodeWithContentDescription("Add Meter").performClick()

        composeTestRule.onNodeWithText("Add New Meter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Meter").assertIsDisplayed()
    }

    @Test
    fun dialog_cancelButton_closesDialog() {
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

        composeTestRule.onNodeWithContentDescription("Add Meter").performClick()
        composeTestRule.onNodeWithText("Add New Meter").assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Add New Meter").assertDoesNotExist()
    }

    @Test
    fun dialog_saveButton_callsOnAddUtility() {
        var savedType: UtilityType? = null
        var savedUnit: String? = null
        var savedReading: Double? = null

        composeTestRule.setContent {
            SnaptricTheme {
                PropertyDetailContent(
                    utilities = emptyList(),
                    onBack = {},
                    onAddUtility = { type, unit, reading, _ ->
                        savedType = type
                        savedUnit = unit
                        savedReading = reading
                    },
                    onUtilityClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add Meter").performClick()

        // Type the initial reading
        composeTestRule.onNodeWithText("Initial reading").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("250")

        composeTestRule.onNodeWithText("Save Meter").performClick()

        assertEquals(UtilityType.ELECTRICITY, savedType)
        assertEquals("m\u00B3", savedUnit) // m³
        assertEquals(250.0, savedReading)
    }

    @Test
    fun dialog_typeSegmentedButtons_areVisible() {
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

        composeTestRule.onNodeWithContentDescription("Add Meter").performClick()

        // All three type options should be visible
        composeTestRule.onNodeWithText("Electricity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Water").assertIsDisplayed()
    }

    @Test
    fun withUtilities_cardClick_triggersCallback() {
        var clickedId: Long? = null
        val utilities = listOf(
            UtilityEntity(
                id = 7, propertyId = 1,
                type = UtilityType.GAS,
                unit = "m\u00B3",
                initialReading = 45.0,
                name = "Boiler"
            )
        )

        composeTestRule.setContent {
            SnaptricTheme {
                PropertyDetailContent(
                    utilities = utilities,
                    onBack = {},
                    onAddUtility = { _, _, _, _ -> },
                    onUtilityClick = { id -> clickedId = id }
                )
            }
        }

        composeTestRule.onNodeWithText("Boiler").performClick()

        assertEquals(7L, clickedId)
    }
}
