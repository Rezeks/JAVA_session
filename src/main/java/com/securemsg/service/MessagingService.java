package com.securemsg.service;

import com.securemsg.domain.DeliveryStatus;
import com.securemsg.domain.Message;
import com.securemsg.security.CryptoService;
import com.securemsg.security.KeyVault;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessagingService {
    private final Map<UUID, Message> messages = new ConcurrentHashMap<>();
    private final CryptoService cryptoService;
    private final KeyVault keyVault;
    private final AuditService auditService;

    public MessagingService(CryptoService cryptoService, KeyVault keyVault, AuditService auditService) {
        this.cryptoService = cryptoService;
        this.keyVault = keyVault;
        this.auditService = auditService;
    }

    public Message send(UUID senderId, UUID recipientId, String plainText) {
        byte[] channelKey = keyVault.getOrCreateEncryptionKey(recipientId.toString());
        String encrypted = cryptoService.encrypt(plainText, channelKey);

        KeyPair signerKeys = keyVault.getOrCreateSigningKeyPair(senderId.toString());
        String signature = cryptoService.sign(encrypted, signerKeys.getPrivate());

        Message message = new Message(UUID.randomUUID(), senderId, recipientId, encrypted, signature,
                DeliveryStatus.SENT, java.time.Instant.now(), java.time.Instant.now());

        messages.put(message.id(), message);
        auditService.record("MESSAGE_SENT", senderId.toString(), "Message " + message.id() + " to " + recipientId);
        return message;
    }

    public void confirmDelivery(UUID messageId) {
        Message existing = requireMessage(messageId);
        Message delivered = existing.withStatus(DeliveryStatus.DELIVERED);
        messages.put(messageId, delivered);
        auditService.record("MESSAGE_DELIVERED", delivered.recipientId().toString(), "Message " + messageId + " delivered");
    }

    public void deleteMessage(UUID messageId, UUID requesterId) {
        Message existing = requireMessage(messageId);
        if (!existing.senderId().equals(requesterId) && !existing.recipientId().equals(requesterId)) {
            throw new SecurityException("User has no rights to delete this message");
        }
        messages.put(messageId, existing.withStatus(DeliveryStatus.DELETED));
        auditService.record("MESSAGE_DELETED", requesterId.toString(), "Message " + messageId + " deleted");
    }

    public List<Message> syncHistory(UUID userId) {
        List<Message> history = new ArrayList<>();
        for (Message message : messages.values()) {
            if (message.senderId().equals(userId) || message.recipientId().equals(userId)) {
                history.add(message);
            }
        }
        auditService.record("HISTORY_SYNC", userId.toString(), "History sync size=" + history.size());
        return history;
    }

    public String decryptForRecipient(Message message, UUID recipientId) {
        if (!message.recipientId().equals(recipientId)) {
            throw new SecurityException("Message recipient mismatch");
        }
        KeyPair senderKeys = keyVault.getOrCreateSigningKeyPair(message.senderId().toString());
        boolean validSignature = cryptoService.verify(message.encryptedPayload(), message.signature(), senderKeys.getPublic());
        if (!validSignature) {
            throw new SecurityException("Invalid signature");
        }

        byte[] key = keyVault.getOrCreateEncryptionKey(recipientId.toString());
        return cryptoService.decrypt(message.encryptedPayload(), key);
    }

    private Message requireMessage(UUID messageId) {
        Message message = messages.get(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found");
        }
        return message;
    }
}
