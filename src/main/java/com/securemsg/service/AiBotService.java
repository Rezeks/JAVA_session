package com.securemsg.service;

import com.securemsg.domain.Message;
import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiBotService {

    public static final String BOT_LOGIN = "ai_bot";
    
    private final UserService userService;
    private final MessagingService messagingService;
    private final AiClient aiClient;
    
    private UUID botId;

    public AiBotService(UserService userService, MessagingService messagingService, AiClient aiClient) {
        this.userService = userService;
        this.messagingService = messagingService;
        this.aiClient = aiClient;
    }

    @PostConstruct
    public void initBot() {
        Optional<User> existing = userService.findByLogin(BOT_LOGIN);
        if (existing.isEmpty()) {
            User bot = userService.register(BOT_LOGIN, "ai_super_secret_password", Role.USER, "ai_token");
            this.botId = bot.id();
            System.out.println("AI Bot created with ID: " + botId);
        } else {
            this.botId = existing.get().id();
            System.out.println("AI Bot found with ID: " + botId);
        }
    }

    @Scheduled(fixedDelayString = "5000")
    public void processOfflineMessages() {
        UUID currentBotId = this.botId;
        if (currentBotId == null) return;

        List<Message> offlineMessages = messagingService.pullOfflineMessages(currentBotId);
        for (Message msg : offlineMessages) {
            try {
                // Дешифровка
                String plainText = messagingService.decryptForRecipient(msg, currentBotId);
                
                // Получение ответа от LLM
                String aiResponse = aiClient.generateResponse(
                    "You are a helpful AI assistant in a secure end-to-end encrypted messaging system. " +
                    "Users will send you their encrypted messages, you receive them decrypted. " +
                    "Answer concisely and politely. Keep answers under 3-4 sentences.",
                    plainText
                );
                
                if (aiResponse == null) aiResponse = "I'm sorry, I couldn't process that.";

                // Отправка зашифрованного ответа
                messagingService.send(currentBotId, Objects.requireNonNull(msg.senderId()), aiResponse);
                
                // Помечаем сообщение как прочитанное
                messagingService.markRead(Objects.requireNonNull(msg.id()), currentBotId);
                
            } catch (Exception e) {
                System.err.println("Error processing message for AI bot: " + e.getMessage());
                // В случае ошибки пометим как error
                messagingService.markError(Objects.requireNonNull(msg.id()), "AI processing error");
            }
        }
    }
    
    public UUID getBotId() {
        return botId;
    }
}
