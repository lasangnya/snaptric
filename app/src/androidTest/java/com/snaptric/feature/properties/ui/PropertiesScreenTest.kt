package com.snaptric.feature.properties.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.designsystem.theme.SnaptricTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertiesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState_showsAddPropertyMessage() {
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
        composeTestRule.onNodeWithText("Add your first property to start tracking utility readings.").assertIsDisplayed()
    }

    @Test
    fun withProperties_showsCards() {
        val properties = listOf(
            PropertyEntity(id = 1, name = "My Home", address = "123 Main St", iconIdentifier = "home"),
            PropertyEntity(id = 2, name = "Office", address = null, iconIdentifier = "business")
        )

        composeTestRule.setContent {
            SnaptricTheme {
                PropertiesContent(
                    properties = properties,
                    onPropertyClick = {},
                    onAddProperty = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("My Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Office").assertIsDisplayed()
        composeTestRule.onNodeWithText("123 Main St").assertIsDisplayed()
    }
}
