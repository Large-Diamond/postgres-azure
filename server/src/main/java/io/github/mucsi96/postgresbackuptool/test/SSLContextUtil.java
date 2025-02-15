package io.github.mucsi96.postgresbackuptool.test;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class SSLContextUtil {

    public static SslContext createSSLContext(String pemFilePath) throws Exception {
        // Load the CA certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert = cf.generateCertificate(new FileInputStream(pemFilePath));

        // Create a new KeyStore and add the certificate
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null); // Initialize empty keystore
        keyStore.setCertificateEntry("custom-ca", caCert);

        // Initialize TrustManagerFactory with the new keystore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Create a new SSLContext with the TrustManager
        return SslContextBuilder.forClient().trustManager(tmf).build();
    }
}
