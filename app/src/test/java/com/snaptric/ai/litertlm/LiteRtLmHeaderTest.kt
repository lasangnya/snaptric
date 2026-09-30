package com.snaptric.ai.litertlm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LiteRtLmHeaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `file with the LiteRT-LM magic is accepted`() {
        val file = tmp.newFile("gemma.litertlm").apply { writeBytes("LITERTLM".toByteArray() + ByteArray(32)) }
        assertTrue(hasLiteRtLmHeader(file))
    }

    @Test
    fun `file with other content is rejected`() {
        val file = tmp.newFile("broken.litertlm").apply { writeBytes(ByteArray(64) { it.toByte() }) }
        assertFalse(hasLiteRtLmHeader(file))
    }

    @Test
    fun `file shorter than the magic is rejected`() {
        val file = tmp.newFile("short.litertlm").apply { writeBytes("LITE".toByteArray()) }
        assertFalse(hasLiteRtLmHeader(file))
    }
}
