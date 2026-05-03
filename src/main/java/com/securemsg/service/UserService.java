package com.securemsg.service;

import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import com.securemsg.domain.UserStatus;
import com.securemsg.repository.UserRepository;
import com.securemsg.security.KeyVault;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Сервис управления пользователями: регистрация, аутентификация (PBKDF2 + 2FA),
 * ролевая модель, блокировка при брутфорсе, восстановление после компрометации.
 */
public class UserService {
    private static final int MAX_FAILED_AUTH_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final KeyVault keyVault;
    private final PasswordEncoder passwordEncoder;

    public UserService(AuditService auditService, KeyVault keyVault, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.auditService = auditService;
        this.keyVault = keyVault;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @NonNull
    public User register(@NonNull String login, @NonNull String password) {
        return register(login, password, Role.USER, generateHardwareToken());
    }

    @NonNull
    public User register(@NonNull String login, @NonNull String password, @NonNull Role role, @NonNull String hardwareTokenSecret) {
        if (userRepository.existsByLogin(login)) {
            throw new IllegalArgumentException("Login already exists");
        }
        User user = new User(
                UUID.randomUUID(),
                login,
                passwordEncoder.encode(password),
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
        userRepository.save(Objects.requireNonNull(existing));
        auditService.record("USER_CONFIRMED", Objects.requireNonNull(login), "User account confirmed");
        return existing;
    }

    public boolean authenticate(String login, String password) {
        User user = requireUser(login);
        if (user.status() != UserStatus.ACTIVE) {
            auditService.record("AUTH_FAILED", Objects.requireNonNull(login), "User is not active");
            return false;
        }
        if (!passwordEncoder.matches(password, user.passwordHash())) {
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
        auditService.record("AUTH_OK_2FA", Objects.requireNonNull(login), "2FA passed");
        return true;
    }

    public User assignRole(String login, Role role) {
        User existing = requireUser(login);
        existing.withRole(role);
        userRepository.save(existing);
        auditService.record("ROLE_ASSIGNED", Objects.requireNonNull(login), "Role set to " + role);
        return existing;
    }

    public User rotateHardwareToken(String login) {
        User existing = requireUser(login);
        existing.withHardwareTokenSecret(generateHardwareToken());
        userRepository.save(existing);
        auditService.record("HARDWARE_TOKEN_ROTATED", Objects.requireNonNull(login), "Token rotated");
        return existing;
    }

    public User block(String login, String reason) {
        User existing = requireUser(login);
        existing.withStatus(UserStatus.BLOCKED);
        userRepository.save(existing);
        auditService.record("USER_BLOCKED", Objects.requireNonNull(login), reason);
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
        auditService.record("USER_RECOVERED", Objects.requireNonNull(login), "Credentials and keys rotated after compromise");
        return existing;
    }

    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    private void onAuthFailure(@NonNull User user, String reason) {
        int attempts = user.failedAuthAttempts() + 1;
        user.withFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_AUTH_ATTEMPTS) {
            user.withStatus(UserStatus.BLOCKED);
            auditService.record("USER_AUTO_BLOCKED", Objects.requireNonNull(user.login()), "Too many failed attempts");
        }
        userRepository.save(user);
        auditService.record("AUTH_FAILED", Objects.requireNonNull(user.login()), reason + "; attempts=" + attempts);
    }

    @NonNull
    private User requireUser(@NonNull String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @NonNull
    private String generateHardwareToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public User changePassword(@NonNull String login, @NonNull String newPassword) {
        User existing = requireUser(login);
        existing.withPasswordHash(passwordEncoder.encode(newPassword));
        existing.withFailedAttempts(0);
        userRepository.save(existing);
        auditService.record("PASSWORD_CHANGED", Objects.requireNonNull(login), "Password changed");
        return existing;
    }
}
