package com.eylul.ticketplatform.identity.internal;


import com.eylul.ticketplatform.identity.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Table(schema = "identity")
@Entity
public class AppUser {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lockedUntil;

    @Column(nullable = false)
    private int failedLoginCount;
}
