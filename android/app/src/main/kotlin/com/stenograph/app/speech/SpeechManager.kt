package com.stenograph.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpeechManager(private val context: Context) {

    var onPartialResult: ((String) -> Unit)? = null
    var onFinalResult: ((String) -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening

    fun startListening() {
        if (isListening) return
        isListening = true
        _listening.value = true
        createAndStart()
    }

    fun stopListening() {
        isListening = false
        _listening.value = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun createAndStart() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("Speech", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = texts?.firstOrNull() ?: return
                if (text.isNotBlank()) {
                    onPartialResult?.invoke(text)
                }
            }

            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = texts?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    onFinalResult?.invoke(text)
                }
                // Auto-restart if still listening
                if (isListening) {
                    createAndStart()
                }
            }

            override fun onError(error: Int) {
                Log.w("Speech", "Error: $error")
                // Auto-restart on recoverable errors
                if (isListening && error in listOf(
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_CLIENT,
                )) {
                    createAndStart()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }
}
