package com.securemsg.config;

import com.securemsg.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация Spring Security:
 * - Stateless (JWT, без сессий)
 * - Публичные: /api/auth/**, /ui/**, /swagger-ui/**, /actuator/**, /api-docs/**
 * - ADMIN: управление пользователями, ключами
 * - Остальное: аутентифицированные пользователи
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ui/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/encryption/**").permitAll()

                        // Admin-only endpoints
                        .requestMatchers(HttpMethod.POST, "/api/users/*/block").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/keys/*/private").hasRole("ADMIN")

                        // All other API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        // Everything else is public
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
