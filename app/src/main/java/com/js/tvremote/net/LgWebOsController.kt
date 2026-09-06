package com.js.tvremote.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LgWebOsController(
    private val ip: String,
    private val onReady: () -> Unit,
    private val onClientKey: (String) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private var pointerSocket: WebSocket? = null
    private var ready = false

    fun connect(savedClientKey: String?) {
        val request = Request.Builder().url("ws://$ip:3000").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                val register = JSONObject().apply {
                    put("type", "register")
                    put("id", "register_0")
                    put("payload", buildRegisterPayload(savedClientKey))
                }
                webSocket.send(register.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (json.optString("id")) {
                    "register_0" -> {
                        if (json.optString("type") != "registered") {
                            onError("El TV LG no autorizó la conexión.")
                            return
                        }
                        json.optJSONObject("payload")?.optString("client-key")
                            ?.takeIf { it.isNotEmpty() }?.let(onClientKey)
                        requestPointerSocket(webSocket)
                    }
                    "pointer_socket" -> {
                        val socketPath = json.optJSONObject("payload")?.optString("socketPath")
                        if (socketPath.isNullOrBlank()) {
                            onError("El TV LG no entregó el canal de control.")
                        } else {
                            openPointerSocket(socketPath)
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                onError("No se pudo conectar con el TV LG.")
            }
        })
    }

    private fun requestPointerSocket(webSocket: WebSocket) {
        val request = JSONObject().apply {
            put("type", "request")
            put("id", "pointer_socket")
            put("uri", "ssap://com.webos.service.networkinput/getPointerInputSocket")
            put("payload", JSONObject())
        }
        webSocket.send(request.toString())
    }

    private fun openPointerSocket(socketPath: String) {
        pointerSocket = client.newWebSocket(Request.Builder().url(socketPath).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                ready = true
                onReady()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                onError("No se pudo abrir el control remoto del TV LG.")
            }
        })
    }

    fun sendButton(button: String): Boolean {
        if (!ready) return false
        val message = "type:button\nname:$button\n\n"
        return pointerSocket?.send(message) == true
    }

    fun sendSsAp(uri: String, payload: JSONObject = JSONObject()): Boolean {
        val ws = socket ?: return false
        val message = JSONObject().apply {
            put("type", "request")
            put("id", "cmd_${System.currentTimeMillis()}")
            put("uri", uri)
            put("payload", payload)
        }
        return ws.send(message.toString())
    }

    private fun buildRegisterPayload(clientKey: String?) = JSONObject().apply {
        put("forcePairing", false)
        put("pairingType", "PROMPT")
        if (!clientKey.isNullOrBlank()) put("client-key", clientKey)
        put("manifest", JSONObject().apply {
            put("manifestVersion", 1)
            put("appVersion", "1.1.0")
            put("permissions", listOf(
                "LAUNCH",
                "CONTROL_AUDIO",
                "CONTROL_INPUT_MEDIA_PLAYBACK",
                "CONTROL_INPUT_TV",
                "CONTROL_MOUSE_AND_KEYBOARD"
            ))
        })
    }

    fun close() {
        pointerSocket?.close(1000, "closed")
        socket?.close(1000, "closed")
        pointerSocket = null
        socket = null
        ready = false
    }
}
