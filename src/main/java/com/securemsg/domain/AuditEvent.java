package com.securemsg.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        String action,
        String actor,
        String details,
        Instant happenedAt
) {
}
