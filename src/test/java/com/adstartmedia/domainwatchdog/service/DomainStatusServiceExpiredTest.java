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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * I personally more prefer Spock Framework for testing. It's more beautiful, less code, more sugar, more readable.
 * But here I'm gonna use standard frameworks from Spring Test, because I'm not sure if you understand Groovy and Spock.
 * And it will bring more dependencies to such simple application.
 */
@SpringBootTest
class DomainStatusServiceExpiredTest {
    @Autowired private DomainRepository repository;
    @MockitoBean private AbstractDomainStatusService service;
    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new AbstractDomainStatusService(
                repository,
                fixedClock,
                0, 0, 0) {
            @Override
            protected X509Certificate fetchLeafCertificate(String domainName) {
                X509Certificate cert = mock(X509Certificate.class);
                when(cert.getNotAfter()).thenReturn(Date.from(Instant.parse("2024-01-01T00:00:00Z")));
                return cert;
            }
        };
    }

    @Test
    void updateFor_InvalidDomain_SetsUnknownStatus() {
        // Given data and stubs
        Domain domain = new Domain("non-existent-domain-123.com");
        repository.save(domain);

        // When we use tested method
        service.updateFor(List.of(domain.getName()));

        // Then
        Optional<Domain> maybeDomain = repository.findById(domain.getName());
        if (maybeDomain.isPresent()) {
            Domain savedDomain = maybeDomain.get();
            assertEquals(savedDomain.getName(), domain.getName());
            assertEquals(ExpirationStatus.EXPIRED, savedDomain.getExpirationStatus());
        } else {
            fail();
        }
    }
}