package com.securemsg.api;

import com.securemsg.domain.Role;
import com.securemsg.domain.User;
import com.securemsg.security.JwtService;
import com.securemsg.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер аутентификации: регистрация и логин с выдачей JWT.
 * Все endpoints публичные (не требуют токена).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Регистрация нового пользователя",
            description = "Создаёт пользователя, хеширует пароль (PBKDF2), возвращает JWT токен")
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        Role role = request.role() == null ? Role.USER : request.role();
        String token = request.hardwareToken() == null || request.hardwareToken().isBlank()
                ? UUID.randomUUID().toString().substring(0, 8)
                : request.hardwareToken();
        User user = userService.register(request.login(), request.password(), role, token);
        userService.confirm(user.login()); // Auto-confirm for demo

        String jwt = jwtService.generateToken(user.login(), user.role().name());

        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
                "id", user.id(),
                "login", user.login(),
                "role", user.role(),
                "hardwareToken", user.hardwareTokenSecret()
        ));
        response.put("token", jwt);
        return response;
    }

    @Operation(summary = "Вход (логин)",
            description = "Аутентификация по логину + пароль + 2FA токен. Возвращает JWT")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        boolean authenticated;
        if (request.hardwareToken() != null && !request.hardwareToken().isBlank()) {
            authenticated = userService.authenticate(request.login(), request.password(), request.hardwareToken());
        } else {
            authenticated = userService.authenticate(request.login(), request.password());
        }

        if (!authenticated) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userService.findByLogin(request.login())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String jwt = jwtService.generateToken(user.login(), user.role().name());

        Map<String, Object> response = new HashMap<>();
        response.put("user", Map.of(
                "id", user.id(),
                "login", user.login(),
                "role", user.role()
        ));
        response.put("token", jwt);
        return response;
    }

    public record RegisterRequest(String login, String password, Role role, String hardwareToken) {}
    public record LoginRequest(String login, String password, String hardwareToken) {}
}
