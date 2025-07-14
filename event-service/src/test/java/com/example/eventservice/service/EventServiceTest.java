package com.example.eventservice.service;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceTest {
    @Mock
    private EventRepository eventRepository;
    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllEvents_empty() {
        when(eventRepository.findAll()).thenReturn(Collections.emptyList());
        List<EventEntity> result = eventService.getAllEvents();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
} 