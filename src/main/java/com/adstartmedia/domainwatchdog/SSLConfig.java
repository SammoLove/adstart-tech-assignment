package com.adstartmedia.domainwatchdog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class SSLConfig {

    @Bean
    public HostnameVerifier insecureHostnameVerifier() {
        return (_, _) -> true;
    }

    @Bean
    public SSLSocketFactory insecureSocketFactory(SSLContext insecureSSLContext) {
        return insecureSSLContext.getSocketFactory();
    }

    @Bean
    public SSLContext insecureSSLContext() {
        try {
            TrustManager[] trustAll = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] c, String a) {
                            // skip checks
                        }
                        public void checkServerTrusted(X509Certificate[] c, String a) {
                            // skip checks, do not throw CertificateException here
                        }
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init insecure SSLContext", e);
        }
    }
}