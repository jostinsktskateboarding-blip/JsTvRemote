package com.js.tvremote.net

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.util.concurrent.TimeUnit

data class TvDevice(
    val name: String,
    val ip: String,
    val brand: Brand,
    val locationUrl: String? = null
)

enum class Brand { ANDROID_TV, ROKU, LG_WEBOS, SAMSUNG, GENERIC_DLNA, UNKNOWN }

object SsdpDiscovery {
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private val SEARCH_TARGETS = listOf(
        "ssdp:all",
        "urn:dial-multiscreen-org:service:dial:1",
        "urn:schemas-upnp-org:device:MediaRenderer:1"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(800, TimeUnit.MILLISECONDS)
        .readTimeout(800, TimeUnit.MILLISECONDS)
        .build()

    suspend fun discover(context: Context, timeoutMs: Int = 3500): List<TvDevice> =
        withContext(Dispatchers.IO) {
            val found = LinkedHashMap<String, TvDevice>()
            AndroidTvMdnsDiscovery.discover(context, timeoutMs).forEach { found[it.ip] = it }
            val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
            val multicastLock = wifiManager?.createMulticastLock("JsTvRemote-SSDP")?.apply {
                setReferenceCounted(false)
                acquire()
            }

            try {
                MulticastSocket().use { socket ->
                    socket.reuseAddress = true
                    socket.soTimeout = 500
                    val group = InetAddress.getByName(SSDP_ADDRESS)

                    SEARCH_TARGETS.forEach { target ->
                        val message = "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 1\r\n" +
                            "ST: $target\r\n\r\n"
                        val data = message.toByteArray(Charsets.US_ASCII)
                        socket.send(DatagramPacket(data, data.size, InetSocketAddress(group, SSDP_PORT)))
                    }

                    val endTime = System.currentTimeMillis() + timeoutMs
                    val buffer = ByteArray(8192)
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            val response = DatagramPacket(buffer, buffer.size)
                            socket.receive(response)
                            val text = String(response.data, 0, response.length, Charsets.UTF_8)
                            val ip = response.address.hostAddress ?: continue
                            val locationUrl = extractHeader(text, "LOCATION")
                            val serverHeader = extractHeader(text, "SERVER") ?: ""
                            val stHeader = extractHeader(text, "ST") ?: ""
                            val friendlyName = locationUrl?.let(::fetchFriendlyName) ?: ip
                            val brand = guessBrand(serverHeader, friendlyName, stHeader, locationUrl ?: "")
                            found.putIfAbsent(ip, TvDevice(friendlyName, ip, brand, locationUrl))
                        } catch (_: java.net.SocketTimeoutException) {
                            // Continue receiving until the overall discovery window ends.
                        } catch (_: Exception) {
                            // Ignore malformed/unreachable devices.
                        }
                    }
                }
            } finally {
                if (multicastLock?.isHeld == true) multicastLock.release()
            }
            found.values.toList()
        }

    private fun extractHeader(raw: String, header: String): String? =
        raw.lineSequence().firstOrNull { it.startsWith(header, ignoreCase = true) }
            ?.substringAfter(":")?.trim()

    private fun fetchFriendlyName(locationUrl: String): String? = try {
        val request = Request.Builder().url(locationUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            Regex("<friendlyName>([^<]+)</friendlyName>", RegexOption.IGNORE_CASE)
                .find(body)?.groupValues?.getOrNull(1)?.trim()
        }
    } catch (_: Exception) {
        null
    }

    private fun guessBrand(server: String, name: String, st: String, location: String): Brand {
        val combined = "$server $name $st $location".lowercase()
        return when {
            "roku" in combined -> Brand.ROKU
            "webos" in combined || "lg electronics" in combined || " lg " in " $combined " -> Brand.LG_WEBOS
            "samsung" in combined || "tizen" in combined -> Brand.SAMSUNG
            "mediarenderer" in combined -> Brand.GENERIC_DLNA
            else -> Brand.UNKNOWN
        }
    }
}
