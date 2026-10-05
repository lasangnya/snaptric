package com.snaptric.core.domain.tariff

import com.snaptric.core.domain.units.MeterUnit

/**
 * Tariff details found in the text of a bill. Every field is null when it couldn't be found;
 * the user reviews and completes them before anything is saved.
 *
 * @property unitRate Price per [billingUnit] in major currency units (pounds, euros, dollars).
 * @property standingChargePerDay Daily fixed charge in major units (monthly charges are converted).
 * @property calorificValue Gas calorific value in MJ/m³.
 * @property volumeCorrection Gas volume correction factor.
 */
data class ParsedBill(
    val unitRate: Double? = null,
    val billingUnit: MeterUnit? = null,
    val standingChargePerDay: Double? = null,
    val currency: String? = null,
    val calorificValue: Double? = null,
    val volumeCorrection: Double? = null
) {
    val foundAnything: Boolean
        get() = listOf(unitRate, standingChargePerDay, calorificValue, volumeCorrection).any { it != null }
}

/** One line of recognised text and where it was on the page. */
data class TextLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = bottom - top
}

/**
 * Puts recognised lines that sit side by side back onto one row, left to right, so a table's
 * label ("Unit rate") and value ("24.50p per kWh") end up together even when OCR reads them as
 * separate blocks.
 */
fun joinIntoRows(lines: List<TextLine>): String {
    val rows = mutableListOf<MutableList<TextLine>>()
    for (line in lines.sortedBy { it.centerY }) {
        val row = rows.lastOrNull()
        val rowCenter = row?.map { it.centerY }?.average()
        val tolerance = (row?.maxOf { it.height } ?: 0).coerceAtLeast(line.height) / 2.0
        if (row != null && rowCenter != null && kotlin.math.abs(line.centerY - rowCenter) <= tolerance) {
            row += line
        } else {
            rows += mutableListOf(line)
        }
    }
    return rows.joinToString("\n") { row -> row.sortedBy { it.left }.joinToString("  ") { it.text } }
}

/**
 * Finds the unit rate, standing charge, billing unit, currency and gas conversion factors in the
 * OCR text of a bill, with regular expressions and simple rules rather than a language model.
 * When a bill lists several rates (day/night, tiers), the first one is used.
 */
object BillTextParser {

    private const val DAYS_PER_MONTH = 365.25 / 12
    private const val DAYS_PER_YEAR = 365.25

    private const val NUMBER = """\d+(?:[.,]\d+)*"""
    private const val UNIT_WORD =
        """kwh|kw\s?h|therms?|m3|m³|cubic\s?met(?:re|er)s?|ccf|hcf|ft3|ft³|cu\.?\s?ft|lit(?:re|er)s?|gal(?:lons?)?"""
    private const val PERIOD_WORD = """day|days|daily|month|months|mth|year|annum|tag|monat|jahr"""

    /** An amount, with an optional currency before or after it and an optional "per kWh/day". */
    private val amountPattern = Regex(
        """(?<pre>[£€$]|eur|gbp|usd)?\s?(?<num>$NUMBER)\s?(?<post>[£€$]|eur\b|gbp\b|usd\b|pence\b|p\b|ct\b|cents?\b|c\b)?""" +
            """(?:\s*(?:/|per\b|a\b|an\b|each\b)\s*(?<per>$UNIT_WORD|$PERIOD_WORD)\b)?""",
        RegexOption.IGNORE_CASE
    )

    /** Text right after a bare number that makes it a quantity rather than a price ("345 kWh", "30 days"). */
    private val quantityFollows = Regex("""^\s?(?:$UNIT_WORD|$PERIOD_WORD)\b""", RegexOption.IGNORE_CASE)

    private val unitRateKeywords = Regex(
        """unit\s?(?:rate|price|charge|cost)|energy\s?(?:charge|rate|price)|price\s?per|rate\s?per|cost\s?per|""" +
            """per\s?kwh|/\s?kwh|kwh\s?(?:rate|price)|arbeitspreis|commodity|supply\s?charge""",
        RegexOption.IGNORE_CASE
    )
    private val standingKeywords = Regex(
        """standing\s?charge|daily\s?charge|fixed\s?charge|service\s?charge|customer\s?charge|basic\s?charge|grundpreis""",
        RegexOption.IGNORE_CASE
    )
    private val calorificKeywords = Regex("""calorific|\bcv\b""", RegexOption.IGNORE_CASE)
    private val correctionKeywords = Regex("""correction|\bvcf\b""", RegexOption.IGNORE_CASE)

