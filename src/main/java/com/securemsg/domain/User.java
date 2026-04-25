package com.securemsg.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String login,
        String passwordHash,
        UserStatus status,
        Role role,
        String hardwareTokenSecret,
        int failedAuthAttempts,
        Instant createdAt,
        Instant updatedAt
) {
    public User withStatus(UserStatus newStatus) {
        return new User(id, login, passwordHash, newStatus, role, hardwareTokenSecret, failedAuthAttempts, createdAt, Instant.now());
    }

    public User withPasswordHash(String newHash) {
        return new User(id, login, newHash, status, role, hardwareTokenSecret, failedAuthAttempts, createdAt, Instant.now());
    }

    public User withRole(Role newRole) {
        return new User(id, login, passwordHash, status, newRole, hardwareTokenSecret, failedAuthAttempts, createdAt, Instant.now());
    }

    public User withHardwareTokenSecret(String tokenSecret) {
        return new User(id, login, passwordHash, status, role, tokenSecret, failedAuthAttempts, createdAt, Instant.now());
    }

    public User withFailedAttempts(int attempts) {
        return new User(id, login, passwordHash, status, role, hardwareTokenSecret, attempts, createdAt, Instant.now());
    }
}
