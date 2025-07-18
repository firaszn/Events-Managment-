package com.example.invitationservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeatLockServiceTest {
    @Autowired
    private SeatLockService seatLockService;

    @Test
    void contextLoads() {
        assertThat(seatLockService).isNotNull();
    }
} 