    /** The conversion line many UK gas bills print: "345 m3 x 1.02264 x 39.2 ÷ 3.6". */
    private val conversionLine = Regex(
        """[x×*]\s*(?<corr>[01][.,]\d{3,})\s*[x×*]\s*(?<cv>[34]\d[.,]\d+)|[x×*]\s*(?<cv2>[34]\d[.,]\d+)\s*[x×*]\s*(?<corr2>[01][.,]\d{3,})""",
        RegexOption.IGNORE_CASE
    )

    private data class Amount(
        val value: Double,
        val currency: String?,
        val hasCurrencyMark: Boolean,
        val inMinorUnits: Boolean,
        val perUnit: MeterUnit?,
        val perPeriodDays: Double?,
        val isQuantity: Boolean
    )

    fun parse(text: String): ParsedBill {
        val rows = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val currency = detectCurrency(text)

        val unitRate = findUnitRate(rows)
        val standing = findStandingCharge(rows)
        val (correctionFromLine, cvFromLine) = findConversionLine(text)

        val billingUnit = unitRate?.second ?: mostMentionedUnit(text)
        return ParsedBill(
            unitRate = unitRate?.first?.let { roundTo(it, 6) },
            billingUnit = billingUnit,
            standingChargePerDay = standing?.let { roundTo(it, 6) },
            currency = currency,
            calorificValue = findLabelledNumber(rows, calorificKeywords, 30.0..50.0) ?: cvFromLine,
            volumeCorrection = findLabelledNumber(rows, correctionKeywords, 0.9..1.1) ?: correctionFromLine
        )
    }

    /** Parses "24.50", "24,50", "1,234.56" and "1.234,56" into a number. */
    fun parseNumber(raw: String): Double? {
        val s = raw.replace(" ", "")
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val normalized = when {
            lastDot >= 0 && lastComma >= 0 ->
                if (lastComma > lastDot) s.replace(".", "").replace(',', '.') else s.replace(",", "")
            lastComma >= 0 -> {
                val after = s.length - lastComma - 1
                val before = s.substring(0, lastComma)
                val isThousands = s.count { it == ',' } > 1 || (after == 3 && before != "0")
                if (isThousands) s.replace(",", "") else s.replace(',', '.')
            }
            s.count { it == '.' } > 1 -> s.replace(".", "")
            else -> s
        }
        return normalized.toDoubleOrNull()
    }

    private fun amountsIn(row: String): List<Amount> = amountPattern.findAll(row).mapNotNull { match ->
        val number = parseNumber(match.groups["num"]!!.value) ?: return@mapNotNull null
        val pre = match.groups["pre"]?.value?.lowercase()
        val post = match.groups["post"]?.value?.lowercase()
        val per = match.groups["per"]?.value?.lowercase()
        val minor = post in setOf("p", "pence", "ct", "c", "cent", "cents")
        Amount(
            value = if (minor) number / 100 else number,
            currency = currencyOf(pre) ?: currencyOf(post),
            hasCurrencyMark = pre != null || post != null,
            inMinorUnits = minor,
            perUnit = per?.let { MeterUnit.parse(it.replace(Regex("""\s"""), "")) },
            perPeriodDays = per?.let(::periodDays),
            isQuantity = pre == null && post == null && per == null &&
                quantityFollows.containsMatchIn(row.substring(match.range.last + 1))
        )
    }.toList()

    private fun currencyOf(mark: String?): String? = when (mark) {
        "£", "gbp", "p", "pence" -> "GBP"
        "€", "eur", "ct" -> "EUR"
        "$", "usd" -> "USD"
        else -> null
    }

    private fun periodDays(per: String): Double? = when {
        per.startsWith("day") || per == "daily" || per == "tag" -> 1.0
        per.startsWith("month") || per == "mth" || per == "monat" -> DAYS_PER_MONTH
        per == "year" || per == "annum" || per == "jahr" -> DAYS_PER_YEAR
        else -> null
    }

    /** The first price quoted per unit of energy or volume, in major units, and that unit. */
    private fun findUnitRate(rows: List<String>): Pair<Double, MeterUnit?>? {
        // Strongest signal: a price with "per kWh" (or another unit) right after it.
        for (row in rows) {
            if (standingKeywords.containsMatchIn(row)) continue
            amountsIn(row).firstOrNull { it.perUnit != null && !it.isQuantity && it.value > 0 }
                ?.let { return normalizeRate(it) to it.perUnit }
        }
        // Otherwise a labelled row ("Unit rate  24.50p"), or the row after a label on its own.
        rows.forEachIndexed { index, row ->
            if (!unitRateKeywords.containsMatchIn(row) || standingKeywords.containsMatchIn(row)) return@forEachIndexed
            val candidates = (rowAmounts(row) ?: rows.getOrNull(index + 1)?.let(::rowAmounts)).orEmpty()
            candidates.firstOrNull { it.perPeriodDays == null }?.let { amount ->
                return normalizeRate(amount) to unitMentionedIn(row)
            }
        }
        return null
    }

