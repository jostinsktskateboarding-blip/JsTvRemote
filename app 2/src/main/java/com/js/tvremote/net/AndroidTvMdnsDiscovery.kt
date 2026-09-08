package com.js.tvremote.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Discovers the standard Android TV Remote v2 mDNS service. */
object AndroidTvMdnsDiscovery {
    private const val SERVICE = "_androidtvremote2._tcp."

    suspend fun discover(context: Context, timeoutMs: Int): List<TvDevice> =
        suspendCancellableCoroutine { cont ->
            val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            val results = LinkedHashMap<String, TvDevice>()
            var stopped = false
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (serviceInfo.serviceType != SERVICE) return
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val ip = info.host?.hostAddress ?: return
                            val name = info.attributes["fn"]?.toString(Charsets.UTF_8)
                                ?: info.serviceName ?: "Android TV"
                            results[ip] = TvDevice(name, ip, Brand.ANDROID_TV)
                        }
                    })
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { finish() }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { finish() }
                private fun finish() {
                    if (stopped) return
                    stopped = true
                    try { nsd.stopServiceDiscovery(this) } catch (_: Exception) {}
                    if (cont.isActive) cont.resume(results.values.toList())
                }
            }
            nsd.discoverServices(SERVICE, NsdManager.PROTOCOL_DNS_SD, listener)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.postDelayed({
                if (!stopped) {
                    stopped = true
                    try { nsd.stopServiceDiscovery(listener) } catch (_: Exception) {}
                    if (cont.isActive) cont.resume(results.values.toList())
                }
            }, timeoutMs.toLong())
            cont.invokeOnCancellation { try { nsd.stopServiceDiscovery(listener) } catch (_: Exception) {} }
        }
}
