package com.flowreader.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class TtsState {
    IDLE,
    PLAYING,
    PAUSED,
    ERROR
}

data class TtsSettings(
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val language: Locale = Locale.CHINESE,
    val volume: Float = 1.0f
)

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _ttsState = MutableStateFlow(TtsState.IDLE)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private val _settings = MutableStateFlow(TtsSettings())
    val settings: StateFlow<TtsSettings> = _settings.asStateFlow()

    private var currentText: String = ""
    private var onCompletionCallback: (() -> Unit)? = null

    fun initialize(onInit: () -> Unit = {}) {
        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                textToSpeech?.language = _settings.value.language
                textToSpeech?.setSpeechRate(_settings.value.speechRate)
                textToSpeech?.setPitch(_settings.value.pitch)
                
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _ttsState.value = TtsState.PLAYING
                    }

                    override fun onDone(utteranceId: String?) {
                        _ttsState.value = TtsState.IDLE
                        onCompletionCallback?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _ttsState.value = TtsState.ERROR
                    }
                })
            }
            onInit()
        }
    }

    fun speak(text: String, onCompletion: () -> Unit = {}) {
        if (!isInitialized) {
            initialize {
                speak(text, onCompletion)
            }
            return
        }

        currentText = text
        onCompletionCallback = onCompletion
        _ttsState.value = TtsState.PLAYING
        
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "utterance_${System.currentTimeMillis()}"
        )
    }

    fun stop() {
        textToSpeech?.stop()
        _ttsState.value = TtsState.IDLE
    }

    fun pause() {
        textToSpeech?.stop()
        _ttsState.value = TtsState.PAUSED
    }

    fun setSpeechRate(rate: Float) {
        _settings.value = _settings.value.copy(speechRate = rate)
        textToSpeech?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        _settings.value = _settings.value.copy(pitch = pitch)
        textToSpeech?.setPitch(pitch)
    }

    fun setLanguage(locale: Locale) {
        _settings.value = _settings.value.copy(language = locale)
        textToSpeech?.language = locale
    }

    fun isSpeaking(): Boolean = textToSpeech?.isSpeaking == true

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        _ttsState.value = TtsState.IDLE
    }
}
