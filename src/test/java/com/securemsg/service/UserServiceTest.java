package com.securemsg.service;

import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import com.securemsg.domain.UserStatus;
import com.securemsg.security.InMemoryKeyVault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    @Test
    void shouldAuthenticateWithTwoFactors() {
        AuditService audit = new AuditService();
        UserService users = new UserService(audit, new InMemoryKeyVault());
        User user = users.register("alice", "password", Role.USER, "token123");
        users.confirm(user.login());

        assertTrue(users.authenticate("alice", "password", "token123"));
        assertFalse(users.authenticate("alice", "password", "bad"));
    }

    @Test
    void shouldAutoBlockAfterTooManyFailedAttempts() {
        AuditService audit = new AuditService();
        UserService users = new UserService(audit, new InMemoryKeyVault());
        User user = users.register("bob", "password", Role.USER, "token456");
        users.confirm(user.login());

        for (int i = 0; i < 5; i++) {
            users.authenticate("bob", "wrong-password");
        }

        User blocked = users.findByLogin("bob").orElseThrow();
        assertEquals(UserStatus.BLOCKED, blocked.status());
    }

    @Test
    void shouldRecoverAfterCompromise() {
        AuditService audit = new AuditService();
        UserService users = new UserService(audit, new InMemoryKeyVault());
        User user = users.register("charlie", "password", Role.USER, "token001");
        users.confirm(user.login());
        users.block(user.login(), "incident");

        User recovered = users.recoverAfterCompromise(user.login());

        assertEquals(UserStatus.ACTIVE, recovered.status());
        assertTrue(users.authenticate("charlie", "password", recovered.hardwareTokenSecret()));
    }
}

