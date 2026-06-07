package com.snaptric.feature.properties.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.designsystem.theme.SnaptricTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertiesFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState_clickAddButton_opensDialog() {
        composeTestRule.setContent {
            SnaptricTheme {
                PropertiesContent(
                    properties = emptyList(),
                    onPropertyClick = {},
                    onAddProperty = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("No Properties Yet").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Add Property").performClick()

        composeTestRule.onNodeWithText("Add New Property").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun dialog_cancelButton_closesDialog() {
        composeTestRule.setContent {
            SnaptricTheme {
                PropertiesContent(
                    properties = emptyList(),
                    onPropertyClick = {},
                    onAddProperty = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add Property").performClick()
        composeTestRule.onNodeWithText("Add New Property").assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Add New Property").assertDoesNotExist()
    }

    @Test
    fun dialog_saveButton_callsOnAddPropertyWithCorrectName() {
        var savedName = ""
        var savedAddress: String? = null

        composeTestRule.setContent {
            SnaptricTheme {
                PropertiesContent(
                    properties = emptyList(),
                    onPropertyClick = {},
                    onAddProperty = { name, address, _ ->
                        savedName = name
                        savedAddress = address
                    }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add Property").performClick()

        // Type into the name field — find by the label text
        composeTestRule.onNodeWithText("Property Name (e.g. Home)").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Beach House")

        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals("Beach House", savedName)
    }

    @Test
    fun withProperties_cardClick_triggersCallback() {
        var clickedId: Long? = null
        val properties = listOf(
            PropertyEntity(id = 42, name = "My Home", address = "123 Main St", iconIdentifier = "home")
        )

        composeTestRule.setContent {
            SnaptricTheme {
                PropertiesContent(
                    properties = properties,
                    onPropertyClick = { id -> clickedId = id },
                    onAddProperty = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("My Home").performClick()

        assertEquals(42L, clickedId)
    }
}
