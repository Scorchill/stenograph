package com.stenograph.app.ui.dictation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stenograph.app.data.PreferencesManager
import com.stenograph.app.network.NetworkMonitor
import com.stenograph.app.network.ServiceDiscovery
import com.stenograph.app.network.StenographWebSocket
import com.stenograph.app.speech.SpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DictationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    val webSocket = StenographWebSocket(viewModelScope)
    private val discovery = ServiceDiscovery(application)
    val speechManager = SpeechManager(application)
    private val networkMonitor = NetworkMonitor(application)

    val connected: StateFlow<Boolean> = webSocket.connected
    val listening: StateFlow<Boolean> = speechManager.listening
    val authRejected: StateFlow<Boolean> = webSocket.authRejected
    val onWifi: StateFlow<Boolean> = networkMonitor.onWifi

    private val _previewText = MutableStateFlow("")
    val previewText: StateFlow<String> = _previewText

    private val _isDictating = MutableStateFlow(false)
    val isDictating: StateFlow<Boolean> = _isDictating

    init {
        speechManager.onPartialResult = { text ->
            _previewText.value = text
            webSocket.sendPartial(text)
        }
        speechManager.onFinalResult = { text ->
            _previewText.value = text
            webSocket.sendFinal(text)
        }

        viewModelScope.launch {
            combine(
                discovery.discoveredHost,
                prefs.pcIp,
                prefs.pcPort,
                prefs.authToken,
            ) { discovered, storedIp, storedPort, token ->
                if (token.isBlank()) return@combine null
                val (ip, port) = discovered ?: (storedIp to storedPort)
                if (ip.isBlank()) return@combine null
                Triple(ip, port, token)
            }.filterNotNull().distinctUntilChanged().collect { (ip, port, token) ->
                android.util.Log.i("DictationVM", "Connecting to $ip:$port")
                webSocket.disconnect()
                webSocket.connect(ip, port, token)
            }
        }

        discovery.startDiscovery()
    }

    /** Call from Activity.onResume to kick reconnection after backgrounding */
    fun onResume() {
        webSocket.ensureConnected()
        // Restart mDNS discovery in case PC IP changed
        discovery.stopDiscovery()
        discovery.startDiscovery()
    }

    fun toggleDictation() {
        if (_isDictating.value) {
            stopDictation()
        } else {
            startDictation()
        }
    }

    private fun startDictation() {
        _isDictating.value = true
        _previewText.value = ""
        speechManager.startListening()
    }

    private fun stopDictation() {
        _isDictating.value = false
        speechManager.stopListening()
        webSocket.sendStop()
        _previewText.value = ""
    }

    fun undo() {
        webSocket.sendUndo()
    }

    fun space() {
        webSocket.sendSpace()
    }

    fun backspace() {
        webSocket.sendBackspace()
    }

    fun clearPairing() {
        speechManager.stopListening()
        _isDictating.value = false
        webSocket.disconnect()
        discovery.stopDiscovery()
        viewModelScope.launch {
            prefs.clearPairing()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        webSocket.disconnect()
        discovery.stopDiscovery()
        networkMonitor.release()
    }
}
