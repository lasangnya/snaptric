package com.snaptric.ai.mlkit

/**
 * One line of text found by OCR, with the height of its bounding box in pixels.
 */
data class OcrLine(val text: String, val height: Int)

// A run of digits, possibly split by spaces (gaps between counter drums), with an optional
// decimal part after '.' or ','.
private val NUMBER = Regex("""\d(?:[\d ]*\d)?(?:[.,]\d+)?""")

// Letters OCR commonly returns for digits on meter drums. Only replaced inside digit runs.
private val LOOKALIKES = mapOf('O' to '0', 'o' to '0', 'D' to '0', 'I' to '1', 'l' to '1', '|' to '1', 'S' to '5', 'B' to '8')
private val LOOKALIKE_RUN = Regex("""[0-9OoDIl|SB]{3,}""")

/** Meter counters show at least four digits; shorter numbers are usually labels or units. */
private const val MIN_COUNTER_DIGITS = 4
private const val MAX_COUNTER_DIGITS = 10

/**
 * Picks the meter reading from OCR lines.
 *
 * Instead of joining every digit in the photo, this keeps each number separate, fixes common
 * letter/digit mix-ups inside digit runs, and prefers the tallest counter-length number: on a
 * meter the counter is printed larger than serial numbers and labels. A decimal point or comma
 * in the number is kept.
 *
 * @return the reading, e.g. "01262.4", or null when no plausible number is found.
 */
fun pickMeterNumber(lines: List<OcrLine>): String? =
    lines
        .flatMap { line ->
            NUMBER.findAll(fixLookalikes(line.text)).map { match ->
                val value = match.value.replace(" ", "").replace(',', '.')
                Candidate(value, line.height)
            }
        }
        .maxWithOrNull(compareBy<Candidate>({ it.isCounterLength }, { it.height }, { it.digitCount }))
        ?.value

private data class Candidate(val value: String, val height: Int) {
    val digitCount = value.count(Char::isDigit)
    val isCounterLength = digitCount in MIN_COUNTER_DIGITS..MAX_COUNTER_DIGITS
}

private fun fixLookalikes(text: String): String =
    LOOKALIKE_RUN.replace(text) { run ->
        // Only treat the run as a number when it's mostly real digits, so words like "BOSS" stay words.
        val digits = run.value.count(Char::isDigit)
        if (digits * 2 >= run.value.length) run.value.map { LOOKALIKES[it] ?: it }.joinToString("")
        else run.value
    }
