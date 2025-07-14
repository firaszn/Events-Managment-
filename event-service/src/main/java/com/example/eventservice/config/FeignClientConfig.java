package com.example.eventservice.config;

import feign.RequestInterceptor;
import feign.Logger;
import feign.codec.ErrorDecoder;
import feign.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignClientConfig {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(FeignClientConfig.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            log.debug("Intercepting Feign request to: {}", requestTemplate.path());
            
            // Méthode 1: Obtenir le token à partir du SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                String token = jwtAuthentication.getToken().getTokenValue();
                log.debug("Adding token from SecurityContext for URL: {}", requestTemplate.path());
                requestTemplate.header(AUTHORIZATION_HEADER, "Bearer " + token);
                return;
            }
            
            // Méthode 2: Obtenir le token à partir de la requête HTTP actuelle
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    log.debug("Successfully added Authorization header from current request for URL: {}", requestTemplate.path());
                    requestTemplate.header(AUTHORIZATION_HEADER, authorizationHeader);
                    return;
                }
            }
            
            log.warn("No Authorization token found for URL: {}", requestTemplate.path());
        };
    }
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
    
    public static class CustomErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new ErrorDecoder.Default();
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(CustomErrorDecoder.class);
        
        @Override
        public Exception decode(String methodKey, Response response) {
            log.error("Error in Feign call: {} - Status: {}", methodKey, response.status());
            try {
                // Log the response body if possible
                if (response.body() != null) {
                    String responseBody = new String(response.body().asInputStream().readAllBytes());
                    log.error("Response body: {}", responseBody);
                }
            } catch (Exception e) {
                log.error("Could not read response body", e);
            }
            
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
} 