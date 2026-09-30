package com.snaptric.core.domain.insights

import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * A short, readable summary of this month's usage.
 *
 * @property writtenOnDevice True when an on-device model wrote the text; false for the built-in template.
 */
data class Insight(
    val text: String,
    val writtenOnDevice: Boolean
)

/**
 * Turns meter recaps into a short summary. Every number must come from the recaps; implementations
 * only choose the wording.
 */
interface InsightWriter {
    suspend fun write(recaps: List<MeterRecap>): Insight
}

/**
 * One plain sentence of facts per meter. Used as the template text and as the only source of
 * numbers the on-device model is allowed to use.
 */
fun recapFacts(recaps: List<MeterRecap>, locale: Locale = Locale.getDefault()): List<String> =
    recaps.map { recap ->
        val unit = recap.utility.unit
        val monthName = recap.month.month.getDisplayName(TextStyle.FULL, locale)
        val previousName = recap.month.minusMonths(1).month.getDisplayName(TextStyle.FULL, locale)
        buildString {
            append("${recap.meterLabel()}: ${formatAmount(recap.usedSoFar)} $unit used so far in $monthName.")
            recap.forecast?.let { append(" On track for about ${formatAmount(it)} $unit.") }
            val change = recap.changePercent
            val lastMonth = recap.lastMonth
            if (change != null && lastMonth != null) {
                val direction = when {
                    change > 0 -> "${abs(change)}% more than"
                    change < 0 -> "${abs(change)}% less than"
                    else -> "the same as"
                }
                append(" That's $direction $previousName (${formatAmount(lastMonth)} $unit).")
            }
        }
    }

/**
 * The always-available writer: joins the fact sentences as they are.
 */
class TemplateInsightWriter : InsightWriter {
    override suspend fun write(recaps: List<MeterRecap>): Insight =
        Insight(text = recapFacts(recaps).joinToString(" "), writtenOnDevice = false)
}

/**
 * True when every number in [text] also appears in [facts], so a model can't invent or
 * miscalculate a figure.
 */
fun usesOnlyFactNumbers(text: String, facts: String): Boolean {
    val numberPattern = Regex("""\d+(?:[.,]\d+)?""")
    val allowed = numberPattern.findAll(facts).map { it.value }.toSet()
    return numberPattern.findAll(text).all { it.value in allowed }
}
