package com.snaptric.feature.home.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.home.viewmodel.buildHomeReadingItems
import com.snaptric.feature.home.viewmodel.buildMeterSummaries
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val properties = listOf(
        PropertyEntity(id = 1, name = "Home", address = null),
        PropertyEntity(id = 2, name = "Office", address = null)
    )
    private val utilities = listOf(
        UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
    )
    private val readings = listOf(
        ReadingEntity(id = 1, utilityId = 1, value = 1200.0, timestamp = 1_000L, source = "Manual"),
        ReadingEntity(id = 2, utilityId = 1, value = 1234.5, timestamp = 2_000L, source = "MLKit")
    )

    @Test
    fun emptyState_showsNoReadingsMessageAndScanAction() {
        var scanClicked = false
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    properties = emptyList(),
                    readingItems = emptyList(),
                    onScanClick = { scanClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("No readings yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take a photo of your meter to get started.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan your first meter").performClick()
        assertTrue(scanClicked)
    }

    @Test
    fun dashboard_showsGreeting() {
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    properties = emptyList(),
                    readingItems = emptyList()
                )
            }
        }

        composeTestRule.onNodeWithText("Snaptric").assertIsDisplayed()
    }

    @Test
    fun hero_showsLatestReadingWithMeterAndDelta() {
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    properties = properties,
                    readingItems = buildHomeReadingItems(readings, utilities, properties)
                )
            }
        }

        composeTestRule.onNodeWithText("Latest reading").assertIsDisplayed()
        composeTestRule.onNodeWithText("1234.5").assertIsDisplayed()
        // Shown in the hero and again on the older reading in Recent Activity.
        composeTestRule.onAllNodesWithText("Electricity · Home").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("+34.5 kWh", substring = true).assertIsDisplayed()
    }

    @Test
    fun dashboard_showsMetersAndRecentActivity() {
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    properties = properties,
                    readingItems = buildHomeReadingItems(readings, utilities, properties),
                    meters = buildMeterSummaries(readings, utilities, properties)
                )
            }
        }

        composeTestRule.onNodeWithText("Your meters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()
        // The older reading is listed under Recent Activity.
        composeTestRule.onNodeWithText("1200").assertIsDisplayed()
    }

    @Test
    fun meterWithoutReadings_isShownWithAWayToReadIt() {
        val gas = UtilityEntity(id = 2, propertyId = 1, type = UtilityType.GAS, unit = "m³", initialReading = 0.0)
        var scanClicked = false
        composeTestRule.setContent {
            SnaptricTheme {
                HomeContent(
                    properties = properties,
                    readingItems = buildHomeReadingItems(readings, utilities + gas, properties),
                    meters = buildMeterSummaries(readings, utilities + gas, properties),
                    onScanClick = { scanClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Gas").assertIsDisplayed()
        composeTestRule.onNodeWithText("No readings yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take first reading").performClick()
        assertTrue(scanClicked)
    }
}
