package com.snaptric.ai.mlkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StableReadingDetectorTest {

    @Test
    fun `same value three frames in a row is stable`() {
        val detector = StableReadingDetector(requiredFrames = 3)
        assertNull(detector.offer("01262"))
        assertNull(detector.offer("01262"))
        assertEquals("01262", detector.offer("01262"))
    }

    @Test
    fun `a changing value restarts the count`() {
        val detector = StableReadingDetector(requiredFrames = 3)
        detector.offer("01262")
        detector.offer("01268")
        assertNull(detector.offer("01268"))
        assertEquals("01268", detector.offer("01268"))
    }

    @Test
    fun `missing or short values break the streak`() {
        val detector = StableReadingDetector(requiredFrames = 3)
        detector.offer("01262")
        detector.offer("01262")
        assertNull(detector.offer(null))
        assertNull(detector.offer("01262"))
        assertNull(detector.offer("50"))
    }

    @Test
    fun `after firing it needs a fresh streak`() {
        val detector = StableReadingDetector(requiredFrames = 3)
        repeat(3) { detector.offer("01262") }
        assertNull(detector.offer("01262"))
    }

    @Test
    fun `gate fires on a steady stream of identical frames`() {
        val gate = AutoCaptureGate(StableReadingDetector(requiredFrames = 3))
        val fired = (1..3).map { gate.shouldCapture("01262", ready = true) }
        assertEquals(listOf(false, false, true), fired)
    }

    @Test
    fun `gate doesn't re-capture the same value after the sheet closes`() {
        val gate = AutoCaptureGate(StableReadingDetector(requiredFrames = 3))
        repeat(3) { gate.shouldCapture("01262", ready = true) }
        repeat(2) { gate.shouldCapture("01262", ready = false) } // sheet open
        val again = (1..5).map { gate.shouldCapture("01262", ready = true) }
        assertEquals(List(5) { false }, again)

        // A different reading still triggers.
        val next = (1..3).map { gate.shouldCapture("01270", ready = true) }
        assertEquals(listOf(false, false, true), next)
    }

    @Test
    fun `by default two matching frames are enough`() {
        val gate = AutoCaptureGate()
        assertEquals(listOf(false, true), (1..2).map { gate.shouldCapture("00561", ready = true) })
    }
}
