package com.example.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;


import java.time.Instant;

import java.util.Map;
import org.springframework.security.oauth2.jwt.BadJwtException;
import reactor.core.scheduler.Schedulers;

import java.text.ParseException;
import java.util.HashMap;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${jwt.secret-key}")
    private String secretKey;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Décodeur JWT de débogage qui ne valide PAS la signature.
     * ATTENTION : À N'UTILISER QUE POUR LE DÉBOGAGE.
     */
    private static class UnsafeDebugJwtDecoder implements ReactiveJwtDecoder {
        @Override
        public Mono<Jwt> decode(String token) {
            return Mono.fromCallable(() -> {
                try {
                    com.nimbusds.jwt.JWT jwt = com.nimbusds.jwt.JWTParser.parse(token);
                    JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
                    Map<String, Object> headers = new HashMap<>(jwt.getHeader().toJSONObject());
                    Map<String, Object> claims = new HashMap<>(claimsSet.getClaims());

                    Instant issuedAt = claimsSet.getIssueTime().toInstant();
                    Instant expiresAt = claimsSet.getExpirationTime().toInstant();

                    return new Jwt(token, issuedAt, expiresAt, headers, claims);
                } catch (ParseException e) {
                    throw new BadJwtException("Failed to parse JWT", e);
                }
            }).subscribeOn(Schedulers.boundedElastic());
        }
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .cors().and()
            .csrf().disable()
            .authorizeExchange()
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .pathMatchers("/auth/**").permitAll()
            .pathMatchers("/events/**").authenticated()
            .pathMatchers("/api/invitations/**").authenticated()
            .pathMatchers("/api/users/**").authenticated()
            .pathMatchers("/api/password/**").authenticated()
            .pathMatchers("/actuator/**").permitAll()
            .anyExchange().authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()));
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        logger.warn("!!! AVERTISSEMENT DE SÉCURITÉ : LA VALIDATION DE LA SIGNATURE JWT EST DÉSACTIVÉE. POUR LE DÉBOGAGE UNIQUEMENT. !!!");
        return new UnsafeDebugJwtDecoder();
    }

    private Converter<Jwt, Mono<org.springframework.security.authentication.AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}