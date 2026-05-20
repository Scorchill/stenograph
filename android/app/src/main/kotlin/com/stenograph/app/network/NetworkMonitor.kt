package com.stenograph.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks whether the phone is currently connected to a Wi-Fi network.
 *
 * Stenograph only works when the phone and PC share a local network, so a phone
 * on mobile data (or with Wi-Fi off) is the most common reason a connection
 * silently fails. [onWifi] exposes that state so the UI can explain the problem
 * instead of showing a generic "disconnected".
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _onWifi = MutableStateFlow(currentlyOnWifi())

    /** True when the phone's active network is Wi-Fi. */
    val onWifi: StateFlow<Boolean> = _onWifi

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _onWifi.value = currentlyOnWifi()
        }

        override fun onLost(network: Network) {
            _onWifi.value = currentlyOnWifi()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            _onWifi.value = currentlyOnWifi()
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    /** Stop listening for network changes. Call when the owner is destroyed. */
    fun release() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: IllegalArgumentException) {
            // Callback was already unregistered — nothing to do.
        }
    }

    private fun currentlyOnWifi(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
