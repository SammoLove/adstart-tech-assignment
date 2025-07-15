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

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
public class DomainStatusService {
    private static final Logger log = LoggerFactory.getLogger(DomainStatusService.class);
    private final DomainRepository repository;
    private final int criticalThresholdDays;
    private final int warningThresholdDays;
    private final int noticeThresholdDays;

    public DomainStatusService(
            DomainRepository repository,
            @Value("${cert.threshold.critical}") int criticalThresholdDays,
            @Value("${cert.threshold.warning}") int warningThresholdDays,
            @Value("${cert.threshold.notice}") int noticeThresholdDays) {

        this.repository = repository;
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

    //@Scheduled(cron = "@daily")
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    void dailyCheck() {
        Iterable<Domain> allDomains = repository.findAll();
        allDomains.forEach(this::findOutStatus);
        repository.saveAll(allDomains);
    }

    private void findOutStatus(Domain domain) {
        try (SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket()) {
            socket.connect(new InetSocketAddress(domain.getName(), 443), (int) TimeUnit.SECONDS.toMillis(10));
            socket.startHandshake();

            X509Certificate[] certs = (X509Certificate[]) socket.getSession().getPeerCertificates();
            X509Certificate cert = certs[0];
            Instant expiry = cert.getNotAfter().toInstant();

            domain.setExpirationTime(expiry);
            domain.setExpirationStatus(determineStatus(expiry));
        } catch (SSLPeerUnverifiedException e) {
            log.warn("SSL verification failed for domain: " + domain, e);
            domain.setExpirationTime(null);
            domain.setExpirationStatus(ExpirationStatus.UNKNOWN);
        } catch (Exception e) {
            log.warn("Error retrieving certificate for domain: " + domain, e);
            domain.setExpirationTime(null);
            domain.setExpirationStatus(ExpirationStatus.UNKNOWN);
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