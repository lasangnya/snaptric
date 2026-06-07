package com.snaptric.feature.properties.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.designsystem.theme.SnaptricTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilityScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState_showsNoReadingsMessage() {
        composeTestRule.setContent {
            SnaptricTheme {
                UtilityDetailContent(
                    readings = emptyList(),
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No Readings Yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Use the camera to capture your first meter reading.").assertIsDisplayed()
    }

    @Test
    fun singleReading_showsChartPlaceholder() {
        val readings = listOf(
            ReadingEntity(id = 1, utilityId = 1, value = 1234.0, timestamp = System.currentTimeMillis(), source = "MLKit")
        )

        composeTestRule.setContent {
            SnaptricTheme {
                UtilityDetailContent(
                    readings = readings,
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Capture readings across multiple months to see trends").assertIsDisplayed()
    }

    @Test
    fun multipleReadings_showsHistoryItems() {
        val readings = listOf(
            ReadingEntity(id = 1, utilityId = 1, value = 1325.0, timestamp = System.currentTimeMillis() - 500000000L, source = "Gemma"),
            ReadingEntity(id = 2, utilityId = 1, value = 1310.0, timestamp = System.currentTimeMillis() - 400000000L, source = "MLKit"),
            ReadingEntity(id = 3, utilityId = 1, value = 1295.0, timestamp = System.currentTimeMillis() - 300000000L, source = "Manual"),
            ReadingEntity(id = 4, utilityId = 1, value = 1280.0, timestamp = System.currentTimeMillis() - 200000000L, source = "MLKit")
        )

        composeTestRule.setContent {
            SnaptricTheme {
                UtilityDetailContent(
                    readings = readings,
                    onBack = {}
                )
            }
        }

        // The header should be visible
        composeTestRule.onNodeWithText("Reading History").assertIsDisplayed()
    }
}
