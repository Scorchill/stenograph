package com.stenograph.app.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StenographWebSocket(
    private val scope: CoroutineScope,
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES) // no read timeout for long-lived connections
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var currentIp: String = ""
    private var currentPort: Int = 9476
    private var currentToken: String = ""
    private var shouldReconnect = false
    private var reconnectDelay = INITIAL_DELAY

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    // Exposed so UI can show "Re-pair needed" vs "Disconnected"
    private val _authRejected = MutableStateFlow(false)
    val authRejected: StateFlow<Boolean> = _authRejected

    fun connect(ip: String, port: Int, token: String) {
        currentIp = ip
        currentPort = port
        currentToken = token
        shouldReconnect = true
        _authRejected.value = false
        reconnectDelay = INITIAL_DELAY
        doConnect()
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connected.value = false
    }

    /** Call from onResume to kick reconnection if needed */
    fun ensureConnected() {
        if (!_connected.value && shouldReconnect && !_authRejected.value) {
            reconnectDelay = INITIAL_DELAY
            scheduleReconnect()
        }
    }

    fun sendPartial(text: String) {
        send(JSONObject().put("type", "partial").put("text", text).toString())
    }

    fun sendFinal(text: String) {
        send(JSONObject().put("type", "final").put("text", text).toString())
    }

    fun sendUndo() {
        send(JSONObject().put("type", "undo").toString())
    }

    fun sendStop() {
        send(JSONObject().put("type", "stop").toString())
    }

    fun sendSpace() {
        send(JSONObject().put("type", "space").toString())
    }

    fun sendBackspace() {
        send(JSONObject().put("type", "backspace").toString())
    }

    private fun send(message: String) {
        webSocket?.send(message)
    }

    private fun doConnect() {
        webSocket?.cancel()
        val request = Request.Builder()
            .url("ws://$currentIp:$currentPort")
            .header("Authorization", "Bearer $currentToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Connected to PC at $currentIp:$currentPort")
                _connected.value = true
                _authRejected.value = false
                reconnectDelay = INITIAL_DELAY // reset backoff on success
                reconnectJob?.cancel()
                reconnectJob = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connected.value = false

                // Detect 403 auth rejection — stop retrying, need re-pair
                if (response?.code == 403 || t.message?.contains("403") == true) {
                    Log.w(TAG, "Auth rejected (403) — re-pair needed")
                    _authRejected.value = true
                    shouldReconnect = false
                    return
                }

                Log.w(TAG, "Connection failed: ${t.message}")
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Disconnected: code=$code reason=$reason")
                _connected.value = false
                if (code != 1000 && shouldReconnect) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        // Don't start a new reconnect loop if one is already running
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            while (isActive && shouldReconnect && !_connected.value && !_authRejected.value) {
                delay(reconnectDelay)
                if (!isActive || !shouldReconnect || _connected.value) break
                Log.i(TAG, "Reconnecting in ${reconnectDelay}ms...")
                doConnect()
                reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_DELAY)
                // Wait for the connection attempt to resolve before looping
                delay(reconnectDelay)
            }
        }
    }

    companion object {
        private const val TAG = "StenographWS"
        private const val INITIAL_DELAY = 500L
        private const val MAX_DELAY = 30_000L
    }
}
