package com.snaptric.feature.tariffs.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.tariffs.viewmodel.BillScanState
import com.snaptric.feature.tariffs.viewmodel.TariffDraft
import com.snaptric.feature.tariffs.viewmodel.TariffField
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TariffScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val gas = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.GAS, unit = "m³", initialReading = 0.0)

    @Test
    fun noTariffs_offersScanAndManualEntry() {
        composeTestRule.setContent {
            SnaptricTheme {
                TariffContent(utility = gas, tariffs = emptyList(), draft = null, scanState = BillScanState.Idle, onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Scan bill").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter by hand").assertIsDisplayed()
    }

    @Test
    fun scannedDraft_marksFieldsAndNeedsMissingOnesBeforeSaving() {
        var saved = false
        composeTestRule.setContent {
            var draft by remember {
                mutableStateOf(
                    TariffDraft(
                        unitRate = "0.0624", billingUnit = "kWh", currency = "GBP", effectiveFrom = LocalDate.of(2026, 10, 1),
                        fromBill = setOf(TariffField.UNIT_RATE, TariffField.CURRENCY), scanned = true
                    )
                )
            }
            SnaptricTheme {
                TariffContent(
                    utility = gas,
                    tariffs = emptyList(),
                    draft = draft,
                    scanState = BillScanState.Idle,
                    onBack = {},
                    onDraftChange = { draft = it(draft) },
                    onSave = { saved = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Check scanned tariff").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Read from bill – check it", substring = true).assertCountEquals(2)
        composeTestRule.onNodeWithText("Save tariff").performScrollTo().assertIsNotEnabled()

        composeTestRule.onNodeWithText("Standing charge (GBP per day)").performTextInput("0.3143")
        composeTestRule.onNodeWithText("Save tariff").performScrollTo().assertIsEnabled().performClick()
        assertTrue(saved)
    }
}
