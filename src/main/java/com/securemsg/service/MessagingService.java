package com.securemsg.service;

import com.securemsg.domain.DeliveryStatus;
import com.securemsg.domain.GroupChat;
import com.securemsg.domain.Message;
import com.securemsg.domain.Role;
import com.securemsg.repository.GroupChatRepository;
import com.securemsg.repository.MessageRepository;
import com.securemsg.security.CryptoService;
import com.securemsg.security.KeyVault;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.lang.NonNull;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessagingService {
    private static final Duration KEY_ROTATION_INTERVAL = Duration.ofHours(1);

    private final MessageRepository messageRepository;
    private final GroupChatRepository groupChatRepository;
    // keyRotation and ratchetStep are runtime state, kept in-memory
    private final Map<String, Instant> keyRotationByAlias = new ConcurrentHashMap<>();
    private final Map<String, Integer> ratchetStepByAlias = new ConcurrentHashMap<>();
    private final CryptoService cryptoService;
    private final KeyVault keyVault;
    private final AuditService auditService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public MessagingService(CryptoService cryptoService, KeyVault keyVault, AuditService auditService,
                            KafkaTemplate<String, String> kafkaTemplate,
                            MessageRepository messageRepository, GroupChatRepository groupChatRepository,
                            SimpMessagingTemplate messagingTemplate) {
        this.cryptoService = cryptoService;
        this.keyVault = keyVault;
        this.auditService = auditService;
        this.kafkaTemplate = kafkaTemplate;
        this.messageRepository = messageRepository;
        this.groupChatRepository = groupChatRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public MessagingService(CryptoService cryptoService, KeyVault keyVault, AuditService auditService,
                            MessageRepository messageRepository, GroupChatRepository groupChatRepository) {
        this(cryptoService, keyVault, auditService, null, messageRepository, groupChatRepository, null);
    }

    public Message send(@NonNull UUID senderId, @NonNull UUID recipientId, @NonNull String plainText) {
        String keyAlias = recipientId.toString();
        rotateKeyIfNeeded(keyAlias);
        byte[] oneTimeMessageKey = CryptoService.generateRandomBytes(32);
        String encrypted = cryptoService.encrypt(plainText, oneTimeMessageKey);
        String wrappedMessageKey = cryptoService.wrapKey(
                oneTimeMessageKey,
                keyVault.getOrCreateSigningKeyPair(recipientId.toString()).getPublic());
        int ratchetStep = ratchetStepByAlias.merge(keyAlias, 1, (v1, v2) -> v1 + v2);

        KeyPair signerKeys = keyVault.getOrCreateSigningKeyPair(senderId.toString());
        String signature = cryptoService.sign(encrypted, signerKeys.getPrivate());

        Message message = new Message(UUID.randomUUID(), senderId, recipientId, null, encrypted,
                wrappedMessageKey, ratchetStep, signature, null,
                DeliveryStatus.QUEUED, Instant.now(), Instant.now());

        messageRepository.save(message);
        auditService.record("MESSAGE_SENT", senderId.toString(), "Message " + message.id() + " to " + recipientId);
        publishEvent("message.sent", Objects.requireNonNull(message.id().toString()));
        notifyUser(recipientId.toString(), "message.new", message);
        return message;
    }

    public GroupChat createGroup(UUID ownerId, String name, Set<UUID> members) {
        Set<UUID> allMembers = new HashSet<>(members);
        allMembers.add(ownerId);
        GroupChat group = new GroupChat(UUID.randomUUID(), name, ownerId, Set.copyOf(allMembers), Instant.now(), Instant.now(), Instant.now());
        groupChatRepository.save(group);
        keyVault.getOrCreateEncryptionKey(groupKeyAlias(group.id()));
        auditService.record("GROUP_CREATED", ownerId.toString(), "Group " + group.id() + " name=" + name);
        return group;
    }

    public List<Message> sendGroupMessage(@NonNull UUID senderId, @NonNull UUID groupId, @NonNull String plainText) {
        GroupChat group = requireGroup(groupId);
        if (!group.members().contains(senderId)) {
            throw new SecurityException("Sender is not in group");
        }
        String alias = groupKeyAlias(groupId);
        rotateKeyIfNeeded(alias);
        byte[] oneTimeGroupKey = CryptoService.generateRandomBytes(32);
        String encrypted = cryptoService.encrypt(plainText, oneTimeGroupKey);
        int ratchetStep = ratchetStepByAlias.merge(alias, 1, (v1, v2) -> v1 + v2);
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
            messageRepository.save(message);
            created.add(message);
            notifyUser(memberId.toString(), "group.message.new", message);
        }
        auditService.record("GROUP_MESSAGE_SENT", senderId.toString(), "Group " + groupId + ", fanout=" + created.size());
        publishEvent("group.message.sent", groupId + ":" + created.size());
        return created;
    }

    public void confirmDelivery(@NonNull UUID messageId) {
        Message existing = requireMessage(messageId);
        existing.withStatus(DeliveryStatus.DELIVERED);
        messageRepository.save(existing);
        auditService.record("MESSAGE_DELIVERED", existing.recipientId().toString(), "Message " + messageId + " delivered");
        publishEvent("message.delivered", Objects.requireNonNull(messageId.toString()));
        notifyUser(existing.senderId().toString(), "message.delivered", existing);
    }

    public void markRead(@NonNull UUID messageId, @NonNull UUID readerId) {
        Message existing = requireMessage(messageId);
        if (!existing.recipientId().equals(readerId)) {
            throw new SecurityException("Only recipient can mark message as read");
        }
        existing.withStatus(DeliveryStatus.READ);
        messageRepository.save(existing);
        auditService.record("MESSAGE_READ", readerId.toString(), "Message " + messageId + " read");
        notifyUser(existing.senderId().toString(), "message.read", existing);
    }

    public void markError(@NonNull UUID messageId, @NonNull String reason) {
        Message existing = requireMessage(messageId);
        existing.withStatus(DeliveryStatus.ERROR);
        messageRepository.save(existing);
        auditService.record("MESSAGE_ERROR", existing.recipientId().toString(), "Message " + messageId + " error=" + reason);
        publishEvent("message.error", messageId + ":" + reason);
        notifyUser(existing.senderId().toString(), "message.error", existing);
    }

    public void deleteMessage(@NonNull UUID messageId, @NonNull UUID requesterId) {
        deleteMessage(messageId, requesterId, Role.USER);
    }

    public void deleteMessage(UUID messageId, UUID requesterId, Role requesterRole) {
        Message existing = requireMessage(Objects.requireNonNull(messageId));
        boolean ownsMessage = existing.senderId().equals(requesterId) || existing.recipientId().equals(requesterId);
        boolean privileged = requesterRole == Role.ADMIN || requesterRole == Role.OPERATOR;
        if (!ownsMessage && !privileged) {
            throw new SecurityException("User has no rights to delete this message");
        }
        existing.withStatus(DeliveryStatus.DELETED);
        messageRepository.save(existing);
        auditService.record("MESSAGE_DELETED", requesterId.toString(), "Message " + messageId + " deleted");
    }

    public List<Message> syncHistory(UUID userId) {
        List<Message> history = messageRepository.findBySenderIdOrRecipientId(userId, userId);
        auditService.record("HISTORY_SYNC", userId.toString(), "History sync size=" + history.size());
        return history;
    }

    public List<Message> pullOfflineMessages(UUID userId) {
        List<Message> queued = messageRepository.findByRecipientIdAndStatus(userId, DeliveryStatus.QUEUED);
        List<Message> pulled = new ArrayList<>();
        for (Message msg : queued) {
            msg.withStatus(DeliveryStatus.SENT);
            messageRepository.save(msg);
            pulled.add(msg);
        }
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
        publishEvent("security.recovered", Objects.requireNonNull(alias));
    }

    public Message sendFileNotification(UUID senderId, UUID recipientId, UUID transferId, String encryptedPayload) {
        byte[] oneTimeMessageKey = CryptoService.generateRandomBytes(32);
        String wrappedMessageKey = cryptoService.wrapKey(
                oneTimeMessageKey,
                keyVault.getOrCreateSigningKeyPair(recipientId.toString()).getPublic());
        int ratchetStep = ratchetStepByAlias.merge(recipientId.toString(), 1, (v1, v2) -> v1 + v2);
        KeyPair signerKeys = keyVault.getOrCreateSigningKeyPair(senderId.toString());
        String signature = cryptoService.sign(encryptedPayload, signerKeys.getPrivate());
        Message message = new Message(UUID.randomUUID(), senderId, recipientId, null, encryptedPayload,
                wrappedMessageKey, ratchetStep, signature, transferId,
                DeliveryStatus.QUEUED, Instant.now(), Instant.now());
        messageRepository.save(message);
        auditService.record("FILE_NOTIFICATION_SENT", senderId.toString(), "Transfer " + transferId + " -> " + recipientId);
        publishEvent("file.notification.sent", Objects.requireNonNull(transferId.toString()));
        return message;
    }

    @NonNull
    private GroupChat requireGroup(@NonNull UUID groupId) {
        return Objects.requireNonNull(groupChatRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found")));
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

    private void publishEvent(@NonNull String topic, @NonNull String payload) {
        if (kafkaTemplate == null) {
            return;
        }
        try {
            kafkaTemplate.send(topic, payload);
        } catch (Exception e) {
            // Kafka недоступна — логируем, но не прерываем бизнес-логику.
            // Сообщение уже сохранено в PostgreSQL.
            System.err.println("[WARN] Kafka publish failed (topic=" + topic + "): " + e.getMessage());
        }
    }

    private void notifyUser(String userId, String eventType, Message message) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/user." + userId, Map.of(
                "type", eventType,
                "message", message
            ));
        }
    }

    @NonNull
    private Message requireMessage(@NonNull UUID messageId) {
        return Objects.requireNonNull(messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found")));
    }
}
