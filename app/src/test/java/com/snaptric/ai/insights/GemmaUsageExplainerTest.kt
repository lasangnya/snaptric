package com.snaptric.ai.insights

import com.snaptric.core.domain.insights.TemplateUsageExplainer
import com.snaptric.core.domain.insights.UsageComparison
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaUsageExplainerTest {

    private val comparison = UsageComparison(unit = "kWh", usedSinceLast = 62.0, days = 5.0, averagePerDay = 8.1)

    private suspend fun explainWith(reply: String?) =
        GemmaUsageExplainer({ _, _ -> reply }, TemplateUsageExplainer()).explain(comparison)

    @Test
    fun `model reply that reuses the facts is shown`() = runTest {
        val insight = explainWith("You used 12.4 kWh a day, 53% more than your usual 8.1 kWh.")

        assertTrue(insight.writtenOnDevice)
        assertEquals("You used 12.4 kWh a day, 53% more than your usual 8.1 kWh.", insight.text)
    }

    @Test
    fun `model reply with an invented number falls back to the template`() = runTest {
        val insight = explainWith("You used 50% more, about 4 extra kWh a day.")

        assertFalse(insight.writtenOnDevice)
        assertTrue(insight.text.contains("53% more than usual"))
    }

    @Test
    fun `model reply longer than one line falls back to the template`() = runTest {
        assertFalse(explainWith("12.4 kWh a day.\nThat's 53% more than usual.").writtenOnDevice)
    }

    @Test
    fun `no model, a failure or a slow model falls back to the template`() = runTest {
        assertFalse(explainWith(null).writtenOnDevice)
        assertFalse(GemmaUsageExplainer({ _, _ -> error("engine failed") }, TemplateUsageExplainer()).explain(comparison).writtenOnDevice)

        val slow = GemmaUsageExplainer(
            generate = { _, _ -> delay(10_000); "12.4 kWh a day." },
            fallback = TemplateUsageExplainer(),
            timeoutMillis = 1_000
        )
        assertFalse(slow.explain(comparison).writtenOnDevice)
    }
}
