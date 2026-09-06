package com.js.tvremote.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object RokuController {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    enum class Key(val ecpName: String) {
        HOME("Home"), BACK("Back"), UP("Up"), DOWN("Down"),
        LEFT("Left"), RIGHT("Right"), SELECT("Select"),
        PLAY("Play"), REWIND("Rev"), FORWARD("Fwd"),
        VOLUME_UP("VolumeUp"), VOLUME_DOWN("VolumeDown"), MUTE("VolumeMute"),
        POWER("Power")
    }

    suspend fun send(ip: String, key: Key): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:8060/keypress/${key.ecpName}"
            val request = Request.Builder().url(url).post(okhttp3.RequestBody.create(null, ByteArray(0))).build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
