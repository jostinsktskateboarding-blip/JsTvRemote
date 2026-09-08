private fun selfSigned(kp: KeyPair): X509Certificate {
    ensureProvider()

    val now = System.currentTimeMillis()

    val builder = JcaX509v3CertificateBuilder(
        X500Name("CN=Js TV Remote"),
        BigInteger.valueOf(now),
        Date(now - 60_000L),
        Date(now + 10L * 365 * 24 * 60 * 60 * 1000),
        X500Name("CN=Js TV Remote"),
        kp.public
    )

    val signer = JcaContentSignerBuilder("SHA256withRSA")
        .setProvider("BC")
        .build(kp.private)

    return JcaX509CertificateConverter()
        .setProvider("BC")
        .getCertificate(builder.build(signer))
}
