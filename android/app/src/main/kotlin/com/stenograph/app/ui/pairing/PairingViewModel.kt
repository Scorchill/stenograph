package com.stenograph.app.ui.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stenograph.app.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class PairingStep {
    CAMERA_PERMISSION,
    SCANNING,
    PROCESSING,
    SUCCESS,
    ERROR
}

data class PairingUiState(
    val step: PairingStep = PairingStep.CAMERA_PERMISSION,
    val errorMessage: String? = null,
)

class PairingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun onCameraPermissionGranted() {
        _uiState.value = _uiState.value.copy(step = PairingStep.SCANNING)
    }

    fun onCameraPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            step = PairingStep.ERROR,
            errorMessage = "Camera permission is required to scan the QR code from your PC."
        )
    }

    /**
     * Called when a QR code is successfully scanned.
     * Expected payload: {"app":"stenograph","version":1,"token":"xxx","port":9476,"ip":"192.168.x.x"}
     */
    fun onQrCodeScanned(content: String) {
        _uiState.value = _uiState.value.copy(step = PairingStep.PROCESSING)

        val parsed = parseQrPayload(content)
        if (parsed == null) {
            _uiState.value = _uiState.value.copy(
                step = PairingStep.ERROR,
                errorMessage = "Invalid QR code. Please scan the QR code shown in Stenograph on your PC."
            )
            return
        }

        viewModelScope.launch {
            try {
                android.util.Log.i("PairingVM", "Saving pairing: ip=${parsed.ip}, port=${parsed.port}, token=${parsed.token.take(8)}...")
                prefs.savePairingInfo(
                    token = parsed.token,
                    ip = parsed.ip,
                    port = parsed.port,
                )
                _uiState.value = _uiState.value.copy(step = PairingStep.SUCCESS)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    step = PairingStep.ERROR,
                    errorMessage = "Pairing failed: ${e.message}"
                )
            }
        }
    }

    fun retry() {
        _uiState.value = PairingUiState(step = PairingStep.SCANNING)
    }

    private data class QrPayload(val token: String, val ip: String, val port: Int)

    private fun parseQrPayload(content: String): QrPayload? {
        return try {
            val json = JSONObject(content)
            if (json.optString("app") != "stenograph") return null
            val token = json.getString("token")
            val ip = json.getString("ip")
            val port = json.getInt("port")
            if (token.isBlank() || ip.isBlank()) return null
            QrPayload(token, ip, port)
        } catch (_: Exception) {
            null
        }
    }
}
