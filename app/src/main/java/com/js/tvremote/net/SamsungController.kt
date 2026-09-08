package com.js.tvremote.net

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SamsungController {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .build()

    enum class Key(val samsungKey: String) {
        HOME("KEY_HOME"), BACK("KEY_RETURN"), ENTER("KEY_ENTER"),
        UP("KEY_UP"), DOWN("KEY_DOWN"), LEFT("KEY_LEFT"), RIGHT("KEY_RIGHT"),
        VOLUME_UP("KEY_VOLUP"), VOLUME_DOWN("KEY_VOLDOWN"), MUTE("KEY_MUTE"),
        PLAY("KEY_PLAY"), REWIND("KEY_REWIND"), FORWARD("KEY_FF"),
        POWER("KEY_POWER")
    }

    fun connectAndSend(ip: String, appName: String, key: Key) {
        open(ip, appName, key, secure = false)
    }

    private fun open(ip: String, appName: String, key: Key, secure: Boolean) {
        val nameB64 = Base64.encodeToString(appName.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val encodedName = URLEncoder.encode(nameB64, Charsets.UTF_8.name())
        val port = if (secure) 8002 else 8001
        val scheme = if (secure) "wss" else "ws"
        val url = "$scheme://$ip:$port/api/v2/channels/samsung.remote.control?name=$encodedName"
        val request = Request.Builder().url(url).build()

        client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (json.optString("event") == "ms.channel.connect") {
                    val command = JSONObject().apply {
                        put("method", "ms.remote.control")
                        put("params", JSONObject().apply {
                            put("Cmd", "Click")
                            put("DataOfCmd", key.samsungKey)
                            put("Option", "false")
                            put("TypeOfRemote", "SendRemoteKey")
                        })
                    }
                    webSocket.send(command.toString())
                    webSocket.close(1000, "command sent")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                if (!secure) open(ip, appName, key, secure = true)
            }
        })
    }
}
