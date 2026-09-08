package com.shohan.khatago.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.absoluteValue

/**
 * Money.
 *
 * ABSOLUTE RULE: financial values are never Float or Double.
 * Every amount in KhataGo is a [Long] count of minor units (paisa):
 *
 *      BDT 100.50  ->  10050
 *
 * All arithmetic goes through the safe helpers below so that overflow cannot
 * silently corrupt a balance. Parsing and percentage maths use [BigDecimal] and
 * are rounded deterministically (HALF_UP) back into minor units.
 */
object Money {

    const val CURRENCY_CODE = "BDT"
    const val CURRENCY_SYMBOL = "৳"
    const val MINOR_UNITS_PER_MAJOR = 100L

    /** Largest amount KhataGo will intentionally format (about 92 quadrillion BDT). */
    val MAX_SAFE = Long.MAX_VALUE / 1_000_000L

    // ------------------------------------------------------------- construction

    /** BDT 42 -> 4200 minor units. */
    fun ofMajor(major: Long): Long = safeMultiply(major, MINOR_UNITS_PER_MAJOR)

    /** Minor units -> exact decimal BDT (for exports, PDFs and reports). */
    fun toDecimal(minor: Long): BigDecimal =
        BigDecimal.valueOf(minor)
            .divide(BigDecimal.valueOf(MINOR_UNITS_PER_MAJOR), 2, RoundingMode.UNNECESSARY)

    /**
     * Parses user input such as `1250`, `1,250.75`, `৳ 1250.5` into minor units.
     * Returns null when the text is not a usable amount — callers then show
     * "Enter a valid amount." instead of a stack trace.
     */
    fun parse(input: String?): Long? {
        val cleaned = input
            ?.trim()
            ?.replace(CURRENCY_SYMBOL, "")
            ?.replace(",", "")
            ?.replace("−", "-")
            ?.replace(" ", "")
            ?: return null
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
        val value = runCatching { BigDecimal(cleaned) }.getOrNull() ?: return null
        if (value < BigDecimal.ZERO) return null
        if (value > BigDecimal.valueOf(Long.MAX_VALUE)) return null
        return value
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    // ------------------------------------------------------------- arithmetic

    fun safeAdd(a: Long, b: Long): Long =
        try {
            Math.addExact(a, b)
        } catch (_: ArithmeticException) {
            if (a >= 0) Long.MAX_VALUE else Long.MIN_VALUE
        }

    fun safeSubtract(a: Long, b: Long): Long =
        try {
            Math.subtractExact(a, b)
        } catch (_: ArithmeticException) {
            if (a >= 0) Long.MAX_VALUE else Long.MIN_VALUE
        }

    fun safeMultiply(a: Long, b: Long): Long =
        try {
            Math.multiplyExact(a, b)
        } catch (_: ArithmeticException) {
            if ((a > 0) xor (b > 0)) Long.MIN_VALUE else Long.MAX_VALUE
        }

    /** Multiplies an amount by a rate expressed in basis points (12.50% -> 1250). */
    fun applyRate(amount: Long, rateBps: Long): Long {
        val result = BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(rateBps))
            .divide(BigDecimal.valueOf(10_000L), 0, RoundingMode.HALF_UP)
        return clampToLong(result)
    }

    /** quantity is held in thousandths (1.5 kg -> 1500) so no Double is ever used. */
    fun lineTotal(unitPriceMinor: Long, quantityMilli: Long): Long {
        val result = BigDecimal.valueOf(unitPriceMinor)
            .multiply(BigDecimal.valueOf(quantityMilli))
            .divide(BigDecimal.valueOf(1_000L), 0, RoundingMode.HALF_UP)
        return clampToLong(result)
    }

    /**
     * Splits [total] into [parts] nearly-equal minor-unit pieces.
     * The first `remainder` pieces receive one extra paisa so the pieces always
     * add back up to exactly [total] (no paisa is ever lost).
     */
    fun splitEvenly(total: Long, parts: Int): List<Long> {
        if (parts <= 0) return emptyList()
        if (total <= 0L) return List(parts) { 0L }
        val base = total / parts
        val remainder = (total % parts).toInt()
        return List(parts) { index -> base + if (index < remainder) 1L else 0L }
    }

    private fun clampToLong(value: BigDecimal): Long =
        try {
            value.longValueExact()
        } catch (_: ArithmeticException) {
            if (value.signum() >= 0) Long.MAX_VALUE else Long.MIN_VALUE
        }

    // -------------------------------------------------------------- formatting

    /**
     * `4200 -> ৳42`, `4250 -> ৳42.50`, `4250 signed -> +৳42.50`.
     * Trailing paisa are hidden when zero so totals read cleanly.
     */
    fun format(
        minor: Long,
        symbol: String = CURRENCY_SYMBOL,
        showDecimals: Boolean = true,
        signed: Boolean = false
    ): String {
        val negative = minor < 0
        val magnitude = if (minor == Long.MIN_VALUE) Long.MAX_VALUE else minor.absoluteValue
        val whole = magnitude / MINOR_UNITS_PER_MAJOR
        val fraction = magnitude % MINOR_UNITS_PER_MAJOR
        val grouped = groupThousands(whole)
        val number = if (showDecimals && fraction != 0L) {
            "$grouped.${fraction.toString().padStart(2, '0')}"
        } else {
            grouped
        }
        val sign = when {
            negative -> "−"
            signed -> "+"
            else -> ""
        }
        return "$sign$symbol$number"
    }

    /** Plain number with grouping, no currency symbol. */
    fun formatPlain(minor: Long, showDecimals: Boolean = true): String =
        format(minor, symbol = "", showDecimals = showDecimals)

    /** Short form used on chart axes: ৳850, ৳8.4K, ৳1.2L, ৳2.4Cr. */
    fun formatShort(minor: Long, symbol: String = CURRENCY_SYMBOL): String {
        val negative = minor < 0
        val magnitude = if (minor == Long.MIN_VALUE) Long.MAX_VALUE else minor.absoluteValue
        val taka = magnitude / MINOR_UNITS_PER_MAJOR
        val sign = if (negative) "−" else ""
        return when {
            taka >= 1_00_00_000L -> sign + symbol + trimDecimal(taka / 1_00_00_000.0) + "Cr"
            taka >= 1_00_000L -> sign + symbol + trimDecimal(taka / 1_00_000.0) + "L"
            taka >= 1_000L -> sign + symbol + trimDecimal(taka / 1_000.0) + "K"
            else -> sign + symbol + taka.toString()
        }
    }

    private fun trimDecimal(value: Double): String {
        val rounded = (value * 10).toLong() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private fun groupThousands(value: Long): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val builder = StringBuilder()
        var count = 0
        for (index in raw.length - 1 downTo 0) {
            builder.append(raw[index])
            count++
            if (count % 3 == 0 && index > 0) builder.append(',')
        }
        return builder.reverse().toString()
    }
}
