package com.securemsg.domain;

import java.time.Instant;
import java.util.UUID;

public record Message(
        UUID id,
        UUID senderId,
        UUID recipientId,
        String encryptedPayload,
        String signature,
        DeliveryStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Message withStatus(DeliveryStatus newStatus) {
        return new Message(id, senderId, recipientId, encryptedPayload, signature, newStatus, createdAt, Instant.now());
    }
}
