package com.securemsg;

import com.securemsg.service.AuditService;
import com.securemsg.service.MessagingService;
import com.securemsg.service.UserService;
import com.securemsg.security.CryptoService;
import com.securemsg.security.InMemoryKeyVault;

/**
 * Базовый каркас защищённой системы обмена сообщениями.
 *
 * Для production-среды требуется:
 * - вынести хранилища в БД;
 * - использовать настоящий HSM/TPM через KeyVault-адаптер;
 * - добавить mTLS и полноценные API-контроллеры (REST/gRPC/WebSocket);
 * - настроить репликацию и мониторинг.
 */
public class Application {
    public static void main(String[] args) {
        AuditService auditService = new AuditService();
        UserService userService = new UserService(auditService);
        MessagingService messagingService = new MessagingService(new CryptoService(), new InMemoryKeyVault(), auditService);

        System.out.printf("Secure Messaging skeleton started. Users=%d, messages=%d%n",
                userService.findByLogin("system").isPresent() ? 1 : 0,
                messagingService.syncHistory(java.util.UUID.randomUUID()).size());
    }
}
