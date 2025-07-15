package com.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) { super(message, cause); }
}

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender emailSender;

    public void sendEmail(String to, String subject, String text) {
        log.info("Tentative d'envoi d'email à : {} | Sujet : {}", to, subject);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            emailSender.send(message);
            
            log.info("Email envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email à : {} | Sujet : {}", to, subject, e);
            throw new EmailSendException("Erreur lors de l'envoi de l'email", e);
        }
    }
} 