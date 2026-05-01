package com.securemsg.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String action;

    @Column(length = 255)
    private String actor;

    @Column(name = "subject", length = 1024)
    private String details;

    @Column(name = "created_at")
    private Instant timestamp;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, String action, String actor, String details, Instant timestamp) {
        this.id = id;
        this.action = action;
        this.actor = actor;
        this.details = details;
        this.timestamp = timestamp;
    }

    // --- Getters (record-style) ---
    public UUID id() { return id; }
    public String action() { return action; }
    public String actor() { return actor; }
    public String details() { return details; }
    public Instant timestamp() { return timestamp; }

    // --- JavaBean getters ---
    public UUID getId() { return id; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}
