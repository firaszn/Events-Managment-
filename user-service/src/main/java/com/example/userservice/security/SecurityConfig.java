package com.example.userservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("Configuring security filter chain");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

        http
            .cors(cors -> {
                cors.configurationSource(corsConfigurationSource());
                System.out.println("CORS configuration applied");
            })
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
                    .requestMatchers("/auth/welcome").permitAll()
                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers("/auth/hello").permitAll()
                    .requestMatchers("/auth/register").permitAll()
                    .requestMatchers("/auth/login").permitAll()
                    .requestMatchers("/auth/google").permitAll()
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
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter);
                    jwt.decoder(jwtDecoder());
                    System.out.println("JWT authentication converter and decoder configured");
                });
            });

        System.out.println("Security filter chain configuration completed");
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        System.out.println("Creating JWT decoder with secret key");
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
