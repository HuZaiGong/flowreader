package com.flowreader.app.ui.screens.reader

import com.flowreader.app.util.TtsManager

/**
 * Small TTS coordination seam for [ReaderViewModel]: decides what to speak from a reading
 * position and exposes pause / stop / shutdown. The speech engine itself stays in [TtsManager].
 *
 * Extracted from `ReaderViewModel` in v54 so the ViewModel only orchestrates.
 */
class ReaderTtsCoordinator(private val ttsManager: TtsManager) {

    val state = ttsManager.state

    val isSpeaking: Boolean
        get() = ttsManager.state.value.isSpeaking

    /** Speaks from [position] to the end of the chapter, restarting the chapter when empty. */
    fun speakFrom(content: String, position: Int) {
        val start = position.coerceIn(0, content.length)
        val text = content.substring(start).ifBlank { content }
        ttsManager.speak(text)
    }

    fun pause() {
        ttsManager.pause()
    }

    fun stop() {
        ttsManager.stop()
    }

    fun shutdown() {
        ttsManager.shutdown()
    }
}
