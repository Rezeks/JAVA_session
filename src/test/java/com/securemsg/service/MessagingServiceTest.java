package com.securemsg.service;

import com.securemsg.domain.DeliveryStatus;
import com.securemsg.domain.Message;
import com.securemsg.repository.AuditEventRepository;
import com.securemsg.repository.GroupChatRepository;
import com.securemsg.repository.MessageRepository;
import com.securemsg.security.CryptoService;
import com.securemsg.security.InMemoryKeyVault;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagingServiceTest {

    private MessagingService createMessagingService() {
        AuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        AuditService audit = new AuditService(auditRepo);
        MessageRepository messageRepo = new InMemoryMessageRepository();
        GroupChatRepository groupRepo = new InMemoryGroupChatRepository();
        return new MessagingService(new CryptoService(), new InMemoryKeyVault(), audit, messageRepo, groupRepo);
    }

    @Test
    void shouldSendDecryptAndSyncMessages() {
        MessagingService messagingService = createMessagingService();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        Message message = messagingService.send(alice, bob, "hello bob");
        assertTrue(message.wrappedMessageKey() != null && !message.wrappedMessageKey().isBlank());
        messagingService.confirmDelivery(message.id());

        List<Message> bobHistory = messagingService.syncHistory(bob);
        assertEquals(1, bobHistory.size());
        assertEquals(DeliveryStatus.DELIVERED, bobHistory.getFirst().status());

        String decrypted = messagingService.decryptForRecipient(message, bob);
        assertEquals("hello bob", decrypted);
    }

    @Test
    void shouldSetErrorStatus() {
        MessagingService messagingService = createMessagingService();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        Message message = messagingService.send(alice, bob, "hello");
        messagingService.markError(message.id(), "network");

        Message inHistory = messagingService.syncHistory(bob).getFirst();
        assertEquals(DeliveryStatus.ERROR, inHistory.status());
    }

    @Test
    void shouldForbidDeleteByOtherUser() {
        MessagingService messagingService = createMessagingService();

        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID eve = UUID.randomUUID();

        Message message = messagingService.send(alice, bob, "hello");

        assertThrows(SecurityException.class, () -> messagingService.deleteMessage(message.id(), eve));

        messagingService.deleteMessage(message.id(), bob);
        Message deleted = messagingService.syncHistory(bob).getFirst();
        assertTrue(deleted.status() == DeliveryStatus.DELETED);
    }
}
