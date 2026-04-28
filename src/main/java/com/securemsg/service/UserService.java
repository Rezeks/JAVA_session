package com.securemsg.service;

import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import com.securemsg.domain.UserStatus;
import com.securemsg.security.InMemoryKeyVault;
import com.securemsg.security.KeyVault;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.InvalidKeySpecException;

public class UserService {
    private static final int MAX_FAILED_AUTH_ATTEMPTS = 5;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int SALT_SIZE = 32;

    private final Map<String, User> usersByLogin = new ConcurrentHashMap<>();
    private final AuditService auditService;
    private final KeyVault keyVault;

    public UserService(AuditService auditService) {
        this(auditService, new InMemoryKeyVault());
    }

    public UserService(AuditService auditService, KeyVault keyVault) {
        this.auditService = auditService;
        this.keyVault = keyVault;
    }

    public User register(String login, String password) {
        return register(login, password, Role.USER, generateHardwareToken());
    }

    public User register(String login, String password, Role role, String hardwareTokenSecret) {
        if (usersByLogin.containsKey(login)) {
            throw new IllegalArgumentException("Login already exists");
        }
        User user = new User(
                UUID.randomUUID(),
                login,
                hash(password),
                UserStatus.PENDING_CONFIRMATION,
                role,
                hardwareTokenSecret,
                0,
                Instant.now(),
                Instant.now());
        usersByLogin.put(login, user);
        keyVault.getOrCreateSigningKeyPair(user.id().toString());
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
        if (user.status() != UserStatus.ACTIVE) {
            auditService.record("AUTH_FAILED", login, "User is not active");
            return false;
        }
        if (!verifyPassword(password, user.passwordHash())) {
            onAuthFailure(user, "Wrong password");
            return false;
        }
        usersByLogin.put(login, user.withFailedAttempts(0));
        auditService.record("AUTH_OK", login, "Password factor validated");
        return true;
    }

    public boolean authenticate(String login, String password, String hardwareTokenCode) {
        if (!authenticate(login, password)) {
            return false;
        }
        User user = requireUser(login);
        boolean secondFactorOk = user.hardwareTokenSecret().equals(hardwareTokenCode);
        if (!secondFactorOk) {
            onAuthFailure(user, "Invalid hardware token");
            return false;
        }
        usersByLogin.put(login, user.withFailedAttempts(0));
        auditService.record("AUTH_OK_2FA", login, "2FA passed");
        return true;
    }

    public User assignRole(String login, Role role) {
        User existing = requireUser(login);
        User updated = existing.withRole(role);
        usersByLogin.put(login, updated);
        auditService.record("ROLE_ASSIGNED", login, "Role set to " + role);
        return updated;
    }

    public User rotateHardwareToken(String login) {
        User existing = requireUser(login);
        User updated = existing.withHardwareTokenSecret(generateHardwareToken());
        usersByLogin.put(login, updated);
        auditService.record("HARDWARE_TOKEN_ROTATED", login, "Token rotated");
        return updated;
    }

    public User block(String login, String reason) {
        User existing = requireUser(login);
        User updated = existing.withStatus(UserStatus.BLOCKED);
        usersByLogin.put(login, updated);
        auditService.record("USER_BLOCKED", login, reason);
        return updated;
    }

    public User recoverAfterCompromise(String login) {
        User existing = requireUser(login);
        keyVault.rotateSigningKeyPair(existing.id().toString());
        keyVault.rotateEncryptionKey(existing.id().toString());
        User recovered = existing
                .withStatus(UserStatus.ACTIVE)
                .withFailedAttempts(0)
                .withHardwareTokenSecret(generateHardwareToken());
        usersByLogin.put(login, recovered);
        auditService.record("USER_RECOVERED", login, "Credentials and keys rotated after compromise");
        return recovered;
    }

    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(usersByLogin.get(login));
    }

    private void onAuthFailure(User user, String reason) {
        int attempts = user.failedAuthAttempts() + 1;
        User updated = user.withFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_AUTH_ATTEMPTS) {
            updated = updated.withStatus(UserStatus.BLOCKED);
            auditService.record("USER_AUTO_BLOCKED", user.login(), "Too many failed attempts");
        }
        usersByLogin.put(user.login(), updated);
        auditService.record("AUTH_FAILED", user.login(), reason + "; attempts=" + attempts);
    }

    private User requireUser(String login) {
        User user = usersByLogin.get(login);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    private String hash(String input) {
        return hashPassword(input);
    }

    /**
     * Password hashing with PBKDF2-SHA256.
     * Returns format: "salt$hash" (both base64-encoded).
     */
    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[SALT_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            SecretKey key = factory.generateSecret(spec);
            byte[] hash = key.getEncoded();

            String encodedSalt = Base64.getEncoder().encodeToString(salt);
            String encodedHash = Base64.getEncoder().encodeToString(hash);
            return encodedSalt + "$" + encodedHash;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 algorithm not available", e);
        }
    }

    /**
     * Verify password against PBKDF2 hash.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid password hash format");
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            SecretKey key = factory.generateSecret(spec);
            byte[] hash = key.getEncoded();

            byte[] storedHashBytes = Base64.getDecoder().decode(parts[1]);
            return Arrays.equals(hash, storedHashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 algorithm not available", e);
        }
    }

    private String generateHardwareToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public User changePassword(String login, String newPassword) {
        User existing = requireUser(login);
        User updated = existing.withPasswordHash(hash(newPassword)).withFailedAttempts(0);
        usersByLogin.put(login, updated);
        auditService.record("PASSWORD_CHANGED", login, "Password changed");
        return updated;
    }
}
