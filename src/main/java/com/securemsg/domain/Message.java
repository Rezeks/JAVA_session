package com.securemsg.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "encrypted_payload", nullable = false, columnDefinition = "TEXT")
    private String encryptedPayload;

    @Column(name = "wrapped_message_key", nullable = false, columnDefinition = "TEXT")
    private String wrappedMessageKey;

    @Column(name = "ratchet_step")
    private int ratchetStep;

    @Column(nullable = false, length = 2048)
    private String signature;

    @Column(name = "attachment_id")
    private UUID attachmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Message() {
    }

    public Message(UUID id, UUID senderId, UUID recipientId, UUID groupId, String encryptedPayload,
                   String wrappedMessageKey, int ratchetStep, String signature, UUID attachmentId,
                   DeliveryStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.groupId = groupId;
        this.encryptedPayload = encryptedPayload;
        this.wrappedMessageKey = wrappedMessageKey;
        this.ratchetStep = ratchetStep;
        this.signature = signature;
        this.attachmentId = attachmentId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters (record-style) ---
    public UUID id() { return id; }
    public UUID senderId() { return senderId; }
    public UUID recipientId() { return recipientId; }
    public UUID groupId() { return groupId; }
    public String encryptedPayload() { return encryptedPayload; }
    public String wrappedMessageKey() { return wrappedMessageKey; }
    public int ratchetStep() { return ratchetStep; }
    public String signature() { return signature; }
    public UUID attachmentId() { return attachmentId; }
    public DeliveryStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    // --- JavaBean getters ---
    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getRecipientId() { return recipientId; }
    public UUID getGroupId() { return groupId; }
    public String getEncryptedPayload() { return encryptedPayload; }
    public String getWrappedMessageKey() { return wrappedMessageKey; }
    public int getRatchetStep() { return ratchetStep; }
    public String getSignature() { return signature; }
    public UUID getAttachmentId() { return attachmentId; }
    public DeliveryStatus getDeliveryStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- Setters ---
    public void setStatus(DeliveryStatus status) { this.status = status; this.updatedAt = Instant.now(); }

    // --- Legacy with* ---
    public Message withStatus(DeliveryStatus newStatus) { setStatus(newStatus); return this; }
}
