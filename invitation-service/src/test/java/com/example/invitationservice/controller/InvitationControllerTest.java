package com.example.invitationservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InvitationControllerTest {
    @Autowired
    private InvitationController invitationController;

    @Test
    void contextLoads() {
        assertThat(invitationController).isNotNull();
    }
} 