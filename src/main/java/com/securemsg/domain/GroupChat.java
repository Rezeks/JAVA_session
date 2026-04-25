package com.securemsg.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record GroupChat(
        UUID id,
        String name,
        UUID ownerId,
        Set<UUID> members,
        Instant keyRotatedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public GroupChat withMembers(Set<UUID> newMembers) {
        return new GroupChat(id, name, ownerId, Set.copyOf(newMembers), keyRotatedAt, createdAt, Instant.now());
    }

    public GroupChat withRotatedKeyAt(Instant rotatedAt) {
        return new GroupChat(id, name, ownerId, members, rotatedAt, createdAt, Instant.now());
    }
}

