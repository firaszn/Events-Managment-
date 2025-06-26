package com.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEventRegistrationEmail(String to, String eventTitle) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Confirmation d'inscription à l'événement : " + eventTitle);
            
            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #2c3e50;">✨ Confirmation d'inscription</h2>
                    <p style="color: #34495e;">Bonjour,</p>
                    <p style="color: #34495e;">Nous sommes ravis de vous confirmer votre inscription à l'événement :</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <h3 style="color: #2c3e50; margin: 0;">📅 %s</h3>
                    </div>
                    <p style="color: #34495e;">Nous avons hâte de vous y retrouver !</p>
                    <p style="color: #7f8c8d; font-size: 0.9em; margin-top: 30px;">
                        Ceci est un email automatique, merci de ne pas y répondre.
                    </p>
                </div>
                """, eventTitle);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✉️ Email de confirmation envoyé à {} pour l'événement {}", to, eventTitle);
        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
} 