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

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:gemini-1.5-flash}")
    private String model;

    @Value("${ai.url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String apiUrl;

    public AiClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public String generateResponse(String systemPrompt, String userMessage) {
        try {
            // Формат запроса для Google Gemini REST API
            Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", userMessage)))
                )
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // Собираем URL: https://.../models/gemini-1.5-flash:generateContent
            String fullUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
            fullUrl += model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0)
                           .path("content")
                           .path("parts").get(0)
                           .path("text").asText();
            } else {
                System.err.println("Gemini API Error: " + response.statusCode() + " " + response.body());
                return "Извините, в данный момент я не могу ответить. (Ошибка Gemini API)";
            }
        } catch (Exception e) {
            System.err.println("Failed to call Gemini API: " + e.getMessage());
            return "Ошибка связи с AI: " + e.getMessage();
        }
    }
}
