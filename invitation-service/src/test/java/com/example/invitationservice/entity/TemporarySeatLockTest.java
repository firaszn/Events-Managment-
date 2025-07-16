package com.example.invitationservice.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemporarySeatLockTest {
    @Test
    void canInstantiate() {
        TemporarySeatLock seatLock = new TemporarySeatLock();
        assertThat(seatLock).isNotNull();
    }
} 