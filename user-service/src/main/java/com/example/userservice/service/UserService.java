package com.example.userservice.service;

import com.example.userservice.entity.UserEntity;
import com.example.userservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), user.getAuthorities()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public UserEntity saveUser(UserEntity user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public UserEntity findByEmail(String email) {
        logger.info("Recherche de l'utilisateur avec l'email: {}", email);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        logger.info("Utilisateur trouvé: {}", user);
        return user;
    }

    public UserEntity findById(Long id) {
        return userRepository.findById(id)
                .orElse(null); // Retourne null si non trouvé, le controller gère le 404
    }

    public UserEntity createUser(UserEntity user) {
        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already exists: " + user.getEmail());
        }

        // Vérifier si le username existe déjà (si applicable)
        if (user.getUsernameField() != null && userRepository.findByUsername(user.getUsernameField()).isPresent()) {
            throw new IllegalStateException("Username already exists: " + user.getUsernameField());
        }

        // Encoder le mot de passe
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Définir les valeurs par défaut
        if (user.getRole() == null) {
            user.setRole(UserEntity.Role.USER);
        }
        // Le champ enabled est déjà initialisé à true par défaut dans l'entité

        return userRepository.save(user);
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found with id: " + id));

        if (user.getRole() == UserEntity.Role.ADMIN) {
            throw new IllegalStateException("Cannot delete admin users");
        }

        userRepository.delete(user);
    }

    public UserEntity updateUser(Long id, UserEntity updatedUser) {
        UserEntity existingUser = userRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("User with id " + id + " does not exist"));

        // Update fields if they are not null
        if (updatedUser.getFirstName() != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            existingUser.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new IllegalStateException("Email already taken");
            }
            existingUser.setEmail(updatedUser.getEmail());
        } else if (updatedUser.getEmail() != null) {
            existingUser.setEmail(updatedUser.getEmail());
        }
        // ✅ INTÉGRATION : Encoder le password si fourni dans UserUpdateRequest
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found with id: " + userId));

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalStateException("Old password is incorrect");
        }

        // Encoder et sauvegarder le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }





}
