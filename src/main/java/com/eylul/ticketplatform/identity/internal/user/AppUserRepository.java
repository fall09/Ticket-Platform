package com.eylul.ticketplatform.identity.internal.user;

import com.eylul.ticketplatform.identity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    boolean existsByEmail(String email);
    Optional<AppUser> findByEmail(String email);
    boolean existsByRole(Role role);
}
