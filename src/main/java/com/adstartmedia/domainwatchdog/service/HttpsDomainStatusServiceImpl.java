package com.adstartmedia.domainwatchdog.service;

import com.adstartmedia.domainwatchdog.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

@Service
public class HttpsDomainStatusServiceImpl extends AbstractDomainStatusService {
    private final SSLSocketFactory insecureSocketFactory;
    private final HostnameVerifier insecureHostnameVerifier;

    public HttpsDomainStatusServiceImpl(
            DomainRepository repository,
            SSLSocketFactory insecureSocketFactory,
            HostnameVerifier insecureHostnameVerifier,
            Clock clock,
            @Value("${cert.threshold.critical}") int criticalThresholdDays,
            @Value("${cert.threshold.warning}") int warningThresholdDays,
            @Value("${cert.threshold.notice}") int noticeThresholdDays) {

        super(repository, clock, criticalThresholdDays, warningThresholdDays, noticeThresholdDays);
        this.insecureSocketFactory = insecureSocketFactory;
        this.insecureHostnameVerifier = insecureHostnameVerifier;
    }

    @Override
    protected X509Certificate fetchLeafCertificate(String domainName) throws IOException {
        URI uri = URI.create("https://" + domainName);
        HttpsURLConnection conn = (HttpsURLConnection) uri.toURL().openConnection();
        conn.setSSLSocketFactory(insecureSocketFactory);
        conn.setHostnameVerifier(insecureHostnameVerifier);
        conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
        conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
        try {
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            if (certs == null || certs.length == 0) {
                throw new IOException("No certificates found for domain: " + domainName);
            }
            return (X509Certificate) conn.getServerCertificates()[0];
        } finally {
            conn.disconnect();
        }
    }
}