package com.adstartmedia.domainwatchdog.service;

import com.adstartmedia.domainwatchdog.model.Domain;
import com.adstartmedia.domainwatchdog.model.ExpirationStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DomainStatusService {

    @Async
    public void updateFor(Iterable<Domain> domains) {
        domains.forEach(this::findOutStatus);
    }

    private void findOutStatus(Domain domain) {
        //TODO
        domain.setExpirationStatus(ExpirationStatus.OK);
        domain.setExpirationTime(Instant.now());
    }
}