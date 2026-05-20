package com.stenograph.app.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ServiceDiscovery(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredHost = MutableStateFlow<Pair<String, Int>?>(null)
    val discoveredHost: StateFlow<Pair<String, Int>?> = _discoveredHost

    fun startDiscovery() {
        stopDiscovery()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.i("Discovery", "Started looking for _stenograph._tcp")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.i("Discovery", "Found service: ${serviceInfo.serviceName}")
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {
                        Log.w("Discovery", "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(si: NsdServiceInfo) {
                        val host = si.host?.hostAddress ?: return
                        val port = si.port
                        Log.i("Discovery", "Resolved: $host:$port")
                        _discoveredHost.value = Pair(host, port)
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.i("Discovery", "Service lost: ${serviceInfo.serviceName}")
                _discoveredHost.value = null
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i("Discovery", "Discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w("Discovery", "Start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w("Discovery", "Stop failed: $errorCode")
            }
        }

        nsdManager.discoverServices("_stenograph._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.w("Discovery", "Stop error: ${e.message}")
            }
        }
        discoveryListener = null
    }
}
