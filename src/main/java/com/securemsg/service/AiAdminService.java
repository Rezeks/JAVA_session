package com.securemsg.service;

import com.securemsg.domain.AuditEvent;
import com.securemsg.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiAdminService {

    private final AuditService auditService;
    private final UserRepository userRepository;
    private final AiClient aiClient;

    public AiAdminService(AuditService auditService, UserRepository userRepository, AiClient aiClient) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
    }

    public String analyzeSystem() {
        long totalUsers = userRepository.count();
        List<AuditEvent> allEvents = auditService.allEvents();
        List<AuditEvent> recentEvents = allEvents.stream()
                .skip(Math.max(0, allEvents.size() - 50)) // последние 50 событий
                .toList();
                
        String eventsText = recentEvents.stream()
                .map(e -> e.timestamp() + " | " + e.action() + " | " + e.actor() + " | " + e.details())
                .collect(Collectors.joining("\n"));

        String systemPrompt = "You are a cybersecurity AI administrator. Analyze the following system metrics and audit logs. " +
                "Provide a brief security summary, identify any suspicious activity (like repeated AUTH_FAILED or USER_BLOCKED), " +
                "and give recommendations. Keep it under 10 sentences and use Markdown format. Respond in Russian.";
                
        String userMessage = "Total users: " + totalUsers + "\nRecent Audit Logs:\n" + eventsText;

        return aiClient.generateResponse(systemPrompt, userMessage);
    }
}
