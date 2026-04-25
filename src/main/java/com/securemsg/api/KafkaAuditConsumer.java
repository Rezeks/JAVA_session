package com.securemsg.api;

import com.securemsg.service.AuditService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Локальный Kafka-consumer для демонстрации "реального времени" через события.
 */
@Component
@ConditionalOnProperty(name = "securemsg.kafka.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaAuditConsumer {
    private final AuditService auditService;

    public KafkaAuditConsumer(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(topics = {"message.sent", "message.delivered", "message.error", "group.message.sent", "security.recovered"},
            groupId = "securemsg-demo")
    public void onEvent(String payload) {
        auditService.record("KAFKA_EVENT", "kafka", payload);
    }
}

