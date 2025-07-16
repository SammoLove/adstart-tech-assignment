package com.adstartmedia.domainwatchdog.controller;

import com.adstartmedia.domainwatchdog.model.Domain;
import com.adstartmedia.domainwatchdog.service.DomainService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/domains")
public class DomainController {
    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addDomains(@RequestBody Iterable<Domain> domains) {
        domainService.addDomains(domains);
    }

    @GetMapping
    public Iterable<Domain> getAll() {
        return domainService.getAllDomains();
    }

    @GetMapping("/{name}")
    public Optional<Domain> getByName(@PathVariable("name") String name) {
        return domainService.getByName(name);
    }
}