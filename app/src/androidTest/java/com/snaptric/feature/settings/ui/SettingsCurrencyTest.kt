package com.snaptric.feature.settings.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.ai.litertlm.GemmaModelDownloader
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.settings.viewmodel.DataSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsCurrencyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pickingACurrency_reportsItsCode() {
        var chosen: String? = null
        composeTestRule.setContent {
            SnaptricTheme {
                SettingsContent(
                    modelState = GemmaModelDownloader.State.NotInstalled,
                    dataSummary = DataSummary(0, 0, 0),
                    versionName = "1.0",
                    currency = "GBP",
                    onCurrencyChange = { chosen = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Default: GBP", substring = true).performScrollTo().performClick()
        composeTestRule.onNodeWithText("Search, e.g. EUR or euro").performTextInput("Sri Lankan")
        composeTestRule.onNodeWithText("LKR").performClick()
        assertEquals("LKR", chosen)
    }
}
