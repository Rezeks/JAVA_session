package com.securemsg.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Secure Messaging System API")
                        .version("1.0.0")
                        .description("""
                                API защищённой системы обмена сообщениями.
                                
                                **Криптография:** AES-256-GCM, RSA-OAEP, PBKDF2, SHA256withRSA
                                
                                **Функционал:** Пользователи, Сообщения, Группы, Файлы, Аудит, Сравнение шифрования
                                """)
                        .contact(new Contact()
                                .name("Secure Messaging Team"))
                        .license(new License()
                                .name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local dev")))
                .tags(List.of(
                        new Tag().name("Users").description("Регистрация, аутентификация (PBKDF2 + 2FA), роли"),
                        new Tag().name("Messages").description("E2E шифрование (AES-256-GCM + RSA-OAEP), подписи, группы"),
                        new Tag().name("Files").description("Потоковая передача с шифрованием и SHA-256 checksum"),
                        new Tag().name("Keys").description("Экспорт/импорт криптографических ключей"),
                        new Tag().name("Audit").description("Журнал всех событий безопасности"),
                        new Tag().name("Encryption Comparison").description("Сравнение 7 методов шифрования")));
    }
}
