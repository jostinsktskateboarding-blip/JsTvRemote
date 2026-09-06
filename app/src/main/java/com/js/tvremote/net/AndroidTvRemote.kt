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
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.security.Security
import java.util.Date

/** Android TV / Google TV Remote v2 transport. Direct phone -> TV, no server. */
class AndroidTvRemote(private val context: Context, private val device: TvDevice) {
    companion object {
        private const val PAIR_PORT = 6467
        private const val REMOTE_PORT = 6466
        private const val PREFS = "android_tv_remote_v2"
        private const val KEY_CERT = "client_cert"
        private const val KEY_PRIVATE = "client_private"

        private val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
    }

    private var socket: SSLSocket? = null
    private var output: OutputStream? = null

    suspend fun pairWithPrompt(requestPin: suspend () -> String): Result<Unit> = runCatching {
        val ssl = openTls(PAIR_PORT)
        try {
            send(ssl.outputStream, outer(10, nested(
                pbStr(1, "atvremote") + pbStr(2, "Js TV Remote")
            )))
            readMessage(ssl.inputStream)
            send(ssl.outputStream, outer(20, nested(
                pbBytes(1, nested(pbUInt(1, 3) + pbUInt(2, 6))) + pbUInt(3, 1)
            )))
            readMessage(ssl.inputStream)
            send(ssl.outputStream, outer(30, nested(
                pbBytes(1, nested(pbUInt(1, 3) + pbUInt(2, 6))) + pbUInt(2, 1)
            )))
            readMessage(ssl.inputStream)

            val code = requestPin().trim().uppercase()
            require(Regex("^[0-9A-F]{6}$").matches(code)) { "El PIN debe tener 6 caracteres hexadecimales." }
            val secret = pairingSecret(ssl, code)
            send(ssl.outputStream, outer(40, nested(pbBytes(1, secret))))
            val ack = readMessage(ssl.inputStream)
            require(ack != null) { "El TV no confirmó el emparejamiento." }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("paired_${device.ip}", true).apply()
        } finally {
            ssl.close()
        }
    }

    fun isPaired(): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("paired_${device.ip}", false)

    fun connect(): Result<Unit> = runCatching {
        if (socket?.isConnected == true && socket?.isClosed == false) return@runCatching
        val s = openTls(REMOTE_PORT)
        socket = s
        output = s.outputStream
        send(output!!, outerRemote(1, nested(
            pbUInt(1, 622) + pbBytes(2, nested(
                pbStr(1, "Js TV Remote") + pbStr(2, "Js TV Remote") + pbUInt(3, 1) + pbStr(4, "1")
            ))
        )))
        send(output!!, outerRemote(2, nested(pbUInt(1, 622))))
    }

    fun sendKey(keyCode: Int): Result<Unit> = runCatching {
        if (socket?.isConnected != true || socket?.isClosed == true) connect().getOrThrow()
        send(output!!, outerRemote(10, nested(pbUInt(1, keyCode) + pbUInt(2, 3))))
    }

    fun close() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        output = null
    }

    private fun openTls(port: Int): SSLSocket {
        ensureProvider()
        val sslContext = sslContext()
        val raw = Socket()
        raw.connect(InetSocketAddress(device.ip, port), 5000)
        val ssl = sslContext.socketFactory.createSocket(raw, device.ip, port, true) as SSLSocket
        ssl.useClientMode = true
        ssl.startHandshake()
        return ssl
    }

    private fun sslContext(): SSLContext {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val keyPair = loadOrCreateIdentity()
        val cert = certificateFrom(prefs.getString(KEY_CERT, null)!!)
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
        if (privateB64 != null && certB64 != null) {
            val kf = KeyFactory.getInstance("RSA")
            return KeyPair(
                certificateFrom(certB64).publicKey,
                kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateB64, Base64.NO_WRAP)))
            )
        }
        ensureProvider()
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048, SecureRandom())
        val kp = gen.generateKeyPair()
        val cert = selfSigned(kp)
        prefs.edit()
            .putString(KEY_PRIVATE, Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
            .putString(KEY_CERT, Base64.encodeToString(cert.encoded, Base64.NO_WRAP))
            .apply()
        return kp
    }

    private fun persistIdentity() { /* identity is already persisted */ }

    private fun selfSigned(kp: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            X500Name("CN=Js TV Remote"), BigInteger.valueOf(now), Date(now - 60_000),
            Date(now + 10L * 365 * 24 * 60 * 60 * 1000), X500Name("CN=Js TV Remote"), kp.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(kp.private)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer))
    }

    private fun certificateFrom(b64: String): X509Certificate = CertificateFactory.getInstance("X.509")
        .generateCertificate(Base64.decode(b64, Base64.NO_WRAP).inputStream()) as X509Certificate

    private fun pairingSecret(ssl: SSLSocket, pin: String): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val clientCert = certificateFrom(prefs.getString(KEY_CERT, null)!!)
        val serverCert = ssl.session.peerCertificates.first() as X509Certificate
        val c = clientCert.publicKey as RSAPublicKey
        val s = serverCert.publicKey as RSAPublicKey
        val data = ByteArrayOutputStream()
        data.write(unsigned(c.modulus)); data.write(0); data.write(unsigned(c.publicExponent))
        data.write(unsigned(s.modulus)); data.write(0); data.write(unsigned(s.publicExponent))
        data.write(hex(pin.substring(2)))
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
        require(digest[0].toInt() and 0xFF == pin.substring(0, 2).toInt(16)) { "PIN incorrecto." }
        return digest
    }

    private fun unsigned(n: BigInteger): ByteArray {
        val b = n.toByteArray()
        return if (b.isNotEmpty() && b[0].toInt() == 0) b.copyOfRange(1, b.size) else b
    }
    private fun hex(s: String): ByteArray = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun ensureProvider() { if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider()) }

    private fun send(out: OutputStream, payload: ByteArray) { out.write(varint(payload.size)); out.write(payload); out.flush() }
    private fun readMessage(input: InputStream): ByteArray? {
        val len = readVarint(input) ?: return null
        val data = ByteArray(len); var off = 0
        while (off < len) { val n = input.read(data, off, len - off); if (n < 0) return null; off += n }
        return data
    }
    private fun readVarint(input: InputStream): Int? { var shift = 0; var result = 0; while (shift < 32) { val b = input.read(); if (b < 0) return null; result = result or ((b and 0x7F) shl shift); if ((b and 0x80) == 0) return result; shift += 7 }; return null }
    private fun varint(v0: Int): ByteArray { var v = v0; val b = ByteArrayOutputStream(); do { var x = v and 0x7F; v = v ushr 7; if (v != 0) x = x or 0x80; b.write(x) } while (v != 0); return b.toByteArray() }
    private fun pbUInt(field: Int, value: Int) = varint((field shl 3)) + varint(value)
    private fun pbBytes(field: Int, value: ByteArray) = varint((field shl 3) or 2) + varint(value.size) + value
    private fun pbStr(field: Int, value: String) = pbBytes(field, value.toByteArray(Charsets.UTF_8))
    private fun nested(value: ByteArray) = value
    private fun outer(field: Int, value: ByteArray) = pbUInt(1, 2) + pbUInt(2, 200) + pbBytes(field, value)
    private fun outerRemote(field: Int, value: ByteArray) = pbBytes(field, value)

    private operator fun ByteArray.plus(other: ByteArray): ByteArray { val r = ByteArray(size + other.size); System.arraycopy(this,0,r,0,size); System.arraycopy(other,0,r,size,other.size); return r }
}
