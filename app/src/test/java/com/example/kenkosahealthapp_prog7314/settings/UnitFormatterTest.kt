package com.example.kenkosahealthapp_prog7314

import com.example.kenkosahealthapp_prog7314.data.model.UnitConversions
import com.example.kenkosahealthapp_prog7314.ui.settings.UnitFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit conversions behind the Metric / Imperial / UK setting. */
class UnitFormatterTest {

    @Test
    fun kgToLb_isCorrect() {
        assertEquals(154.3, UnitFormatter.kgToLb(70.0), 0.1)
    }

    @Test
    fun mlToFlOz_oneCupIsAboutEightOunces() {
        assertEquals(8.45, UnitFormatter.mlToFlOz(250.0), 0.01)
    }

    @Test
    fun gToOz_hundredGramsIsAboutThreePointFiveOunces() {
        assertEquals(3.53, UnitFormatter.gToOz(100.0), 0.01)
    }

    @Test
    fun stonesAndPounds_splitsCorrectly() {
        val (stones, pounds) = UnitFormatter.stonesAndPounds(70.0)
        assertEquals(11, stones)
        assertEquals(0.3, pounds, 0.1)
    }

    @Test
    fun feetAndInchesToCm_isCorrect() {
        assertEquals(175.26, UnitConversions.ftInToCm(5, 9.0), 0.05)
    }

    @Test
    fun cmToFeetAndInches_isCorrect() {
        val (feet, inches) = UnitConversions.cmToFtIn(175.26)
        assertEquals(5, feet)
        assertEquals(9.0, inches, 0.1)
    }

    @Test
    fun lbToKg_isCorrect() {
        assertEquals(70.0, UnitConversions.lbToKg(154.3), 0.1)
    }
}