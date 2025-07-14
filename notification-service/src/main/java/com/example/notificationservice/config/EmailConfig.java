package com.example.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

class EmailConfigException extends RuntimeException {
    public EmailConfigException(String message, Throwable cause) { super(message, cause); }
}

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EmailConfig {

    private final Environment env;

    @Bean
    public JavaMailSender javaMailSender() {
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            
            // Get properties from environment
            mailSender.setHost(env.getProperty("spring.mail.host"));
            mailSender.setPort(Integer.parseInt(env.getProperty("spring.mail.port", "587")));
            mailSender.setUsername(env.getProperty("spring.mail.username"));
            mailSender.setPassword(env.getProperty("spring.mail.password"));

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "true");

            log.info("📧 Email configuration loaded successfully");
            log.debug("Mail server: {}:{}", mailSender.getHost(), mailSender.getPort());
            return mailSender;
        } catch (Exception e) {
            log.error("❌ Failed to configure email sender: {}", e.getMessage());
            throw new EmailConfigException("Failed to configure email sender", e);
        }
    }
} 