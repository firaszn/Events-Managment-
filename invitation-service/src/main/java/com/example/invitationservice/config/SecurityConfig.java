package com.example.invitationservice.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

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
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> {
                authorize
                    .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/invitations/check/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/invitations").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/invitations/*/confirm").hasRole("ADMIN")
                    .anyRequest().authenticated();
                logger.debug("Security configuration: /invitations/check/** requires ROLE_USER or ROLE_ADMIN");
                logger.debug("Security configuration: POST /invitations requires ROLE_USER or ROLE_ADMIN");
                logger.debug("Security configuration: PATCH /invitations/*/confirm requires ROLE_ADMIN");
            })
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                jwt.decoder(jwtDecoder());
            }));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        logger.warn("!!! AVERTISSEMENT DE SÉCURITÉ : LA VALIDATION DE LA SIGNATURE JWT EST DÉSACTIVÉE. POUR LE DÉBOGAGE UNIQUEMENT. !!!");
        return new UnsafeDebugJwtDecoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}