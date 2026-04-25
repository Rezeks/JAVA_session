package com.securemsg.domain;

import java.time.Instant;
import java.util.UUID;

public record Message(
        UUID id,
        UUID senderId,
        UUID recipientId,
        UUID groupId,
        String encryptedPayload,
        String wrappedMessageKey,
        int ratchetStep,
        String signature,
        UUID attachmentId,
        DeliveryStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Message withStatus(DeliveryStatus newStatus) {
        return new Message(id, senderId, recipientId, groupId, encryptedPayload, wrappedMessageKey,
                ratchetStep, signature, attachmentId, newStatus, createdAt, Instant.now());
    }
}
