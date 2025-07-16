package com.adstartmedia.domainwatchdog.service;

import com.adstartmedia.domainwatchdog.model.Domain;
import com.adstartmedia.domainwatchdog.model.ExpirationStatus;
import com.adstartmedia.domainwatchdog.repository.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
public class DomainStatusService {
    private static final Logger log = LoggerFactory.getLogger(DomainStatusService.class);
    private final DomainRepository repository;
    private final SSLSocketFactory insecureSocketFactory;
    private final HostnameVerifier insecureHostnameVerifier;
    private final int criticalThresholdDays;
    private final int warningThresholdDays;
    private final int noticeThresholdDays;

    public DomainStatusService(
            DomainRepository repository,
            SSLSocketFactory insecureSocketFactory,
            HostnameVerifier insecureHostnameVerifier,
            @Value("${cert.threshold.critical}") int criticalThresholdDays,
            @Value("${cert.threshold.warning}") int warningThresholdDays,
            @Value("${cert.threshold.notice}") int noticeThresholdDays) {

        this.repository = repository;
        this.insecureSocketFactory = insecureSocketFactory;
        this.insecureHostnameVerifier = insecureHostnameVerifier;
        this.criticalThresholdDays = criticalThresholdDays;
        this.warningThresholdDays = warningThresholdDays;
        this.noticeThresholdDays = noticeThresholdDays;
    }

    @Async
    @Transactional
    public void updateFor(Iterable<String> domainNames) {
        Iterable<Domain> allDomains = repository.findAllById(domainNames);
        allDomains.forEach(this::findOutStatus);
        repository.saveAll(allDomains);
    }

    @Scheduled(cron = "@daily")
    @Transactional
    void dailyCheck() {
        log.info("Daily domain status checking started.");
        Iterable<Domain> allDomains = repository.findAll();
        allDomains.forEach(this::findOutStatus);
        repository.saveAll(allDomains);
        log.info("Daily domain status checking finished.");
    }

    private void findOutStatus(Domain domain) {
        try {
            X509Certificate leafCertificate = fetchLeafCertificate(domain.getName());
            Instant expiry = leafCertificate.getNotAfter().toInstant();
            domain.setExpirationTime(expiry);
            domain.setExpirationStatus(determineStatus(expiry));
        } catch (IOException e) {
            log.error("Error retrieving certificate for domain: " + domain.getName(), e); //or use fluent api, but there's more lines
            domain.setExpirationTime(null);
            domain.setExpirationStatus(ExpirationStatus.UNKNOWN);
        }
    }

    private X509Certificate fetchLeafCertificate(String domainName) throws IOException {
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

    private ExpirationStatus determineStatus(Instant expiry) {
        if (expiry == null) {
            return ExpirationStatus.UNKNOWN;
        }
        Instant now = Instant.now();
        long daysUntilExpiry = ChronoUnit.DAYS.between(now, expiry);
        if (expiry.isBefore(now)) {
            return ExpirationStatus.EXPIRED;
        } else if (daysUntilExpiry <= criticalThresholdDays) {
            return ExpirationStatus.CRITICAL;
        } else if (daysUntilExpiry <= warningThresholdDays) {
            return ExpirationStatus.WARNING;
        } else if (daysUntilExpiry <= noticeThresholdDays) {
            return ExpirationStatus.NOTICE;
        } else {
            return ExpirationStatus.OK;
        }
    }
}