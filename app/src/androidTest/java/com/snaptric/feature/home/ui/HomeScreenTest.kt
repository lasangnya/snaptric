package com.snaptric.feature.home.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.designsystem.theme.SnaptricTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState_showsNoReadingsMessage() {
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    isAnalyzing = false,
                    latestRead = null,
                    properties = emptyList(),
                    readings = emptyList()
                )
            }
        }

        composeTestRule.onNodeWithText("No readings yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take a photo of your meter to get started.").assertIsDisplayed()
    }

    @Test
    fun dashboard_showsGreeting() {
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    isAnalyzing = false,
                    latestRead = null,
                    properties = emptyList(),
                    readings = emptyList()
                )
            }
        }

        // Greeting should be one of the time-based greetings
        composeTestRule.onNodeWithText("Snaptric").assertIsDisplayed()
    }

    @Test
    fun dashboard_showsStatCardsWithData() {
        val properties = listOf(
            PropertyEntity(id = 1, name = "Home", address = null),
            PropertyEntity(id = 2, name = "Office", address = null)
        )
        val readings = listOf(
            ReadingEntity(id = 1, utilityId = 1, value = 1234.5, timestamp = System.currentTimeMillis(), source = "MLKit"),
            ReadingEntity(id = 2, utilityId = 1, value = 1250.0, timestamp = System.currentTimeMillis() - 86400000, source = "Manual")
        )

        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    isAnalyzing = false,
                    latestRead = null,
                    properties = properties,
                    readings = readings
                )
            }
        }

        composeTestRule.onNodeWithText("Properties").assertIsDisplayed()
        composeTestRule.onNodeWithText("Readings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Latest").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()
    }

    @Test
    fun analyzingState_showsAnalyzingText() {
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    isAnalyzing = true,
                    latestRead = null,
                    properties = emptyList(),
                    readings = emptyList()
                )
            }
        }

        composeTestRule.onNodeWithText("AI is analyzing your photo...").assertIsDisplayed()
    }
}
