package com.securemsg.service;

import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import com.securemsg.domain.UserStatus;
import com.securemsg.repository.UserRepository;
import com.securemsg.security.KeyVault;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.InvalidKeySpecException;

/**
 * Сервис управления пользователями: регистрация, аутентификация (PBKDF2 + 2FA),
 * ролевая модель, блокировка при брутфорсе, восстановление после компрометации.
 */
public class UserService {
    private static final int MAX_FAILED_AUTH_ATTEMPTS = 5;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int SALT_SIZE = 32;

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final KeyVault keyVault;

    public UserService(AuditService auditService, KeyVault keyVault, UserRepository userRepository) {
        this.auditService = auditService;
        this.keyVault = keyVault;
        this.userRepository = userRepository;
    }

    public User register(String login, String password) {
        return register(login, password, Role.USER, generateHardwareToken());
    }

    public User register(String login, String password, Role role, String hardwareTokenSecret) {
        if (userRepository.existsByLogin(login)) {
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
        userRepository.save(user);
        keyVault.getOrCreateSigningKeyPair(user.id().toString());
        auditService.record("USER_REGISTERED", login, "New user registered");
        return user;
    }

    public User confirm(String login) {
        User existing = requireUser(login);
        existing.withStatus(UserStatus.ACTIVE);
        userRepository.save(existing);
        auditService.record("USER_CONFIRMED", login, "User account confirmed");
        return existing;
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
        user.withFailedAttempts(0);
        userRepository.save(user);
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
        user.withFailedAttempts(0);
        userRepository.save(user);
        auditService.record("AUTH_OK_2FA", login, "2FA passed");
        return true;
    }

    public User assignRole(String login, Role role) {
        User existing = requireUser(login);
        existing.withRole(role);
        userRepository.save(existing);
        auditService.record("ROLE_ASSIGNED", login, "Role set to " + role);
        return existing;
    }

    public User rotateHardwareToken(String login) {
        User existing = requireUser(login);
        existing.withHardwareTokenSecret(generateHardwareToken());
        userRepository.save(existing);
        auditService.record("HARDWARE_TOKEN_ROTATED", login, "Token rotated");
        return existing;
    }

    public User block(String login, String reason) {
        User existing = requireUser(login);
        existing.withStatus(UserStatus.BLOCKED);
        userRepository.save(existing);
        auditService.record("USER_BLOCKED", login, reason);
        return existing;
    }

    public User recoverAfterCompromise(String login) {
        User existing = requireUser(login);
        keyVault.rotateSigningKeyPair(existing.id().toString());
        keyVault.rotateEncryptionKey(existing.id().toString());
        existing.withStatus(UserStatus.ACTIVE);
        existing.withFailedAttempts(0);
        existing.withHardwareTokenSecret(generateHardwareToken());
        userRepository.save(existing);
        auditService.record("USER_RECOVERED", login, "Credentials and keys rotated after compromise");
        return existing;
    }

    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    private void onAuthFailure(User user, String reason) {
        int attempts = user.failedAuthAttempts() + 1;
        user.withFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_AUTH_ATTEMPTS) {
            user.withStatus(UserStatus.BLOCKED);
            auditService.record("USER_AUTO_BLOCKED", user.login(), "Too many failed attempts");
        }
        userRepository.save(user);
        auditService.record("AUTH_FAILED", user.login(), reason + "; attempts=" + attempts);
    }

    private User requireUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
        existing.withPasswordHash(hash(newPassword));
        existing.withFailedAttempts(0);
        userRepository.save(existing);
        auditService.record("PASSWORD_CHANGED", login, "Password changed");
        return existing;
    }
}
