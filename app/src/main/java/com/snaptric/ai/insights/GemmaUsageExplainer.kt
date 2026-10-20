package com.snaptric.ai.insights

import com.snaptric.core.domain.insights.Insight
import com.snaptric.core.domain.insights.UsageComparison
import com.snaptric.core.domain.insights.UsageExplainer
import com.snaptric.core.domain.insights.comparisonFacts
import com.snaptric.core.domain.insights.usesOnlyFactNumbers
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * Asks an on-device model to phrase how a new reading compares with the meter's usual use.
 *
 * The comparison is calculated in Kotlin beforehand. If the model is missing, fails, takes longer
 * than [timeoutMillis] (the user is waiting on the save screen), or its reply uses a number that
 * isn't in the facts, the [fallback] is used instead.
 */
class GemmaUsageExplainer(
    private val generate: suspend (systemInstruction: String, prompt: String) -> String?,
    private val fallback: UsageExplainer,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) : UsageExplainer {

    override suspend fun explain(comparison: UsageComparison): Insight {
        val facts = comparisonFacts(comparison).joinToString("\n") { "- $it" }
        val reply = try {
            withTimeoutOrNull(timeoutMillis) { generate(SYSTEM_INSTRUCTION, PROMPT_PREFIX + facts) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        val text = reply?.trim()?.removeSurrounding("\"")
        return if (!text.isNullOrBlank() && text.length <= MAX_LENGTH && '\n' !in text && usesOnlyFactNumbers(text, facts)) {
            Insight(text = text, writtenOnDevice = true)
        } else {
            fallback.explain(comparison)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 4_000L
        const val MAX_LENGTH = 180

        const val SYSTEM_INSTRUCTION =
            "You explain household energy and water meter readings in a meter-reading app."

        const val PROMPT_PREFIX =
            "In one short sentence, tell the user how their usage since the last reading compares with usual. " +
                "Copy numbers and units exactly as written and don't calculate anything new. " +
                "Don't guess causes or give advice. Plain text only.\n\nFacts:\n"
    }
}
