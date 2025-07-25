package com.example.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;


class EmailServiceTest {
    @Mock
    private JavaMailSender javaMailSender;
    @InjectMocks
    private EmailService emailService;

    public EmailServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSendEmail() {
        emailService.sendEmail("to@example.com", "subject", "body");
        // No exception means pass, but let's verify interaction
        org.mockito.Mockito.verify(javaMailSender).send(org.mockito.Mockito.any(org.springframework.mail.SimpleMailMessage.class));
    }
} 