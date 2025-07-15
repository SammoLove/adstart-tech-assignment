package com.adstartmedia.domainwatchdog.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
public class Domain {
    @Id
    private String name;
    private Instant expirationTime;
    private ExpirationStatus expirationStatus;
}