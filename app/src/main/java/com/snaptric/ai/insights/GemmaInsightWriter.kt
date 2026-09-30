package com.snaptric.ai.insights

import com.snaptric.core.domain.insights.Insight
import com.snaptric.core.domain.insights.InsightWriter
import com.snaptric.core.domain.insights.MeterRecap
import com.snaptric.core.domain.insights.recapFacts
import com.snaptric.core.domain.insights.usesOnlyFactNumbers
import kotlin.coroutines.cancellation.CancellationException

/**
 * Asks an on-device model to reword the recap facts into a friendlier summary.
 *
 * The facts are calculated in Kotlin beforehand. If the model is missing, fails, or its reply uses a
 * number that isn't in the facts, the [fallback] template is used instead.
 */
class GemmaInsightWriter(
    private val generate: suspend (systemInstruction: String, prompt: String) -> String?,
    private val fallback: InsightWriter
) : InsightWriter {

    override suspend fun write(recaps: List<MeterRecap>): Insight {
        val facts = recapFacts(recaps).joinToString("\n") { "- $it" }
        val reply = try {
            generate(SYSTEM_INSTRUCTION, PROMPT_PREFIX + facts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        val text = reply?.trim()?.removeSurrounding("\"")
        return if (!text.isNullOrBlank() && text.length <= MAX_LENGTH && usesOnlyFactNumbers(text, facts)) {
            Insight(text = text, writtenOnDevice = true)
        } else {
            fallback.write(recaps)
        }
    }

    private companion object {
        const val MAX_LENGTH = 400

        const val SYSTEM_INSTRUCTION =
            "You write short, friendly summaries of household energy and water use for a meter-reading app."

        const val PROMPT_PREFIX =
            "Summarise these facts in at most two sentences for the app's home screen. " +
                "Copy numbers and units exactly as written and don't calculate anything new. " +
                "Plain text only, no lists or markdown.\n\nFacts:\n"
    }
}
