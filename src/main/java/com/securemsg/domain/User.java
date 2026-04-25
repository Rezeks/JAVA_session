package com.securemsg.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String login,
        String passwordHash,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public User withStatus(UserStatus newStatus) {
        return new User(id, login, passwordHash, newStatus, createdAt, Instant.now());
    }

    public User withPasswordHash(String newHash) {
        return new User(id, login, newHash, status, createdAt, Instant.now());
    }
}
