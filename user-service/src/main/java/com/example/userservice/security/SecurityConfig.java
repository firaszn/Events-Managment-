package com.example.userservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.security.oauth2.jwt.BadJwtException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${jwt.secret-key}")
    private String secretKey;

    /**
     * Décodeur JWT de débogage qui ne valide PAS la signature.
     * ATTENTION : À N'UTILISER QUE POUR LE DÉBOGAGE.
     */
    private static class UnsafeDebugJwtDecoder implements JwtDecoder {
        @Override
        public Jwt decode(String token) throws JwtException {
            try {
                com.nimbusds.jwt.JWT jwt = JWTParser.parse(token);
                JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
                Map<String, Object> headers = new HashMap<>(jwt.getHeader().toJSONObject());
                Map<String, Object> claims = new HashMap<>(claimsSet.getClaims());

                Instant issuedAt = claimsSet.getIssueTime().toInstant();
                Instant expiresAt = claimsSet.getExpirationTime().toInstant();

                return new Jwt(token, issuedAt, expiresAt, headers, claims);
            } catch (Exception e) {
                throw new BadJwtException("Failed to parse JWT", e);
            }
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("Configuring security filter chain");

        // Utiliser le bean JwtAuthenticationConverter configuré

        http
            .csrf(csrf -> {
                csrf.disable();
                System.out.println("CSRF disabled");
            })
            .sessionManagement(session -> {
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                System.out.println("Session management configured to STATELESS");
            })
            .authorizeHttpRequests(authorize -> {
                System.out.println("Configuring authorization rules");
                authorize
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/auth/register").permitAll()
                    .requestMatchers("/auth/login").permitAll()
                    .requestMatchers("/auth/google").permitAll()
                    .requestMatchers("/auth/keycloak/**").permitAll() // Nouveaux endpoints Keycloak
                    .requestMatchers("/auth/forgot-password").permitAll()
                    .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/api/users/profile").authenticated()
                    .requestMatchers("/api/password/**").permitAll()
                    .requestMatchers("/auth/reset-password").permitAll()
                    .requestMatchers("/auth/verify-email").permitAll()
                    .requestMatchers("/v2/api-docs", "/v3/api-docs", "/v3/api-docs/**", "/swagger-resources", "/swagger-resources/**", "/configuration/ui", "/configuration/security", "/swagger-ui.html", "/webjars/**").permitAll()
                    .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                    .anyRequest().authenticated();
                System.out.println("Authorization rules configured");
            })
            .oauth2ResourceServer(oauth2 -> {
                System.out.println("Configuring OAuth2 resource server");
                oauth2.jwt(jwt -> {
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                    jwt.decoder(jwtDecoder());
                    System.out.println("JWT authentication converter and decoder configured");
                });
            });

        System.out.println("Security filter chain configuration completed");
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        logger.warn("!!! AVERTISSEMENT DE SÉCURITÉ : LA VALIDATION DE LA SIGNATURE JWT EST DÉSACTIVÉE. POUR LE DÉBOGAGE UNIQUEMENT. !!!");
        return new UnsafeDebugJwtDecoder();
    }

    @Bean
    public OAuth2TokenValidator<Jwt> jwtValidator() {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();

        // Validation du timestamp (obligatoire)
        validators.add(new JwtTimestampValidator());

        // Validation de l'issuer (plus permissive pour les tests)
        validators.add(new JwtIssuerValidator("http://localhost:8080/realms/RepasKeycloak"));

        System.out.println("JWT validator configured with issuer validation for Keycloak");
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

}
