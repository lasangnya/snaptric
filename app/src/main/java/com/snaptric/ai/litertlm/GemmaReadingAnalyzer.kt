package com.snaptric.ai.litertlm

import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Reads a meter with a multimodal Gemma model running on-device through LiteRT-LM.
 * Used as a fallback when ML Kit can't find a clear number (analog dials, glare, low light).
 *
 * The engine takes several seconds to load, so it is created on first use and kept for the
 * lifetime of the process.
 */
class GemmaReadingAnalyzer(
    private val modelLocator: GemmaModelLocator,
    private val cacheDir: File
) : MeterReadingAnalyzer {

    private val mutex = Mutex()
    private var engine: Engine? = null

    /** True when a model file is installed on the device. */
    fun isAvailable(): Boolean = modelLocator.findModel() != null

    override suspend fun analyze(bitmap: Bitmap): Reading = withContext(Dispatchers.Default) {
        val response = mutex.withLock {
            engineOrInit().createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(SYSTEM_INSTRUCTION),
                    // Deterministic output: we want the digits, not creativity.
                    samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)
                )
            ).use { conversation ->
                conversation.sendMessage(
                    Contents.of(
                        Content.ImageBytes(bitmap.toPngBytes()),
                        Content.Text(USER_PROMPT)
                    )
                )
            }
        }
        val text = response.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString("") { it.text }

        Reading(
            value = parseMeterReading(text).orEmpty(),
            timestamp = System.currentTimeMillis(),
            source = SOURCE
        )
    }

    private fun engineOrInit(): Engine =
        engine ?: run {
            val model = checkNotNull(modelLocator.findModel()) { "No Gemma model installed" }
            Engine(
                EngineConfig(
                    modelPath = model.absolutePath,
                    backend = Backend.CPU(),
                    visionBackend = Backend.GPU(),
                    cacheDir = cacheDir.absolutePath
                )
            ).also {
                it.initialize()
                engine = it
            }
        }

    private fun Bitmap.toPngBytes(): ByteArray =
        ByteArrayOutputStream().use { out ->
            compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }

    companion object {
        const val SOURCE = "Gemma"

        private const val SYSTEM_INSTRUCTION =
            "You read utility meters (electricity, gas, water) from photos."

        private const val USER_PROMPT =
            "What is the meter reading in this image? Read the main counter from left to right. " +
                "If there is a decimal section (often red digits or a separate box), put a '.' before it. " +
                "Ignore serial numbers, labels and units. Reply with the number only, or NONE if you can't read it."
    }
}

/**
 * Extracts the meter number from a model reply such as "12345.6", "Reading: 0012 345" or "NONE".
 * Returns the longest number found, with ',' treated as a decimal point, or null if there is none.
 */
fun parseMeterReading(text: String): String? =
    Regex("""\d[\d ]*(?:[.,]\d+)?""")
        .findAll(text)
        .map { it.value.replace(" ", "").replace(',', '.') }
        .maxByOrNull { it.count(Char::isDigit) }
