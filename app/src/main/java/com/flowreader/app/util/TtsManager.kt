package com.flowreader.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext context: Context
) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            tts.language = Locale.getDefault()
            pendingText?.let { speak(it) }
            pendingText = null
        }
    }

    fun speak(text: String) {
        val content = text.take(3800)
        if (!isReady) {
            pendingText = content
            return
        }
        tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, "reader-content")
    }

    fun pause() {
        tts.stop()
    }

    fun stop() {
        tts.stop()
        pendingText = null
    }

    fun shutdown() {
        tts.shutdown()
    }
}
