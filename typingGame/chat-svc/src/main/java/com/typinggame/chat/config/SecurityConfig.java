package com.typinggame.chat.config;

import com.typinggame.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        // IM 握手：ticket（/ws）由 ImWebSocketConfig 校验；JWT（/ws/im）由 ImHandshakeInterceptor 校验
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        // 排行榜公开读（my-rank 仍需登录）
                        .requestMatchers(HttpMethod.GET, "/api/v1/leaderboard").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/leaderboard/period").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/leaderboard/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/leaderboard/details").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/dictionaries/files/**").permitAll()
                        .requestMatchers("/api/v1/internal/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
