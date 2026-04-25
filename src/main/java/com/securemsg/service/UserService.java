package com.securemsg.service;

import com.securemsg.domain.User;
import com.securemsg.domain.UserStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private final Map<String, User> usersByLogin = new ConcurrentHashMap<>();
    private final AuditService auditService;

    public UserService(AuditService auditService) {
        this.auditService = auditService;
    }

    public User register(String login, String password) {
        if (usersByLogin.containsKey(login)) {
            throw new IllegalArgumentException("Login already exists");
        }
        User user = new User(UUID.randomUUID(), login, hash(password), UserStatus.PENDING_CONFIRMATION, Instant.now(), Instant.now());
        usersByLogin.put(login, user);
        auditService.record("USER_REGISTERED", login, "New user registered");
        return user;
    }

    public User confirm(String login) {
        User existing = requireUser(login);
        User updated = existing.withStatus(UserStatus.ACTIVE);
        usersByLogin.put(login, updated);
        auditService.record("USER_CONFIRMED", login, "User account confirmed");
        return updated;
    }

    public boolean authenticate(String login, String password) {
        User user = requireUser(login);
        boolean authenticated = user.status() == UserStatus.ACTIVE && user.passwordHash().equals(hash(password));
        if (!authenticated) {
            auditService.record("AUTH_FAILED", login, "Authentication failed");
        } else {
            auditService.record("AUTH_OK", login, "User authenticated");
        }
        return authenticated;
    }

    public User changePassword(String login, String newPassword) {
        User existing = requireUser(login);
        User updated = existing.withPasswordHash(hash(newPassword));
        usersByLogin.put(login, updated);
        auditService.record("PASSWORD_CHANGED", login, "Password changed");
        return updated;
    }

    public User block(String login, String reason) {
        User existing = requireUser(login);
        User updated = existing.withStatus(UserStatus.BLOCKED);
        usersByLogin.put(login, updated);
        auditService.record("USER_BLOCKED", login, reason);
        return updated;
    }

    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(usersByLogin.get(login));
    }

    private User requireUser(String login) {
        User user = usersByLogin.get(login);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
