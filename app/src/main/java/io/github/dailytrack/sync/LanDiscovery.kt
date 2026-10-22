/*
 * Copyright 2024 Soul Track Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.github.dailytrack.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local-network discovery. Files never touch the internet here; mDNS only
 * tells already-paired devices where to find each other on the same Wi-Fi.
 */
class LanDiscovery(
    context: Context,
    private val store: PeerStore,
    private val onPeerFound: (deviceId: String, host: String, port: Int) -> Unit
) {
    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val running = AtomicBoolean(false)
    private val resolving = ConcurrentHashMap<String, Boolean>()
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun start(port: Int) {
        if (!running.compareAndSet(false, true)) return
        register(port)
        discover()
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        registrationListener = null
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
        resolving.clear()
    }

    private fun register(port: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = "SoulTrack-${store.deviceId.takeLast(10)}"
            serviceType = SERVICE_TYPE
            setPort(port)
            runCatching { setAttribute("id", store.deviceId) }
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "mDNS registered")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.d(TAG, "mDNS registration failed ($errorCode)")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListener = listener
        runCatching { nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { Log.d(TAG, "mDNS register threw ${it.javaClass.simpleName}") }
    }

    private fun discover() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.d(TAG, "mDNS discovery failed ($errorCode)")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                if (serviceInfo.serviceName == "SoulTrack-${store.deviceId.takeLast(10)}") return
                resolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
        }
        discoveryListener = listener
        runCatching { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { Log.d(TAG, "mDNS discover threw ${it.javaClass.simpleName}") }
    }

    @Suppress("DEPRECATION")
    private fun resolve(info: NsdServiceInfo) {
        val key = info.serviceName
        if (resolving.putIfAbsent(key, true) != null) return
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                resolving.remove(key)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                resolving.remove(key)
                val deviceId = serviceInfo.attributes?.get("id")
                    ?.toString(Charsets.UTF_8)
                    ?.takeIf { it.isNotBlank() }
                    ?: store.peers().firstOrNull {
                        serviceInfo.serviceName.endsWith(it.deviceId.takeLast(10))
                    }?.deviceId
                    ?: return
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port
                if (port <= 0) return
                store.updateAddress(deviceId, host, port)
                onPeerFound(deviceId, host, port)
            }
        }
        runCatching { nsdManager.resolveService(info, listener) }
            .onFailure { resolving.remove(key) }
    }

    companion object {
        private const val TAG = "P2pSync"
        const val SERVICE_TYPE = "_soultrack._tcp."
    }
}
