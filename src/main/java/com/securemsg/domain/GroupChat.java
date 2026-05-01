package com.securemsg.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "group_chats")
public class GroupChat {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "member_id")
    private Set<UUID> members = new HashSet<>();

    @Column(name = "key_rotated_at")
    private Instant keyRotatedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected GroupChat() {
    }

    public GroupChat(UUID id, String name, UUID ownerId, Set<UUID> members,
                     Instant keyRotatedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.members = new HashSet<>(members);
        this.keyRotatedAt = keyRotatedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters (record-style) ---
    public UUID id() { return id; }
    public String name() { return name; }
    public UUID ownerId() { return ownerId; }
    public Set<UUID> members() { return Set.copyOf(members); }
    public Instant keyRotatedAt() { return keyRotatedAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    // --- JavaBean getters ---
    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOwnerId() { return ownerId; }
    public Set<UUID> getMembers() { return Set.copyOf(members); }
    public Instant getKeyRotatedAt() { return keyRotatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- Setters ---
    public void setMembers(Set<UUID> newMembers) { this.members = new HashSet<>(newMembers); this.updatedAt = Instant.now(); }
    public void setKeyRotatedAt(Instant rotatedAt) { this.keyRotatedAt = rotatedAt; this.updatedAt = Instant.now(); }

    // --- Legacy with* ---
    public GroupChat withMembers(Set<UUID> newMembers) { setMembers(newMembers); return this; }
    public GroupChat withRotatedKeyAt(Instant rotatedAt) { setKeyRotatedAt(rotatedAt); return this; }
}
