package com.kenkosa.backend.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaterValidationTest {

    @Test
    fun normalIntake_isValid() {
        assertTrue(UpdateWaterRequest(8).validate().isEmpty())
    }

    @Test
    fun boundaries_zeroAndTwentyAreValid() {
        assertTrue(UpdateWaterRequest(0).validate().isEmpty())
        assertTrue(UpdateWaterRequest(20).validate().isEmpty())
    }

    @Test
    fun negativeCups_isRejected() {
        val errors = UpdateWaterRequest(-1).validate()
        assertEquals(1, errors.size)
        assertTrue(errors.first().contains("negative"))
    }

    @Test
    fun unrealisticCups_isRejected() {
        assertTrue(UpdateWaterRequest(21).validate().isNotEmpty())
    }
}