package com.snaptric.ai.litertlm

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runs a text-only Gemma model on-device through LiteRT-LM.
 *
 * The engine takes several seconds to load, so it is created on first use and kept for the
 * lifetime of the process. Calls are serialised because one engine serves one request at a time.
 */
class GemmaTextEngine(
    private val modelLocator: GemmaModelLocator,
    private val cacheDir: File
) {
    private val mutex = Mutex()
    private var engine: Engine? = null

    /** True when a model file is installed on the device. */
    fun isAvailable(): Boolean = modelLocator.findModel() != null

    /**
     * Sends one prompt in a fresh conversation and returns the reply text,
     * or null when no model is installed.
     */
    suspend fun generate(systemInstruction: String, prompt: String): String? = withContext(Dispatchers.Default) {
        mutex.withLock {
            val engine = engineOrInit() ?: return@withLock null
            engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemInstruction),
                    // Low temperature: the wording may vary a little, the facts may not.
                    samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.3)
                )
            ).use { conversation ->
                conversation.sendMessage(prompt).contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                    .trim()
            }
        }
    }

    /**
     * Unloads the model, e.g. before its file is deleted. It loads again on next use.
     */
    suspend fun close() {
        mutex.withLock {
            engine?.close()
            engine = null
        }
    }

    private fun engineOrInit(): Engine? =
        engine ?: modelLocator.findModel()?.let { model ->
            Engine(
                EngineConfig(
                    modelPath = model.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = cacheDir.absolutePath
                )
            ).also {
                it.initialize()
                engine = it
            }
        }
}
