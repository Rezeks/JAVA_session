package com.securemsg;

import com.securemsg.repository.AuditEventRepository;
import com.securemsg.repository.FileTransferRepository;
import com.securemsg.repository.GroupChatRepository;
import com.securemsg.repository.MessageRepository;
import com.securemsg.repository.UserRepository;
import com.securemsg.security.InMemoryKeyVault;
import com.securemsg.security.KeyVault;
import com.securemsg.service.AuditService;
import com.securemsg.service.FileTransferService;
import com.securemsg.service.MessagingService;
import com.securemsg.service.UserService;
import com.securemsg.security.CryptoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.EnableKafka;

import java.nio.file.Path;

/**
 * Базовый каркас защищённой системы обмена сообщениями.
 *
 * Для production-среды требуется:
 * - вынести хранилища в БД;
 * - использовать настоящий HSM/TPM через KeyVault-адаптер;
 * - добавить mTLS и полноценные API-контроллеры (REST/gRPC/WebSocket);
 * - настроить репликацию и мониторинг.
 */
@SpringBootApplication
@EnableKafka
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public AuditService auditService(AuditEventRepository auditEventRepository) {
        return new AuditService(auditEventRepository);
    }

    @Bean
    public KeyVault keyVault() {
        return new InMemoryKeyVault();
    }

    @Bean
    public CryptoService cryptoService() {
        return new CryptoService();
    }

    @Bean
    public UserService userService(AuditService auditService, KeyVault keyVault, UserRepository userRepository) {
        return new UserService(auditService, keyVault, userRepository);
    }

    @Bean
    public MessagingService messagingService(CryptoService cryptoService,
                                             KeyVault keyVault,
                                             AuditService auditService,
                                             ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
                                             MessageRepository messageRepository,
                                             GroupChatRepository groupChatRepository) {
        return new MessagingService(cryptoService, keyVault, auditService,
                kafkaTemplateProvider.getIfAvailable(), messageRepository, groupChatRepository);
    }

    @Bean
    public FileTransferService fileTransferService(CryptoService cryptoService,
                                                   KeyVault keyVault,
                                                   MessagingService messagingService,
                                                   AuditService auditService,
                                                   FileTransferRepository fileTransferRepository) {
        return new FileTransferService(cryptoService, keyVault, messagingService, auditService,
                Path.of("storage"), fileTransferRepository);
    }

    @Bean
    public CommandLineRunner starterProbe(UserService userService, MessagingService messagingService) {
        return ignored -> {
        System.out.printf("Secure Messaging skeleton started. Users=%d, messages=%d%n",
                userService.findByLogin("system").isPresent() ? 1 : 0,
                messagingService.syncHistory(java.util.UUID.randomUUID()).size());
        };
    }
}
