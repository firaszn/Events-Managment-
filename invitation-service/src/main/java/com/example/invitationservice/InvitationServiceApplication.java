package com.example.invitationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class InvitationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvitationServiceApplication.class, args);
    }
} 