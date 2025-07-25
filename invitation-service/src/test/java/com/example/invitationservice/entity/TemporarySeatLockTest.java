package com.example.invitationservice.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;

class TemporarySeatLockTest {
    @Test
    void canInstantiate() {
        TemporarySeatLock seatLock = new TemporarySeatLock();
        assertThat(seatLock).isNotNull();
    }
    @Test
    void isExpired_returnsExpected() {
        TemporarySeatLock lock = TemporarySeatLock.builder().expiryTime(LocalDateTime.now().minusMinutes(1)).build();
        assertThat(lock.isExpired()).isTrue();
        lock = TemporarySeatLock.builder().expiryTime(LocalDateTime.now().plusMinutes(1)).build();
        assertThat(lock.isExpired()).isFalse();
    }
} 