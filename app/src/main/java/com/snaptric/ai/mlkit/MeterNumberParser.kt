package com.snaptric.ai.mlkit

/**
 * One line of text found by OCR, with the height of its bounding box in pixels.
 *
 * @property charConfidences OCR confidence (0-1) for each character of [text], when available.
 */
data class OcrLine(
    val text: String,
    val height: Int,
    val charConfidences: List<Float>? = null
)

/**
 * What was read from a meter photo.
 *
 * @property value The counter reading, e.g. "01262.4", or null when no plausible number was found.
 * @property uncertainDigits Positions in [value] that OCR was unsure about, for the user to double-check.
 * @property serialNumber The meter's serial number, if one was printed and readable.
 */
data class MeterScan(
    val value: String?,
    val uncertainDigits: Set<Int> = emptySet(),
    val serialNumber: String? = null
)

// A run of digits, possibly split by spaces (gaps between counter drums), with an optional
// decimal part after '.' or ','.
private val NUMBER = Regex("""\d(?:[\d ]*\d)?(?:[.,]\d+)?""")

// Letters OCR commonly returns for digits on meter drums. Only replaced inside digit runs.
private val LOOKALIKES = mapOf('O' to '0', 'o' to '0', 'D' to '0', 'I' to '1', 'l' to '1', '|' to '1', 'S' to '5', 'B' to '8')
private val LOOKALIKE_RUN = Regex("""[0-9OoDIl|SB]{3,}""")

// Labels printed next to a meter's serial number.
private val SERIAL_LABEL = Regex("""(?i)\b(s/?n|serial|no\.?|nr\.?|meter\s*no|zähler)\b|#""")

/** Meter counters show at least four digits; shorter numbers are usually labels or units. */
private const val MIN_COUNTER_DIGITS = 4
private const val MAX_COUNTER_DIGITS = 10

/** Serial numbers are long; without a label, only very long numbers are treated as one. */
private const val MIN_SERIAL_DIGITS = 6
private const val MIN_UNLABELLED_SERIAL_DIGITS = 8

/** Digits OCR is less sure of than this are flagged for the user. */
private const val LOW_CONFIDENCE = 0.6f

/**
 * Picks the meter reading from OCR lines. See [scanMeter].
 */
fun pickMeterNumber(lines: List<OcrLine>): String? = scanMeter(lines).value

/**
 * Reads the counter, its uncertain digits, and the serial number from OCR lines.
 *
 * Instead of joining every digit in the photo, this keeps each number separate, fixes common
 * letter/digit mix-ups inside digit runs, and prefers the tallest counter-length number: on a
 * meter the counter is printed larger than serial numbers and labels. A decimal point or comma
 * in the number is kept.
 */
fun scanMeter(lines: List<OcrLine>): MeterScan {
    val candidates = lines.flatMap { line -> candidatesIn(line) }
    val counter = candidates.maxWithOrNull(
        compareBy<Candidate>({ it.isCounterLength }, { it.height }, { it.digitCount })
    ) ?: return MeterScan(value = null)

    val serial = candidates
        .filter { it !== counter && it.value.all(Char::isDigit) }
        .filter { (it.labelled && it.digitCount >= MIN_SERIAL_DIGITS) || it.digitCount >= MIN_UNLABELLED_SERIAL_DIGITS }
        .maxByOrNull { (if (it.labelled) 100 else 0) + it.digitCount }
        ?.value

    return MeterScan(
        value = counter.value,
        uncertainDigits = counter.confidences
            .withIndex()
            .filter { (_, confidence) -> confidence != null && confidence < LOW_CONFIDENCE }
            .map { it.index }
            .toSet(),
        serialNumber = serial
    )
}

private class Candidate(
    val value: String,
    val confidences: List<Float?>,
    val height: Int,
    val labelled: Boolean
) {
    val digitCount = value.count(Char::isDigit)
    val isCounterLength = digitCount in MIN_COUNTER_DIGITS..MAX_COUNTER_DIGITS
}

private fun candidatesIn(line: OcrLine): List<Candidate> {
    val text = fixLookalikes(line.text)
    // Only trust confidences when OCR actually reported some (the bundled model can report 0s).
    val confidences = line.charConfidences?.takeIf { it.size == text.length && it.any { c -> c > 0f } }
    val labelled = SERIAL_LABEL.containsMatchIn(line.text)

    return NUMBER.findAll(text).map { match ->
        val value = StringBuilder()
        val valueConfidences = mutableListOf<Float?>()
        for (index in match.range) {
            val char = text[index]
            if (char == ' ') continue
            value.append(if (char == ',') '.' else char)
            valueConfidences.add(confidences?.get(index))
        }
        Candidate(value.toString(), valueConfidences, line.height, labelled)
    }.toList()
}

private fun fixLookalikes(text: String): String =
    LOOKALIKE_RUN.replace(text) { run ->
        // Only treat the run as a number when it's mostly real digits, so words like "BOSS" stay words.
        // The replacement is one character for one, so positions still line up with confidences.
        val digits = run.value.count(Char::isDigit)
        if (digits * 2 >= run.value.length) run.value.map { LOOKALIKES[it] ?: it }.joinToString("")
        else run.value
    }
