package com.example.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${jwt.secret-key}")
    private String secretKey;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration corsConfig = new CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of("http://localhost:4200"));
                    corsConfig.setMaxAge(3600L);
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        // Endpoints publics d'authentification
                        .pathMatchers("/auth/register", "/auth/login", "/auth/google").permitAll()
                        .pathMatchers("/auth/keycloak/**").permitAll()
                        .pathMatchers("/auth/forgot-password", "/auth/reset-password", "/auth/verify-email").permitAll()

                        // Endpoints Swagger/Actuator
                        .pathMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .pathMatchers("/v2/api-docs", "/swagger-resources/**", "/configuration/**", "/webjars/**").permitAll()

                        // Endpoints utilisateur
                        .pathMatchers("/api/users/profile").authenticated()
                        .pathMatchers("/api/users/**").hasRole("ADMIN")

                        // Autres services
                        .pathMatchers("/events/**").authenticated()
                        .pathMatchers("/invitations/**").authenticated()

                        // Tout le reste nécessite une authentification
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(grantedAuthoritiesExtractor())
                        )
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        System.out.println("Creating TEST JWT decoder (signature validation disabled)");
        
        // Créer un décodeur qui ignore la validation de signature (comme dans user-service)
        return new ReactiveJwtDecoder() {
            private final ObjectMapper objectMapper = new ObjectMapper();
            
            @Override
            public Mono<Jwt> decode(String token) throws JwtException {
                try {
                    System.out.println("Décodage du token JWT sans validation de signature (MODE TEST)");
                    
                    // Séparer les parties du JWT
                    String[] parts = token.split("\\.");
                    if (parts.length != 3) {
                        return Mono.error(new JwtException("Token JWT invalide - doit avoir 3 parties"));
                    }

                    // Décoder le header
                    String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> headers = objectMapper.readValue(headerJson, Map.class);

                    // Décoder le payload
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

                    // Extraire les timestamps
                    Instant issuedAt = claims.containsKey("iat") ?
                        Instant.ofEpochSecond(((Number) claims.get("iat")).longValue()) : 
                        Instant.now();
                        
                    Instant expiresAt = claims.containsKey("exp") ? 
                        Instant.ofEpochSecond(((Number) claims.get("exp")).longValue()) : 
                        Instant.now().plusSeconds(3600);

                    // Créer le JWT
                    Jwt jwt = new Jwt(token, issuedAt, expiresAt, headers, claims);

                    System.out.println("Token décodé avec succès (MODE TEST):");
                    System.out.println("- Issuer: " + jwt.getIssuer());
                    System.out.println("- Subject: " + jwt.getSubject());
                    System.out.println("- Email: " + jwt.getClaimAsString("email"));
                    System.out.println("- Roles: " + jwt.getClaimAsMap("realm_access"));

                    return Mono.just(jwt);

                } catch (Exception e) {
                    System.err.println("Erreur lors du décodage du token: " + e.getMessage());
                    return Mono.error(new JwtException("Impossible de décoder le token JWT", e));
                }
            }
        };
    }

    private Converter<Jwt, Mono<org.springframework.security.authentication.AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:4200"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}