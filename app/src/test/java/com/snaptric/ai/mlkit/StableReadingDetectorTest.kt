package com.snaptric.ai.mlkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StableReadingDetectorTest {

    @Test
    fun `same value three frames in a row is stable`() {
        val detector = StableReadingDetector()
        assertNull(detector.offer("01262"))
        assertNull(detector.offer("01262"))
        assertEquals("01262", detector.offer("01262"))
    }

    @Test
    fun `a changing value restarts the count`() {
        val detector = StableReadingDetector()
        detector.offer("01262")
        detector.offer("01268")
        assertNull(detector.offer("01268"))
        assertEquals("01268", detector.offer("01268"))
    }

    @Test
    fun `missing or short values break the streak`() {
        val detector = StableReadingDetector()
        detector.offer("01262")
        detector.offer("01262")
        assertNull(detector.offer(null))
        assertNull(detector.offer("01262"))
        assertNull(detector.offer("50"))
    }

    @Test
    fun `after firing it needs a fresh streak`() {
        val detector = StableReadingDetector()
        repeat(3) { detector.offer("01262") }
        assertNull(detector.offer("01262"))
    }
}
