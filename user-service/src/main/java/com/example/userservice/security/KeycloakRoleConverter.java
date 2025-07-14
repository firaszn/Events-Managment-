package com.example.userservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakRoleConverter.class);

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        logger.debug("Starting JWT token conversion");
        logger.debug("JWT claims: {}", jwt.getClaims());

        // Check direct role claim first
        String directRole = jwt.getClaimAsString("role");
        logger.debug("Direct role claim: {}", directRole);
        if (directRole != null) {
            addRoleWithBothFormats(authorities, directRole);
        }

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        logger.debug("Realm access: {}", realmAccess);
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            logger.debug("Realm roles found: {}", realmRoles);

            realmRoles.forEach(roleName -> {
                // Ajouter tous les rôles pertinents (ADMIN, USER, etc.)
                if (roleName.equalsIgnoreCase("ADMIN") ||
                    roleName.equalsIgnoreCase("USER") ||
                    roleName.equalsIgnoreCase("MANAGER")) {
                    addRoleWithBothFormats(authorities, roleName);
                }
            });
        }

        // If no roles found, add default USER role
        if (authorities.isEmpty()) {
            logger.debug("No roles found, adding default USER role");
            addRoleWithBothFormats(authorities, "USER");
        }

        logger.debug("Final authorities: {}", authorities);
        return authorities;
    }

    private void addRoleWithBothFormats(Collection<GrantedAuthority> authorities, String role) {
        // Convert role to uppercase
        String upperRole = role.toUpperCase();

        // Add role with ROLE_ prefix
        String roleWithPrefix = "ROLE_" + upperRole;
        logger.debug("Adding role with prefix: {}", roleWithPrefix);
        authorities.add(new SimpleGrantedAuthority(roleWithPrefix));

        // Add role without prefix
        logger.debug("Adding role without prefix: {}", upperRole);
        authorities.add(new SimpleGrantedAuthority(upperRole));
    }
}