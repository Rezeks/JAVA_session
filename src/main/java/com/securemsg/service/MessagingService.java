package com.securemsg.service;

import com.securemsg.domain.DeliveryStatus;
import com.securemsg.domain.GroupChat;
import com.securemsg.domain.Message;
import com.securemsg.domain.Role;
import com.securemsg.security.CryptoService;
import com.securemsg.security.KeyVault;
import org.springframework.kafka.core.KafkaTemplate;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessagingService {
    private static final Duration KEY_ROTATION_INTERVAL = Duration.ofHours(1);

    private final Map<UUID, Message> messages = new ConcurrentHashMap<>();
    private final Map<UUID, GroupChat> groups = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> offlineQueueByRecipient = new ConcurrentHashMap<>();
    private final Map<String, Instant> keyRotationByAlias = new ConcurrentHashMap<>();
    private final Map<String, Integer> ratchetStepByAlias = new ConcurrentHashMap<>();
    private final CryptoService cryptoService;
    private final KeyVault keyVault;
    private final AuditService auditService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessagingService(CryptoService cryptoService, KeyVault keyVault, AuditService auditService, KafkaTemplate<String, String> kafkaTemplate) {
        this.cryptoService = cryptoService;
        this.keyVault = keyVault;
        this.auditService = auditService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public MessagingService(CryptoService cryptoService, KeyVault keyVault, AuditService auditService) {
        this(cryptoService, keyVault, auditService, null);
    }

    public Message send(UUID senderId, UUID recipientId, String plainText) {
        String keyAlias = recipientId.toString();
        rotateKeyIfNeeded(keyAlias);
        byte[] oneTimeMessageKey = CryptoService.generateRandomBytes(32);
        String encrypted = cryptoService.encrypt(plainText, oneTimeMessageKey);
        String wrappedMessageKey = cryptoService.wrapKey(
                oneTimeMessageKey,
                keyVault.getOrCreateSigningKeyPair(recipientId.toString()).getPublic());
        int ratchetStep = ratchetStepByAlias.merge(keyAlias, 1, Integer::sum);

        KeyPair signerKeys = keyVault.getOrCreateSigningKeyPair(senderId.toString());
        String signature = cryptoService.sign(encrypted, signerKeys.getPrivate());

        Message message = new Message(UUID.randomUUID(), senderId, recipientId, null, encrypted,
                wrappedMessageKey, ratchetStep, signature, null,
                DeliveryStatus.QUEUED, Instant.now(), Instant.now());

        messages.put(message.id(), message);
        queueOffline(message);
        auditService.record("MESSAGE_SENT", senderId.toString(), "Message " + message.id() + " to " + recipientId);
        publishEvent("message.sent", message.id().toString());
        return message;
    }

    public GroupChat createGroup(UUID ownerId, String name, Set<UUID> members) {
        Set<UUID> allMembers = new HashSet<>(members);
        allMembers.add(ownerId);
        GroupChat group = new GroupChat(UUID.randomUUID(), name, ownerId, Set.copyOf(allMembers), Instant.now(), Instant.now(), Instant.now());
        groups.put(group.id(), group);
        keyVault.getOrCreateEncryptionKey(groupKeyAlias(group.id()));
        auditService.record("GROUP_CREATED", ownerId.toString(), "Group " + group.id() + " name=" + name);
        return group;
    }

    public List<Message> sendGroupMessage(UUID senderId, UUID groupId, String plainText) {
        GroupChat group = requireGroup(groupId);
        if (!group.members().contains(senderId)) {
            throw new SecurityException("Sender is not in group");
        }
        String alias = groupKeyAlias(groupId);
        rotateKeyIfNeeded(alias);
        byte[] oneTimeGroupKey = CryptoService.generateRandomBytes(32);
        String encrypted = cryptoService.encrypt(plainText, oneTimeGroupKey);
        int ratchetStep = ratchetStepByAlias.merge(alias, 1, Integer::sum);
        KeyPair signerKeys = keyVault.getOrCreateSigningKeyPair(senderId.toString());
        String signature = cryptoService.sign(encrypted, signerKeys.getPrivate());

        List<Message> created = new ArrayList<>();
        for (UUID memberId : group.members()) {
            if (memberId.equals(senderId)) {
                continue;
            }
            String wrappedMessageKey = cryptoService.wrapKey(
                    oneTimeGroupKey,
                    keyVault.getOrCreateSigningKeyPair(memberId.toString()).getPublic());
            Message message = new Message(UUID.randomUUID(), senderId, memberId, groupId, encrypted,
                    wrappedMessageKey, ratchetStep, signature, null,
                    DeliveryStatus.QUEUED, Instant.now(), Instant.now());
            messages.put(message.id(), message);
            queueOffline(message);
            created.add(message);
        }
        auditService.record("GROUP_MESSAGE_SENT", senderId.toString(), "Group " + groupId + ", fanout=" + created.size());
        publishEvent("group.message.sent", groupId + ":" + created.size());
        return created;
    }

    public void confirmDelivery(UUID messageId) {
        Message existing = requireMessage(messageId);
        Message delivered = existing.withStatus(DeliveryStatus.DELIVERED);
        messages.put(messageId, delivered);
        auditService.record("MESSAGE_DELIVERED", delivered.recipientId().toString(), "Message " + messageId + " delivered");
        publishEvent("message.delivered", messageId.toString());
    }

    public void markRead(UUID messageId, UUID readerId) {
        Message existing = requireMessage(messageId);
        if (!existing.recipientId().equals(readerId)) {
            throw new SecurityException("Only recipient can mark message as read");
        }
        messages.put(messageId, existing.withStatus(DeliveryStatus.READ));
        auditService.record("MESSAGE_READ", readerId.toString(), "Message " + messageId + " read");
    }

    public void markError(UUID messageId, String reason) {
        Message existing = requireMessage(messageId);
        messages.put(messageId, existing.withStatus(DeliveryStatus.ERROR));
        auditService.record("MESSAGE_ERROR", existing.recipientId().toString(), "Message " + messageId + " error=" + reason);
        publishEvent("message.error", messageId + ":" + reason);
    }

    public void deleteMessage(UUID messageId, UUID requesterId) {
        deleteMessage(messageId, requesterId, Role.USER);
    }

    public void deleteMessage(UUID messageId, UUID requesterId, Role requesterRole) {
        Message existing = requireMessage(messageId);
        boolean ownsMessage = existing.senderId().equals(requesterId) || existing.recipientId().equals(requesterId);
        boolean privileged = requesterRole == Role.ADMIN || requesterRole == Role.OPERATOR;
        if (!ownsMessage && !privileged) {
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

    public List<Message> pullOfflineMessages(UUID userId) {
        List<UUID> queued = offlineQueueByRecipient.getOrDefault(userId, List.of());
        List<Message> pulled = new ArrayList<>();
        for (UUID messageId : queued) {
            Message msg = messages.get(messageId);
            if (msg != null && msg.status() == DeliveryStatus.QUEUED) {
                messages.put(msg.id(), msg.withStatus(DeliveryStatus.SENT));
                pulled.add(messages.get(msg.id()));
            }
        }
        offlineQueueByRecipient.put(userId, new ArrayList<>());
        auditService.record("OFFLINE_QUEUE_PULL", userId.toString(), "Pulled=" + pulled.size());
        return pulled;
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

        byte[] unwrappedKey = cryptoService.unwrapKey(
                message.wrappedMessageKey(),
                keyVault.getOrCreateSigningKeyPair(recipientId.toString()).getPrivate());
        return cryptoService.decrypt(message.encryptedPayload(), unwrappedKey);
    }

    public void recoverAfterCompromise(UUID userId) {
        String alias = userId.toString();
        keyVault.rotateEncryptionKey(alias);
        keyVault.rotateSigningKeyPair(alias);
        keyRotationByAlias.put(alias, Instant.now());
        ratchetStepByAlias.put(alias, 0);
        auditService.record("POST_COMPROMISE_RECOVERY", alias, "User keys rotated after compromise event");
        publishEvent("security.recovered", alias);
    }

    public Message sendFileNotification(UUID senderId, UUID recipientId, UUID transferId, String encryptedPayload) {
        byte[] oneTimeMessageKey = CryptoService.generateRandomBytes(32);
        String wrappedMessageKey = cryptoService.wrapKey(
                oneTimeMessageKey,
                keyVault.getOrCreateSigningKeyPair(recipientId.toString()).getPublic());
        int ratchetStep = ratchetStepByAlias.merge(recipientId.toString(), 1, Integer::sum);
        KeyPair signerKeys = keyVault.getOrCreateSigningKeyPair(senderId.toString());
        String signature = cryptoService.sign(encryptedPayload, signerKeys.getPrivate());
        Message message = new Message(UUID.randomUUID(), senderId, recipientId, null, encryptedPayload,
                wrappedMessageKey, ratchetStep, signature, transferId,
                DeliveryStatus.QUEUED, Instant.now(), Instant.now());
        messages.put(message.id(), message);
        queueOffline(message);
        auditService.record("FILE_NOTIFICATION_SENT", senderId.toString(), "Transfer " + transferId + " -> " + recipientId);
        publishEvent("file.notification.sent", transferId.toString());
        return message;
    }

    private GroupChat requireGroup(UUID groupId) {
        GroupChat group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found");
        }
        return group;
    }

    private void queueOffline(Message message) {
        offlineQueueByRecipient.computeIfAbsent(message.recipientId(), ignored -> new ArrayList<>()).add(message.id());
    }

    private String groupKeyAlias(UUID groupId) {
        return "group:" + groupId;
    }

    private void rotateKeyIfNeeded(String alias) {
        Instant rotatedAt = keyRotationByAlias.get(alias);
        if (rotatedAt == null || Duration.between(rotatedAt, Instant.now()).compareTo(KEY_ROTATION_INTERVAL) >= 0) {
            keyVault.rotateEncryptionKey(alias);
            keyRotationByAlias.put(alias, Instant.now());
            auditService.record("SESSION_KEY_ROTATED", alias, "Key rotated for active session");
        }
    }

    private void publishEvent(String topic, String payload) {
        if (kafkaTemplate == null) {
            return;
        }
        kafkaTemplate.send(topic, payload);
    }

    private Message requireMessage(UUID messageId) {
        Message message = messages.get(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found");
        }
        return message;
    }
}
