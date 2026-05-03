package com.securemsg.service;

import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import com.securemsg.domain.UserStatus;
import com.securemsg.repository.AuditEventRepository;
import com.securemsg.repository.UserRepository;
import com.securemsg.security.InMemoryKeyVault;
import org.junit.jupiter.api.Test;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private UserService createUserService() {
        UserRepository userRepo = new InMemoryUserRepository();
        AuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        AuditService audit = new AuditService(auditRepo);
        return new UserService(audit, new InMemoryKeyVault(), userRepo, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
    }

    @Test
    void shouldAuthenticateWithTwoFactors() {
        UserService users = createUserService();
        User user = users.register("alice", "password", Role.USER, "token123");
        users.confirm(Objects.requireNonNull(user.login()));

        assertTrue(users.authenticate("alice", "password", "token123"));
        assertFalse(users.authenticate("alice", "password", "bad"));
    }

    @Test
    void shouldAutoBlockAfterTooManyFailedAttempts() {
        UserService users = createUserService();
        User user = users.register("bob", "password", Role.USER, "token456");
        users.confirm(Objects.requireNonNull(user.login()));

        for (int i = 0; i < 5; i++) {
            users.authenticate("bob", "wrong-password");
        }

        User blocked = users.findByLogin("bob").orElseThrow();
        assertEquals(UserStatus.BLOCKED, blocked.status());
    }

    @Test
    void shouldRecoverAfterCompromise() {
        UserService users = createUserService();
        User user = users.register("charlie", "password", Role.USER, "token001");
        users.confirm(Objects.requireNonNull(user.login()));
        users.block(Objects.requireNonNull(user.login()), "incident");

        User recovered = users.recoverAfterCompromise(Objects.requireNonNull(user.login()));

        assertEquals(UserStatus.ACTIVE, recovered.status());
        assertTrue(users.authenticate("charlie", "password", Objects.requireNonNull(recovered.hardwareTokenSecret())));
    }
}