    /** The standing charge converted to a daily amount in major units. */
    private fun findStandingCharge(rows: List<String>): Double? {
        rows.forEachIndexed { index, row ->
            if (!standingKeywords.containsMatchIn(row)) return@forEachIndexed
            val candidates = (rowAmounts(row) ?: rows.getOrNull(index + 1)?.let(::rowAmounts)).orEmpty()
            // A "per day/month" price beats a bare total on the same row.
            val amount = candidates.firstOrNull { it.perPeriodDays != null } ?: candidates.firstOrNull()
                ?: return@forEachIndexed
            // Several pounds or dollars with no period is a monthly charge (common on US bills);
            // daily standing charges are well under that.
            val unlabelledDays = if (amount.hasCurrencyMark && !amount.inMinorUnits && amount.value >= 3) DAYS_PER_MONTH else 1.0
            val days = amount.perPeriodDays ?: periodMentionedIn(row) ?: unlabelledDays
            var value = amount.value
            // "53.35 per day" with no currency mark is almost certainly pence or cents.
            if (!amount.hasCurrencyMark && days == 1.0 && value >= 5) value /= 100
            return value / days
        }
        // No label, but a price quoted per day is a standing charge.
        for (row in rows) {
            amountsIn(row).firstOrNull { it.perPeriodDays == 1.0 && it.hasCurrencyMark }?.let { return it.value }
        }
        return null
    }

    /** Prices on a row, skipping quantities such as "345 kWh"; null when there are none. */
    private fun rowAmounts(row: String): List<Amount>? =
        amountsIn(row).filter { !it.isQuantity && it.value > 0 }.takeIf { it.isNotEmpty() }

    /** A rate with no currency mark that's too big to be in pounds is taken to be in pence/cents. */
    private fun normalizeRate(amount: Amount): Double =
        if (!amount.hasCurrencyMark && !amount.inMinorUnits && amount.value >= 2) amount.value / 100 else amount.value

    private fun unitMentionedIn(row: String): MeterUnit? =
        Regex("""\b($UNIT_WORD)\b""", RegexOption.IGNORE_CASE).find(row)?.let { MeterUnit.parse(it.value.replace(" ", "")) }

    private fun periodMentionedIn(row: String): Double? =
        Regex("""\b($PERIOD_WORD)\b""", RegexOption.IGNORE_CASE).find(row)?.let { periodDays(it.value.lowercase()) }

    private fun mostMentionedUnit(text: String): MeterUnit? =
        Regex("""\b($UNIT_WORD)\b""", RegexOption.IGNORE_CASE).findAll(text)
            .mapNotNull { MeterUnit.parse(it.value.replace(" ", "")) }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key

    /** A number in [range] on a row matching [label] (or the row after it). */
    private fun findLabelledNumber(rows: List<String>, label: Regex, range: ClosedFloatingPointRange<Double>): Double? {
        rows.forEachIndexed { index, row ->
            if (!label.containsMatchIn(row)) return@forEachIndexed
            listOfNotNull(row, rows.getOrNull(index + 1)).forEach { candidateRow ->
                Regex(NUMBER).findAll(candidateRow)
                    .mapNotNull { parseNumber(it.value) }
                    .firstOrNull { it in range }
                    ?.let { return it }
            }
        }
        return null
    }

    private fun findConversionLine(text: String): Pair<Double?, Double?> {
        val match = conversionLine.find(text) ?: return null to null
        val correction = (match.groups["corr"] ?: match.groups["corr2"])?.value?.let(::parseNumber)
        val cv = (match.groups["cv"] ?: match.groups["cv2"])?.value?.let(::parseNumber)
        return correction?.takeIf { it in 0.9..1.1 } to cv?.takeIf { it in 30.0..50.0 }
    }

    /** The currency most often marked on the bill, by symbol or code. */
    private fun detectCurrency(text: String): String? {
        val votes = mutableMapOf<String, Int>()
        fun vote(pattern: String, currency: String) {
            votes.merge(currency, Regex(pattern, RegexOption.IGNORE_CASE).findAll(text).count(), Int::plus)
        }
        vote("""£|\bgbp\b|\d\s?p\b|\bpence\b""", "GBP")
        vote("""€|\beur\b|\d\s?ct\b""", "EUR")
        vote("""\$|\busd\b""", "USD")
        return votes.filterValues { it > 0 }.maxByOrNull { it.value }?.key
    }

    private fun roundTo(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return Math.round(value * factor) / factor
    }
}
