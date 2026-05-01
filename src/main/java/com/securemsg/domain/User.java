package com.securemsg.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "hardware_token_secret")
    private String hardwareTokenSecret;

    @Column(name = "failed_auth_attempts")
    private int failedAuthAttempts;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected User() {
    }

    public User(UUID id, String login, String passwordHash, UserStatus status, Role role,
                String hardwareTokenSecret, int failedAuthAttempts, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.status = status;
        this.role = role;
        this.hardwareTokenSecret = hardwareTokenSecret;
        this.failedAuthAttempts = failedAuthAttempts;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters ---
    public UUID id() { return id; }
    public String login() { return login; }
    public String passwordHash() { return passwordHash; }
    public UserStatus status() { return status; }
    public Role role() { return role; }
    public String hardwareTokenSecret() { return hardwareTokenSecret; }
    public int failedAuthAttempts() { return failedAuthAttempts; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    // --- JavaBean getters for Jackson serialization ---
    public UUID getId() { return id; }
    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public Role getRole() { return role; }
    public String getHardwareTokenSecret() { return hardwareTokenSecret; }
    public int getFailedAuthAttempts() { return failedAuthAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- Setters ---
    public void setStatus(UserStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; this.updatedAt = Instant.now(); }
    public void setRole(Role role) { this.role = role; this.updatedAt = Instant.now(); }
    public void setHardwareTokenSecret(String secret) { this.hardwareTokenSecret = secret; this.updatedAt = Instant.now(); }
    public void setFailedAuthAttempts(int attempts) { this.failedAuthAttempts = attempts; this.updatedAt = Instant.now(); }

    // --- Legacy with* methods (mutate in place, return this) ---
    public User withStatus(UserStatus newStatus) { setStatus(newStatus); return this; }
    public User withPasswordHash(String newHash) { setPasswordHash(newHash); return this; }
    public User withRole(Role newRole) { setRole(newRole); return this; }
    public User withHardwareTokenSecret(String tokenSecret) { setHardwareTokenSecret(tokenSecret); return this; }
    public User withFailedAttempts(int attempts) { setFailedAuthAttempts(attempts); return this; }
}
