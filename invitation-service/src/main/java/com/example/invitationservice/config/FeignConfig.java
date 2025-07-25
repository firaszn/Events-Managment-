package com.example.invitationservice.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            try {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                    Jwt jwt = (Jwt) authentication.getPrincipal();
                    String token = jwt.getTokenValue();
                    requestTemplate.header("Authorization", "Bearer " + token);
                    log.debug("Token ajouté à la requête Feign: {}", token.substring(0, Math.min(20, token.length())) + "...");
                }
            } catch (Exception e) {
                log.warn("Impossible d'ajouter le token à la requête Feign: {}", e.getMessage());
            }
        };
    }
} 