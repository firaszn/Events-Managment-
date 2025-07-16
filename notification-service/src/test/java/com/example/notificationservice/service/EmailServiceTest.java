package com.example.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmailServiceTest {
    @Autowired
    private EmailService emailService;

    @Test
    void contextLoads() {
        assertThat(emailService).isNotNull();
    }
} 