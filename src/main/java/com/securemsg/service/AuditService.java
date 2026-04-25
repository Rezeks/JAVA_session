package com.securemsg.service;

import com.securemsg.domain.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuditService {
    private final CopyOnWriteArrayList<AuditEvent> events = new CopyOnWriteArrayList<>();

    public void record(String action, String actor, String details) {
        events.add(new AuditEvent(UUID.randomUUID(), action, actor, details, Instant.now()));
    }

    public List<AuditEvent> allEvents() {
        return List.copyOf(events);
    }
}
