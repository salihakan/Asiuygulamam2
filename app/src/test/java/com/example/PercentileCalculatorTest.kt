package com.example

import com.example.util.DoseCalculator
import com.example.util.PercentileCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PercentileCalculatorTest {

    @Test
    fun testAgeCalculation() {
        val age = DoseCalculator.calculateDecimalAge("2019-05-15", "2026-05-15")
        assertEquals(7.0f, age, 0.05f)
    }

    @Test
    fun testPercentileInterpolationMaleHeight() {
        // At 7 years old, male height 50th percentile is ~122.0 cm
        val pMid = PercentileCalculator.calculatePercentile(122.0f, 7.0f, true, true)
        assertTrue("Expected percentile around 50, got $pMid", pMid in 45..55)

        // At 7 years old, male height 108 cm is very low (< 3rd percentile)
        val pLow = PercentileCalculator.calculatePercentile(108.0f, 7.0f, true, true)
        assertTrue("Expected low percentile, got $pLow", pLow <= 3)

        // At 7 years old, male height 135 cm is high (> 97th percentile)
        val pHigh = PercentileCalculator.calculatePercentile(135.0f, 7.0f, true, true)
        assertTrue("Expected high percentile, got $pHigh", pHigh >= 97)
    }

    @Test
    fun testPercentileInterpolationFemaleWeight() {
        // At 8 years old, female weight 50th percentile is ~25.5 kg
        val pMid = PercentileCalculator.calculatePercentile(25.5f, 8.0f, false, false)
        assertTrue("Expected percentile around 50, got $pMid", pMid in 45..55)
    }
}
