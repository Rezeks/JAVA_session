package com.securemsg.service;

import com.securemsg.domain.AuditEvent;
import com.securemsg.repository.AuditEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(String action, String actor, String details) {
        auditEventRepository.save(new AuditEvent(UUID.randomUUID(), action, actor, details, Instant.now()));
    }

    public List<AuditEvent> allEvents() {
        return auditEventRepository.findAll();
    }
}
