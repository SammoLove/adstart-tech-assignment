package com.adstartmedia.domainwatchdog.service;

import com.adstartmedia.domainwatchdog.model.Domain;
import com.adstartmedia.domainwatchdog.model.ExpirationStatus;
import com.adstartmedia.domainwatchdog.repository.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class AbstractDomainStatusService {
    private static final Logger log = LoggerFactory.getLogger(AbstractDomainStatusService.class);
    private final DomainRepository repository;
    private final int criticalThresholdDays;
    private final int warningThresholdDays;
    private final int noticeThresholdDays;
    private final Clock clock;

    public AbstractDomainStatusService(
            DomainRepository repository,
            Clock clock,
            @Value("${cert.threshold.critical}") int criticalThresholdDays,
            @Value("${cert.threshold.warning}") int warningThresholdDays,
            @Value("${cert.threshold.notice}") int noticeThresholdDays) {

        this.repository = repository;
        this.clock = clock;
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

    protected abstract X509Certificate fetchLeafCertificate(String domainName) throws IOException;

    ExpirationStatus determineStatus(Instant expiry) {
        if (expiry == null) {
            return ExpirationStatus.UNKNOWN;
        }
        Instant now = Instant.now(clock);
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