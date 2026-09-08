package com.shohan.khatago

import com.google.common.truth.Truth.assertThat
import com.shohan.khatago.core.money.Money
import org.junit.Test

/**
 * Money is stored as Long minor units (paisa) everywhere in KhataGo. These tests
 * guard the two rules that matter most: no floating point is ever used, and a
 * split always adds back up to exactly the original amount.
 */
class MoneyTest {

    @Test
    fun parse_readsPlainAndFormattedInput() {
        assertThat(Money.parse("1250")).isEqualTo(125_000L)
        assertThat(Money.parse("1,250.75")).isEqualTo(125_075L)
        assertThat(Money.parse("৳ 1250.5")).isEqualTo(125_050L)
        assertThat(Money.parse("0.05")).isEqualTo(5L)
    }

    @Test
    fun parse_roundsHalfUpToPaisa() {
        assertThat(Money.parse("10.005")).isEqualTo(1_001L)
        assertThat(Money.parse("0.004")).isEqualTo(0L)
    }

    @Test
    fun parse_rejectsUnusableInput() {
        assertThat(Money.parse(null)).isNull()
        assertThat(Money.parse("")).isNull()
        assertThat(Money.parse("   ")).isNull()
        assertThat(Money.parse("abc")).isNull()
        assertThat(Money.parse(".")).isNull()
        assertThat(Money.parse("-5")).isNull()
    }

    @Test
    fun format_hidesZeroPaisaAndGroupsThousands() {
        assertThat(Money.format(420_000L)).isEqualTo("৳4,200")
        assertThat(Money.format(425_050L)).isEqualTo("৳4,250.50")
        assertThat(Money.format(5L)).isEqualTo("৳0.05")
        assertThat(Money.format(0L)).isEqualTo("৳0")
    }

    @Test
    fun format_marksSignsWhenAsked() {
        assertThat(Money.format(10_000L, signed = true)).isEqualTo("+৳100")
        assertThat(Money.format(-10_000L)).isEqualTo("−৳100")
    }

    @Test
    fun splitEvenly_alwaysSumsToTheTotal() {
        val totals = listOf(100L, 1L, 10_001L, 99_999L, 1_000_003L)
        val counts = listOf(1, 2, 3, 7, 12, 24, 60)
        totals.forEach { total ->
            counts.forEach { parts ->
                val pieces = Money.splitEvenly(total, parts)
                assertThat(pieces).hasSize(parts)
                assertThat(pieces.sum()).isEqualTo(total)
                assertThat(pieces.all { it >= 0L }).isTrue()
            }
        }
    }

    @Test
    fun splitEvenly_spreadsTheRemainderOnePaisaAtATime() {
        assertThat(Money.splitEvenly(10L, 3)).containsExactly(4L, 3L, 3L).inOrder()
        assertThat(Money.splitEvenly(100L, 4)).containsExactly(25L, 25L, 25L, 25L).inOrder()
    }

    @Test
    fun splitEvenly_rejectsNonPositiveCounts() {
        assertThat(Money.splitEvenly(100L, 0)).isEmpty()
        assertThat(Money.splitEvenly(100L, -3)).isEmpty()
    }

    @Test
    fun lineTotal_usesThousandthsWithoutDoubles() {
        // 12.50 x 1.5 = 18.75
        assertThat(Money.lineTotal(1_250L, 1_500L)).isEqualTo(1_875L)
        // 99.99 x 3 = 299.97
        assertThat(Money.lineTotal(9_999L, 3_000L)).isEqualTo(29_997L)
        assertThat(Money.lineTotal(1_250L, 0L)).isEqualTo(0L)
    }

    @Test
    fun applyRate_usesBasisPoints() {
        // 12.50% of 50,000.00
        assertThat(Money.applyRate(5_000_000L, 1_250L)).isEqualTo(625_000L)
        assertThat(Money.applyRate(5_000_000L, 0L)).isEqualTo(0L)
    }

    @Test
    fun safeArithmetic_clampsInsteadOfOverflowing() {
        assertThat(Money.safeAdd(Long.MAX_VALUE, 1L)).isEqualTo(Long.MAX_VALUE)
        assertThat(Money.safeAdd(Long.MIN_VALUE, -1L)).isEqualTo(Long.MIN_VALUE)
        assertThat(Money.safeSubtract(Long.MIN_VALUE, 1L)).isEqualTo(Long.MIN_VALUE)
        assertThat(Money.safeMultiply(Long.MAX_VALUE, 2L)).isEqualTo(Long.MAX_VALUE)
        assertThat(Money.safeMultiply(Long.MIN_VALUE, 2L)).isEqualTo(Long.MIN_VALUE)
    }

    @Test
    fun toDecimal_isExact() {
        assertThat(Money.toDecimal(100_050L).toPlainString()).isEqualTo("1000.50")
        assertThat(Money.toDecimal(-100_050L).toPlainString()).isEqualTo("-1000.50")
    }
}
