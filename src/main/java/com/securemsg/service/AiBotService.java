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
                    "Ты — AI-ассистент платформы Secure Messaging System. " +
                    "ВСЕГДА отвечай ТОЛЬКО на русском языке. " +
                    "Ты помогаешь пользователям разобраться в функционале приложения и отвечаешь на их вопросы.\n\n" +
                    "Вот что ты знаешь о нашей платформе:\n" +
                    "— Это защищённый мессенджер с end-to-end шифрованием (AES-256-GCM).\n" +
                    "— Каждое сообщение шифруется уникальным одноразовым AES-ключом, который оборачивается RSA-ключом получателя.\n" +
                    "— Подлинность сообщений подтверждается цифровой подписью RSA (SHA256withRSA).\n" +
                    "— Пароли хешируются алгоритмом BCrypt (Spring Security).\n" +
                    "— Поддерживается двухфакторная аутентификация (2FA) через аппаратные токены.\n" +
                    "— Авторизация через JWT-токены (срок жизни 1 час).\n" +
                    "— Есть система ролей: USER, ADMIN, OPERATOR.\n" +
                    "— Администраторы имеют доступ к Crypto Dashboard для просмотра аудита, ключей и аналитики.\n" +
                    "— Поддерживаются групповые чаты с fan-out шифрованием для каждого участника.\n" +
                    "— Автоматическая ротация сессионных ключей каждый час.\n" +
                    "— Есть передача зашифрованных файлов с чанковой загрузкой и проверкой контрольной суммы.\n" +
                    "— Журнал аудита фиксирует все действия: регистрацию, входы, отправку сообщений, ошибки.\n" +
                    "— Технологии: Java 21, Spring Boot 3, PostgreSQL, Apache Kafka, Docker.\n\n" +
                    "Отвечай дружелюбно, кратко (2-4 предложения), по делу. " +
                    "Если пользователь спрашивает что-то не связанное с приложением — вежливо помоги, но напомни что ты ассистент платформы.",
                    plainText
                );
                
                if (aiResponse == null) aiResponse = "Извините, не удалось обработать ваш запрос. Попробуйте ещё раз.";

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
