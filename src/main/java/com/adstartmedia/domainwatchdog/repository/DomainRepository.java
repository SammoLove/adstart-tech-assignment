package com.adstartmedia.domainwatchdog.repository;

import com.adstartmedia.domainwatchdog.model.Domain;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends CrudRepository<Domain, String> {}