package com.example.userservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        System.out.println("Starting JWT token conversion");
        System.out.println("JWT claims: " + jwt.getClaims());

        // Check direct role claim first
        String directRole = jwt.getClaimAsString("role");
        System.out.println("Direct role claim: " + directRole);
        if (directRole != null) {
            addRoleWithBothFormats(authorities, directRole);
        }

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        System.out.println("Realm access: " + realmAccess);
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            System.out.println("Realm roles found: " + realmRoles);

            realmRoles.forEach(roleName -> {
                if (roleName.equalsIgnoreCase("ADMIN")) {
                    addRoleWithBothFormats(authorities, roleName);
                }
            });
        }

        // If no roles found, add default USER role
        if (authorities.isEmpty()) {
            System.out.println("No roles found, adding default USER role");
            addRoleWithBothFormats(authorities, "USER");
        }

        System.out.println("Final authorities: " + authorities);
        return authorities;
    }

    private void addRoleWithBothFormats(Collection<GrantedAuthority> authorities, String role) {
        // Convert role to uppercase
        String upperRole = role.toUpperCase();

        // Add role with ROLE_ prefix
        String roleWithPrefix = "ROLE_" + upperRole;
        System.out.println("Adding role with prefix: " + roleWithPrefix);
        authorities.add(new SimpleGrantedAuthority(roleWithPrefix));

        // Add role without prefix
        System.out.println("Adding role without prefix: " + upperRole);
        authorities.add(new SimpleGrantedAuthority(upperRole));
    }
}