package com.adstartmedia.domainwatchdog.service;

import com.adstartmedia.domainwatchdog.model.Domain;
import com.adstartmedia.domainwatchdog.repository.DomainRepository;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
public class DomainService {
    private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile("(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]");
    private final DomainRepository repository;
    private final DomainStatusService domainStatusService;

    public DomainService(DomainRepository repository, DomainStatusService domainStatusService) {
        this.repository = repository;
        this.domainStatusService = domainStatusService;
    }

    @Transactional
    public void addDomains(Iterable<Domain> domains) throws IllegalArgumentException {
        for (Domain domain : domains) {
            validate(domain.getName());
        }
        repository.saveAll(domains);

        List<String> domainNames = StreamSupport.stream(domains.spliterator(), false)
                .map(Domain::getName)
                .toList();
        domainStatusService.updateFor(domainNames);
    }

    @Transactional(readOnly = true)
    public Iterable<Domain> getAllDomains() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Domain> getByName(String name) throws IllegalArgumentException {
        if (Strings.isNotBlank(name)) {
            return repository.findById(name);
        } else {
            throw new IllegalArgumentException("Domain name must not be null or empty");
        }
    }

    private void validate(String name) throws IllegalArgumentException {
        if (Strings.isBlank(name)) {
            throw new IllegalArgumentException("Domain name is null or empty");
        }
        if (!DOMAIN_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid domain name format for: " + name);
        }
    }
}