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

data class TtsState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val error: String? = null
)

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext context: Context
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()
    private var pendingText: String? = null

    private fun ensureEngine() {
        if (tts != null) return
        try {
            tts = TextToSpeech(appContext, this).also { engine ->
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = _state.value.copy(isSpeaking = true, error = null)
            }

            override fun onDone(utteranceId: String?) {
                _state.value = _state.value.copy(isSpeaking = false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = _state.value.copy(isSpeaking = false, error = "朗读失败")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = _state.value.copy(isSpeaking = false, error = "朗读失败: $errorCode")
            }
                })
            }
        } catch (e: Exception) {
            _state.value = TtsState(error = "TTS 初始化失败: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    override fun onInit(status: Int) {
        val ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val engine = tts ?: return
            val languageResult = engine.setLanguage(Locale.getDefault())
            val unsupported = languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            _state.value = TtsState(isReady = !unsupported, error = if (unsupported) "当前系统语言不支持朗读" else null)
            pendingText?.let { text ->
                pendingText = null
                speak(text)
            }
        } else {
            _state.value = TtsState(error = "TTS 初始化失败")
        }
    }

    fun speak(text: String) {
        val content = text.replace(Regex("\\s+"), " ").trim().take(3800)
        if (content.isBlank()) return
        ensureEngine()
        if (!_state.value.isReady) {
            pendingText = content
            return
        }
        tts?.speak(content, TextToSpeech.QUEUE_FLUSH, null, "reader-content")
    }

    fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun stop() {
        tts?.stop()
        pendingText = null
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        pendingText = null
        _state.value = TtsState()
    }
}
