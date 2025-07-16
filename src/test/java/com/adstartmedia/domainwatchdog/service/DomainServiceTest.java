package com.adstartmedia.domainwatchdog.service;

import com.adstartmedia.domainwatchdog.model.Domain;
import com.adstartmedia.domainwatchdog.repository.DomainRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest
class DomainServiceTest {
    @Autowired private DomainService service;
    @Autowired private DomainRepository repository;
    @MockitoBean AbstractDomainStatusService domainStatusService;

    @Test
    @Transactional
    void addDomains_successfullySaved() {
        //Given
        List<Domain> domains = List.of(new Domain("domain1.com"), new Domain("domain2.com"));
        List<String> domainNames = domains.stream().map(Domain::getName).toList();

        //When
        service.addDomains(domains);

        //Then repository now contains saved domains
        Iterable<Domain> savedDomains = repository.findAllById(domainNames);
        assertThat(savedDomains).containsExactlyInAnyOrderElementsOf(domains);

        //And also they are all sent for checking
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainStatusService).updateFor(captor.capture());
        List<String> passedNames = captor.getValue();
        assertThat(passedNames).containsExactlyInAnyOrder(passedNames.toArray(String[]::new));
    }

    @Test
    void validDomains_shouldPass() {
        assertDoesNotThrow(() -> service.validate("example1.com"));
        assertDoesNotThrow(() -> service.validate("sub-domain.domain.co.uk"));
        assertDoesNotThrow(() -> service.validate("i.ua"));
    }

    @Test
    void invalidDomains_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> service.validate("http://example.com"));
        assertThrows(IllegalArgumentException.class, () -> service.validate("localhost"));
        assertThrows(IllegalArgumentException.class, () -> service.validate("1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> service.validate("example..com"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(""));
        assertThrows(IllegalArgumentException.class, () -> service.validate("abc"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(null));
    }
}