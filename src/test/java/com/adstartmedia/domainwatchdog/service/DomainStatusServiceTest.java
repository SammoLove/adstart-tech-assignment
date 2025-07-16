package com.adstartmedia.domainwatchdog.service;

import com.adstartmedia.domainwatchdog.model.Domain;
import com.adstartmedia.domainwatchdog.model.ExpirationStatus;
import com.adstartmedia.domainwatchdog.repository.DomainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class DomainStatusServiceTest {
    @Autowired private DomainRepository repository;
    @MockitoBean private AbstractDomainStatusService service;
    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final int CRITICAL_THRESHOLD_DAYS = 7;
    private static final int WARNING_THRESHOLD_DAYS = 30;
    private static final int NOTICE_THRESHOLD_DAYS = 90;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new AbstractDomainStatusService(
                repository,
                fixedClock,
                CRITICAL_THRESHOLD_DAYS,
                WARNING_THRESHOLD_DAYS,
                NOTICE_THRESHOLD_DAYS) {
            @Override
            protected X509Certificate fetchLeafCertificate(String domainName) {
                X509Certificate cert = mock(X509Certificate.class);
                when(cert.getNotAfter()).thenReturn(Date.from(Instant.parse("2030-01-01T00:00:00Z")));
                return cert;
            }
        };
    }

    @Test
    void updateFor_savesUpdatedValues() {
        //Given init data in DB //again, Spock style :)
        List<Domain> domains = List.of(new Domain("domain1.com"), new Domain("domain2.com"));
        List<String> domainNames = domains.stream().map(Domain::getName).toList();
        repository.saveAll(domains);

        //When we use AbstractDomainStatusService.updateFor method
        service.updateFor(domainNames);

        //Then 1. repository now contains saved domains
        Iterable<Domain> savedDomains = repository.findAllById(domainNames);
        List<String> savedDomainNames = StreamSupport.stream(savedDomains.spliterator(), false)
                .map(Domain::getName)
                .toList();
        assertThat(savedDomainNames).containsExactlyInAnyOrderElementsOf(domainNames);

        //And 2. Check that all saved domains have status and time now
        for (Domain savedDomain : savedDomains) {
            assertThat(savedDomain.getExpirationTime())
                    .withFailMessage("expirationTime is null for domain: %s", savedDomain.getName())
                    .isNotNull();
            assertThat(savedDomain.getExpirationStatus())
                    .withFailMessage("expirationStatus is null for domain: %s", savedDomain.getName())
                    .isNotNull();
        }
    }

    @Test
    void returnsUnknown_ifExpiryIsNull() {
        assertThat(service.determineStatus(null)).isEqualTo(ExpirationStatus.UNKNOWN);
    }

    @Test
    void returnsExpired_ifExpiryBeforeNow() {
        Instant expired = FIXED_NOW.minus(1, ChronoUnit.DAYS);
        assertThat(service.determineStatus(expired)).isEqualTo(ExpirationStatus.EXPIRED);
    }

    @Test
    void returnsCritical_ifWithinCriticalThreshold() {
        Instant expiry = FIXED_NOW.plus(CRITICAL_THRESHOLD_DAYS, ChronoUnit.DAYS);
        assertThat(service.determineStatus(expiry)).isEqualTo(ExpirationStatus.CRITICAL);
    }

    @Test
    void returnsWarning_ifWithinWarningThreshold() {
        Instant expiry = FIXED_NOW.plus(WARNING_THRESHOLD_DAYS, ChronoUnit.DAYS);
        assertThat(service.determineStatus(expiry)).isEqualTo(ExpirationStatus.WARNING);
    }

    @Test
    void returnsNotice_ifWithinNoticeThreshold() {
        Instant expiry = FIXED_NOW.plus(NOTICE_THRESHOLD_DAYS, ChronoUnit.DAYS);
        assertThat(service.determineStatus(expiry)).isEqualTo(ExpirationStatus.NOTICE);
    }

    @Test
    void returnsOk_ifBeyondNoticeThreshold() {
        Instant expiry = FIXED_NOW.plus(NOTICE_THRESHOLD_DAYS + 1, ChronoUnit.DAYS);
        assertThat(service.determineStatus(expiry)).isEqualTo(ExpirationStatus.OK);
    }
}