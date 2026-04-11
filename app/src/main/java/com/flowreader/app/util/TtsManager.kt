package com.flowreader.app.util

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
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
    val language: Locale = Locale.CHINESE
)

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "TtsManager"
        private const val MIN_RATE = 0.1f
        private const val MAX_RATE = 2.0f
    }

    private var textToSpeech: TextToSpeech? = null

    @Volatile
    private var isInitialized = false

    private val _ttsState = MutableStateFlow(TtsState.IDLE)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private val _settings = MutableStateFlow(TtsSettings())
    val settings: StateFlow<TtsSettings> = _settings.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private val utteranceCounter = AtomicLong(0)

    private var onCompletionCallback: (() -> Unit)? = null

    fun initialize(onInit: () -> Unit = {}) {
        // Prevent resource leak by shutting down existing instance
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }

        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                textToSpeech?.language = _settings.value.language
                textToSpeech?.setSpeechRate(_settings.value.speechRate)
                textToSpeech?.setPitch(_settings.value.pitch)

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch { _ttsState.update { TtsState.PLAYING } }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch {
                            _ttsState.update { TtsState.IDLE }
                            onCompletionCallback?.invoke()
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        scope.launch {
                            Log.e(TAG, "TTS error: $errorCode")
                            _ttsState.update { TtsState.ERROR }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onError(utteranceId, -1)
                    }
                })
                onInit()
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                scope.launch { _ttsState.update { TtsState.ERROR } }
            }
        }
    }

    fun speak(text: String, onCompletion: () -> Unit = {}) {
        require(text.isNotBlank()) { "Text must not be blank" }

        if (textToSpeech == null || !isInitialized) {
            initialize {
                speak(text, onCompletion)
            }
            return
        }

        onCompletionCallback = onCompletion
        scope.launch { _ttsState.update { TtsState.PLAYING } }

        val utteranceId = "utterance_${utteranceCounter.incrementAndGet()}"

        try {
            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
            scope.launch { _ttsState.update { TtsState.ERROR } }
        }
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        scope.launch { _ttsState.update { TtsState.IDLE } }
    }

    fun pause() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing TTS", e)
        }
        scope.launch { _ttsState.update { TtsState.PAUSED } }
    }

    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(MIN_RATE, MAX_RATE)
        _settings.value = _settings.value.copy(speechRate = clampedRate)
        textToSpeech?.setSpeechRate(clampedRate)
    }

    fun setPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(MIN_RATE, MAX_RATE)
        _settings.value = _settings.value.copy(pitch = clampedPitch)
        textToSpeech?.setPitch(clampedPitch)
    }

    fun setLanguage(locale: Locale) {
        _settings.value = _settings.value.copy(language = locale)
        textToSpeech?.language = locale
    }

    /**
     * Get current volume (0.0 to 1.0) from AudioManager
     */
    fun getVolume(): Float {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) currentVolume / maxVolume.toFloat() else 1.0f
    }

    fun isSpeaking(): Boolean = textToSpeech?.isSpeaking == true

    fun isAvailable(): Boolean = textToSpeech != null && isInitialized

    /**
     * Release TTS resources. Should be called when the manager is no longer needed.
     * Hilt will invoke this when the application is terminated.
     */
    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        } finally {
            textToSpeech = null
            isInitialized = false
            onCompletionCallback = null
            scope.launch { _ttsState.update { TtsState.IDLE } }
        }
    }
}
