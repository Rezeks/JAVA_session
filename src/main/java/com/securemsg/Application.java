package com.securemsg;

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
    public AuditService auditService() {
        return new AuditService();
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
    public UserService userService(AuditService auditService, KeyVault keyVault) {
        return new UserService(auditService, keyVault);
    }

    @Bean
    public MessagingService messagingService(CryptoService cryptoService,
                                             KeyVault keyVault,
                                             AuditService auditService,
                                             ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
        return new MessagingService(cryptoService, keyVault, auditService, kafkaTemplateProvider.getIfAvailable());
    }

    @Bean
    public FileTransferService fileTransferService(CryptoService cryptoService,
                                                   KeyVault keyVault,
                                                   MessagingService messagingService,
                                                   AuditService auditService) {
        return new FileTransferService(cryptoService, keyVault, messagingService, auditService, Path.of("storage"));
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
