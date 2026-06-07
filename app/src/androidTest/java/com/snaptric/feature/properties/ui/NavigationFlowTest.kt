package com.snaptric.feature.properties.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun propertyDetailScreen_backButton_triggersCallback() {
        var backClicked = false

        composeTestRule.setContent {
            SnaptricTheme {
                PropertyDetailContent(
                    utilities = emptyList(),
                    onBack = { backClicked = true },
                    onAddUtility = { _, _, _, _ -> },
                    onUtilityClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(true, backClicked)
    }

    @Test
    fun utilityScreen_backButton_triggersCallback() {
        var backClicked = false
        val readings = listOf(
            ReadingEntity(id = 1, utilityId = 1, value = 1234.0, timestamp = System.currentTimeMillis(), source = "MLKit")
        )

        composeTestRule.setContent {
            SnaptricTheme {
                UtilityDetailContent(
                    readings = readings,
                    onBack = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(true, backClicked)
    }

    @Test
    fun propertyDetail_navigatesToUtilityDetail() {
        var navigatedUtilityId: Long? = null
        val utilities = listOf(
            UtilityEntity(
                id = 99, propertyId = 1,
                type = UtilityType.ELECTRICITY,
                unit = "kWh",
                initialReading = 100.0,
                name = "Main Meter"
            )
        )

        composeTestRule.setContent {
            SnaptricTheme {
                PropertyDetailContent(
                    utilities = utilities,
                    onBack = {},
                    onAddUtility = { _, _, _, _ -> },
                    onUtilityClick = { id -> navigatedUtilityId = id }
                )
            }
        }

        composeTestRule.onNodeWithText("Main Meter").performClick()

        assertEquals(99L, navigatedUtilityId)
    }

    @Test
    fun propertiesScreen_cardClick_navigatesToDetail() {
        var navigatedPropertyId: Long? = null
        val properties = listOf(
            PropertyEntity(id = 55, name = "Summer House", address = "Beach Rd", iconIdentifier = "villa")
        )

        composeTestRule.setContent {
            SnaptricTheme {
                PropertiesContent(
                    properties = properties,
                    onPropertyClick = { id -> navigatedPropertyId = id },
                    onAddProperty = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Summer House").performClick()

        assertEquals(55L, navigatedPropertyId)
    }
}
