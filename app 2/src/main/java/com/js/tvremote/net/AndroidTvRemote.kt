package com.js.tvremote.net

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

/** Google/Android TV Remote v2 client. */
class AndroidTvRemote(
    private val context: Context,
    private val device: TvDevice
) {
    companion object {
        private const val PAIR_PORT = 6467
        private const val REMOTE_PORT = 6466
        private const val PREFS = "android_tv_remote_v2"
        private const val KEY_CERT = "client_cert"
        private const val KEY_PRIVATE = "client_private"
        private const val KEY_PAIRED_PREFIX = "paired_"

        private val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
    }

    private val writeLock = Any()
    private val readerExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "JsTvRemote-reader").apply { isDaemon = true }
    }
    private val readerRunning = AtomicBoolean(false)

    @Volatile private var socket: SSLSocket? = null
    @Volatile private var output: OutputStream? = null

    suspend fun pairWithPrompt(requestPin: suspend () -> String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ssl = openTls(PAIR_PORT)
                try {
                    send(ssl.outputStream, outer(10, pbStr(1, "atvremote") + pbStr(2, "Js TV Remote")))
                    require(readMessage(ssl.inputStream) != null) { "El TV no respondió." }

                    send(
                        ssl.outputStream,
                        outer(20, pbBytes(1, pbUInt(1, 3) + pbUInt(2, 6)) + pbUInt(3, 1))
                    )
                    require(readMessage(ssl.inputStream) != null) { "El TV rechazó las opciones." }

                    send(
                        ssl.outputStream,
                        outer(30, pbBytes(1, pbUInt(1, 3) + pbUInt(2, 6)) + pbUInt(2, 1))
                    )
                    require(readMessage(ssl.inputStream) != null) { "El TV no mostró el código." }

                    val code = requestPin().trim().uppercase()
                    require(Regex("^[0-9A-F]{6}$").matches(code)) {
                        "El código debe tener 6 caracteres hexadecimales."
                    }

                    val secret = pairingSecret(ssl, code)
                    send(ssl.outputStream, outer(40, pbBytes(1, secret)))
                    require(readMessage(ssl.inputStream) != null) { "El TV no confirmó el emparejamiento." }

                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_PAIRED_PREFIX + device.ip, true)
                        .apply()
                } finally {
                    try { ssl.close() } catch (_: Exception) { }
                }
            }
        }

    fun isPaired(): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PAIRED_PREFIX + device.ip, false) &&
            !prefs.getString(KEY_CERT, null).isNullOrBlank() &&
            !prefs.getString(KEY_PRIVATE, null).isNullOrBlank()
    }

    fun markNotPaired() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PAIRED_PREFIX + device.ip, false).apply()
    }

    suspend fun sendKey(keyCode: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (socket?.isConnected != true || socket?.isClosed == true || output == null) {
                closeSocketOnly()
                connectInternal().getOrThrow()
            }
            val out = output ?: error("No hay conexión con el TV.")
            send(out, outerRemote(10, pbUInt(1, keyCode) + pbUInt(2, 3)))
        }
    }

    suspend fun reconnect(): Result<Unit> = withContext(Dispatchers.IO) { connectInternal() }

    private fun closeSocketOnly() {
        readerRunning.set(false)
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        output = null
    }

    fun close() {
        readerRunning.set(false)
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        output = null
    }

    private fun connectInternal(): Result<Unit> = runCatching {
        if (socket?.isConnected == true && socket?.isClosed == false) return@runCatching

        val s = openTls(REMOTE_PORT)
        socket = s
        output = s.outputStream

        // RemoteConfigure: code1=622 + device information.
        // The RemoteDeviceInfo field order is: model(1), vendor(2),
        // unknown1(3), unknown2(4), package_name(5), app_version(6).
        val deviceInfo =
            pbStr(1, "Js TV Remote") +
                pbStr(2, "Js") +
                pbUInt(3, 1) +
                pbStr(4, "1") +
                pbStr(5, "com.js.tvremote") +
                pbStr(6, "1.3.0")

        send(
            s.outputStream,
            outerRemote(
                1,
                pbUInt(1, 622) + pbBytes(2, deviceInfo)
            )
        )
        send(s.outputStream, outerRemote(2, pbUInt(1, 622)))
        startReader(s)
    }

    private fun openTls(port: Int): SSLSocket {
        ensureProvider()
        val sslContext = sslContext()
        val raw = Socket()
        raw.connect(InetSocketAddress(device.ip, port), 5000)
        val ssl = sslContext.socketFactory.createSocket(raw, device.ip, port, true) as SSLSocket
        ssl.useClientMode = true
        val supported = ssl.supportedProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }
        if (supported.isNotEmpty()) ssl.enabledProtocols = supported.toTypedArray()
        ssl.startHandshake()
        return ssl
    }

    private fun sslContext(): SSLContext {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val keyPair = loadOrCreateIdentity()
        val certB64 = prefs.getString(KEY_CERT, null)
            ?: error("No se encontró la identidad del control remoto.")
        val cert = certificateFrom(certB64)

        val keyStore = java.security.KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("client", keyPair.private, "changeit".toCharArray(), arrayOf(cert))
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "changeit".toCharArray())
        }
        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, trustAll, SecureRandom())
        }
    }

    private fun loadOrCreateIdentity(): KeyPair {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val privateB64 = prefs.getString(KEY_PRIVATE, null)
        val certB64 = prefs.getString(KEY_CERT, null)

        if (!privateB64.isNullOrBlank() && !certB64.isNullOrBlank()) {
            try {
                val cert = certificateFrom(certB64)
                val privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                    PKCS8EncodedKeySpec(Base64.decode(privateB64, Base64.DEFAULT))
                )
                return KeyPair(cert.publicKey, privateKey)
            } catch (_: Exception) {
                prefs.edit().remove(KEY_CERT).remove(KEY_PRIVATE)
                    .putBoolean(KEY_PAIRED_PREFIX + device.ip, false).apply()
            }
        }

        ensureProvider()
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        val keyPair = generator.generateKeyPair()
        val cert = selfSigned(keyPair)

        prefs.edit()
            .putString(KEY_PRIVATE, Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP))
            .putString(KEY_CERT, Base64.encodeToString(cert.encoded, Base64.NO_WRAP))
            .apply()
        return keyPair
    }

    private fun selfSigned(keyPair: KeyPair): X509Certificate {
        ensureProvider()
        val now = System.currentTimeMillis()
        val builder = JcaX509v3CertificateBuilder(
            X500Name("CN=Js TV Remote"),
            BigInteger.valueOf(now),
            Date(now - 60_000L),
            Date(now + 10L * 365 * 24 * 60 * 60 * 1000),
            X500Name("CN=Js TV Remote"),
            keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(keyPair.private)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer))
    }

    private fun certificateFrom(base64: String): X509Certificate {
        ensureProvider()
        return JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(X509CertificateHolder(Base64.decode(base64, Base64.DEFAULT)))
    }

    private fun pairingSecret(ssl: SSLSocket, pin: String): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val clientCert = certificateFrom(prefs.getString(KEY_CERT, null)!!)
        val serverCert = ssl.session.peerCertificates.firstOrNull() as? X509Certificate
            ?: error("El TV no presentó un certificado válido.")
        val clientKey = clientCert.publicKey as? RSAPublicKey ?: error("Clave del teléfono no compatible.")
        val serverKey = serverCert.publicKey as? RSAPublicKey ?: error("Clave del TV no compatible.")

        val data = ByteArrayOutputStream()
        data.write(unsigned(clientKey.modulus))
        data.write(0)
        data.write(unsigned(clientKey.publicExponent))
        data.write(unsigned(serverKey.modulus))
        data.write(0)
        data.write(unsigned(serverKey.publicExponent))
        data.write(hex(pin.substring(2)))

        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
        require((digest[0].toInt() and 0xFF) == pin.substring(0, 2).toInt(16)) {
            "Código incorrecto. Verifica el código del TV."
        }
        return digest
    }

    private fun startReader(s: SSLSocket) {
        if (!readerRunning.compareAndSet(false, true)) return
        readerExecutor.execute {
            try {
                while (readerRunning.get() && !s.isClosed) {
                    val message = readMessage(s.inputStream) ?: break
                    if (containsField(message, 8)) {
                        output?.let { send(it, outerRemote(9, pbUInt(1, 1))) }
                    }
                }
            } catch (_: Exception) {
                // The next command will reconnect.
            } finally {
                readerRunning.set(false)
                if (socket === s) {
                    socket = null
                    output = null
                }
            }
        }
    }

    private fun containsField(data: ByteArray, wantedField: Int): Boolean {
        var index = 0
        while (index < data.size) {
            val tag = readVarintAt(data, index) ?: return false
            index = tag.next
            val field = tag.value ushr 3
            val wire = tag.value and 7
            if (field == wantedField) return true
            index = when (wire) {
                0 -> readVarintAt(data, index)?.next ?: return false
                1 -> index + 8
                2 -> {
                    val length = readVarintAt(data, index) ?: return false
                    length.next + length.value
                }
                5 -> index + 4
                else -> return false
            }
            if (index > data.size) return false
        }
        return false
    }

    private data class VarintResult(val value: Int, val next: Int)

    private fun readVarintAt(data: ByteArray, start: Int): VarintResult? {
        var index = start
        var shift = 0
        var result = 0
        while (index < data.size && shift < 32) {
            val b = data[index++].toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return VarintResult(result, index)
            shift += 7
        }
        return null
    }

    private fun unsigned(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return if (bytes.isNotEmpty() && bytes[0].toInt() == 0) bytes.copyOfRange(1, bytes.size) else bytes
    }

    private fun hex(value: String): ByteArray =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun ensureProvider() {
        if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
    }

    private fun send(out: OutputStream, payload: ByteArray) {
        require(payload.size <= 1_048_576) { "Mensaje demasiado grande." }
        synchronized(writeLock) {
            out.write(varint(payload.size))
            out.write(payload)
            out.flush()
        }
    }

    private fun readMessage(input: InputStream): ByteArray? {
        val length = readVarint(input) ?: return null
        require(length in 0..1_048_576) { "Mensaje de TV demasiado grande." }
        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(data, offset, length - offset)
            if (count < 0) return null
            offset += count
        }
        return data
    }

    private fun readVarint(input: InputStream): Int? {
        var shift = 0
        var result = 0
        while (shift < 32) {
            val b = input.read()
            if (b < 0) return null
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        return null
    }

    private fun varint(value0: Int): ByteArray {
        var value = value0
        val out = ByteArrayOutputStream()
        do {
            var b = value and 0x7F
            value = value ushr 7
            if (value != 0) b = b or 0x80
            out.write(b)
        } while (value != 0)
        return out.toByteArray()
    }

    private fun pbUInt(field: Int, value: Int) = varint(field shl 3) + varint(value)
    private fun pbBytes(field: Int, value: ByteArray) = varint((field shl 3) or 2) + varint(value.size) + value
    private fun pbStr(field: Int, value: String) = pbBytes(field, value.toByteArray(Charsets.UTF_8))
    private fun outer(field: Int, value: ByteArray) = pbUInt(1, 2) + pbUInt(2, 200) + pbBytes(field, value)
    private fun outerRemote(field: Int, value: ByteArray) = pbBytes(field, value)

    private operator fun ByteArray.plus(other: ByteArray): ByteArray {
        val result = ByteArray(size + other.size)
        System.arraycopy(this, 0, result, 0, size)
        System.arraycopy(other, 0, result, size, other.size)
        return result
    }
}
