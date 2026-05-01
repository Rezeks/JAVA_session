package com.securemsg.api;

import com.securemsg.domain.FileTransfer;
import com.securemsg.domain.GroupChat;
import com.securemsg.domain.Message;
import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import com.securemsg.domain.AuditEvent;
import com.securemsg.repository.UserRepository;
import com.securemsg.security.KeyVault;
import com.securemsg.service.AuditService;
import com.securemsg.service.FileTransferService;
import com.securemsg.service.MessagingService;
import com.securemsg.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DemoController {
    private final UserService userService;
    private final MessagingService messagingService;
    private final FileTransferService fileTransferService;
    private final KeyVault keyVault;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public DemoController(UserService userService,
                          MessagingService messagingService,
                          FileTransferService fileTransferService,
                          KeyVault keyVault,
                          AuditService auditService,
                          UserRepository userRepository) {
        this.userService = userService;
        this.messagingService = messagingService;
        this.fileTransferService = fileTransferService;
        this.keyVault = keyVault;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    // ============ USERS ============

    @Operation(summary = "Список пользователей", description = "Возвращает всех зарегистрированных пользователей (без passwordHash)")
    @Tag(name = "Users")
    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", u.id());
            dto.put("login", u.login());
            dto.put("status", u.status());
            dto.put("role", u.role());
            dto.put("createdAt", u.createdAt());
            return dto;
        }).toList();
    }

    @Operation(summary = "Регистрация", description = "Создаёт нового пользователя. Пароль хешируется PBKDF2-SHA256 (100K итераций)")
    @Tag(name = "Users")
    @PostMapping("/users/register")
    public User register(@RequestBody RegisterRequest request) {
        Role role = request.role() == null ? Role.USER : request.role();
        String token = request.hardwareToken() == null || request.hardwareToken().isBlank()
                ? UUID.randomUUID().toString().substring(0, 8)
                : request.hardwareToken();
        return userService.register(request.login(), request.password(), role, token);
    }

    @PostMapping("/users/{login}/confirm")
    public User confirm(@PathVariable String login) {
        return userService.confirm(login);
    }

    @PostMapping("/users/auth")
    public boolean authenticate(@RequestBody AuthRequest request) {
        return userService.authenticate(request.login(), request.password(), request.hardwareToken());
    }

    @PostMapping("/users/{login}/role")
    public User assignRole(@PathVariable String login, @RequestBody AssignRoleRequest request) {
        return userService.assignRole(login, request.role());
    }

    @PostMapping("/users/{login}/password")
    public User changePassword(@PathVariable String login, @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(login, request.newPassword());
    }

    @PostMapping("/users/{login}/block")
    public User block(@PathVariable String login, @RequestBody BlockRequest request) {
        return userService.block(login, request.reason());
    }

    @PostMapping("/users/{login}/token/rotate")
    public User rotateToken(@PathVariable String login) {
        return userService.rotateHardwareToken(login);
    }

    @PostMapping("/users/{login}/recover")
    public User recoverAfterCompromise(@PathVariable String login) {
        User recovered = userService.recoverAfterCompromise(login);
        messagingService.recoverAfterCompromise(recovered.id());
        return recovered;
    }

    @PostMapping("/messages/send")
    public Message sendMessage(@RequestBody SendMessageRequest request) {
        return messagingService.send(request.senderId(), request.recipientId(), request.text());
    }

    @PostMapping("/messages/{messageId}/deliver")
    public void confirmDelivery(@PathVariable UUID messageId) {
        messagingService.confirmDelivery(messageId);
    }

    @PostMapping("/messages/{messageId}/read")
    public void markRead(@PathVariable UUID messageId, @RequestParam UUID readerId) {
        messagingService.markRead(messageId, readerId);
    }

    @PostMapping("/messages/{messageId}/error")
    public void markError(@PathVariable UUID messageId, @RequestBody MarkErrorRequest request) {
        messagingService.markError(messageId, request.reason());
    }

    @GetMapping("/messages/history/{userId}")
    public List<Message> history(@PathVariable UUID userId) {
        return messagingService.syncHistory(userId);
    }

    @GetMapping("/messages/offline/{userId}")
    public List<Message> pullOffline(@PathVariable UUID userId) {
        return messagingService.pullOfflineMessages(userId);
    }

    @PostMapping("/messages/group")
    public GroupChat createGroup(@RequestBody CreateGroupRequest request) {
        return messagingService.createGroup(request.ownerId(), request.name(), Set.copyOf(request.members()));
    }

    @PostMapping("/messages/group/{groupId}/send")
    public List<Message> sendGroup(@PathVariable UUID groupId, @RequestBody SendGroupRequest request) {
        return messagingService.sendGroupMessage(request.senderId(), groupId, request.text());
    }

    @PostMapping("/files/init")
    public FileTransfer initFileUpload(@RequestBody InitFileRequest request) {
        return fileTransferService.initiateUpload(request.senderId(), request.recipientId(), request.fileName(), request.totalSize());
    }

    @PostMapping(value = "/files/{transferId}/chunk", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public FileTransfer uploadChunk(@PathVariable UUID transferId, @RequestParam long offset, @RequestBody byte[] payload) {
        return fileTransferService.uploadChunk(transferId, offset, new ByteArrayInputStream(payload));
    }

    @PostMapping("/files/{transferId}/finalize")
    public FileTransfer finalizeUpload(@PathVariable UUID transferId) {
        return fileTransferService.finalizeUpload(transferId);
    }

    @GetMapping("/files/{transferId}/verify")
    public boolean verifyChecksum(@PathVariable UUID transferId, @RequestParam String checksum) {
        return fileTransferService.verifyChecksum(transferId, checksum);
    }

    @PostMapping("/files/{transferId}/delivered")
    public void confirmFileDelivered(@PathVariable UUID transferId) {
        fileTransferService.confirmDelivered(transferId);
    }

    @GetMapping("/keys/{ownerId}/public")
    public String exportPublicKey(@PathVariable String ownerId) {
        return keyVault.exportPublicKey(ownerId);
    }

    @GetMapping("/keys/{ownerId}/private")
    public String exportPrivateKey(@PathVariable String ownerId) {
        return keyVault.exportPrivateKey(ownerId);
    }

    @PostMapping("/keys/{ownerId}/import")
    public void importKeys(@PathVariable String ownerId, @RequestBody ImportKeyRequest request) {
        keyVault.importSigningKeyPair(ownerId, request.publicKey(), request.privateKey());
    }

    // ============ MESSAGES: DECRYPT & DELETE ============

    @PostMapping("/messages/{messageId}/decrypt")
    public Map<String, String> decryptMessage(@PathVariable UUID messageId, @RequestParam UUID recipientId) {
        Message message = messagingService.syncHistory(recipientId).stream()
                .filter(m -> m.id().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        String plaintext = messagingService.decryptForRecipient(message, recipientId);
        Map<String, String> result = new HashMap<>();
        result.put("messageId", messageId.toString());
        result.put("plaintext", plaintext);
        result.put("verified", "true");
        return result;
    }

    @PostMapping("/messages/{messageId}/delete")
    public void deleteMessage(@PathVariable UUID messageId, @RequestParam UUID requesterId) {
        messagingService.deleteMessage(messageId, requesterId);
    }

    // ============ AUDIT ============

    @GetMapping("/audit")
    public List<AuditEvent> audit() {
        return auditService.allEvents();
    }

    public record RegisterRequest(String login, String password, Role role, String hardwareToken) {
    }

    public record AuthRequest(String login, String password, String hardwareToken) {
    }

    public record AssignRoleRequest(Role role) {
    }

    public record ChangePasswordRequest(String newPassword) {
    }

    public record BlockRequest(String reason) {
    }

    public record SendMessageRequest(UUID senderId, UUID recipientId, String text) {
    }

    public record MarkErrorRequest(String reason) {
    }

    public record CreateGroupRequest(UUID ownerId, String name, List<UUID> members) {
    }

    public record SendGroupRequest(UUID senderId, String text) {
    }

    public record InitFileRequest(UUID senderId, UUID recipientId, String fileName, long totalSize) {
    }

    public record ImportKeyRequest(String publicKey, String privateKey) {
    }
}


