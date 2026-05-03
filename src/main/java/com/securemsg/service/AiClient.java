package com.securemsg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class AiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key:empty}")
    private String apiKey;

    @Value("${ai.model:llama3.2}")
    private String model;

    @Value("${ai.url:http://localhost:11434/v1/chat/completions}")
    private String apiUrl;

    public AiClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public String generateResponse(String systemPrompt, String userMessage) {
        try {
            // Формат запроса OpenAI (поддерживается всеми локальными серверами: Ollama, LM Studio)
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.7
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("choices").get(0).path("message").path("content").asText();
            } else {
                System.err.println("Local AI API Error: " + response.statusCode() + " " + response.body());
                return "Извините, локальный AI сервер недоступен или вернул ошибку.";
            }
        } catch (Exception e) {
            System.err.println("Failed to call Local AI API: " + e.getMessage());
            return "Ошибка связи с локальным AI: " + e.getMessage();
        }
    }
}
