package com.example.eventservice.service;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.repository.EventRepository;
import com.example.eventservice.model.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    @Test
    void testCreateEvent() {
        EventEntity event = new EventEntity();
        when(eventRepository.save(event)).thenReturn(event);
        assertEquals(event, eventService.createEvent(event));
    }

    @Test
    void testGetEventById_found() {
        EventEntity event = new EventEntity();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        assertTrue(eventService.getEventById(1L).isPresent());
    }

    @Test
    void testDeleteEvent() {
        eventService.deleteEvent(1L);
        verify(eventRepository, times(1)).deleteById(1L);
    }

    @Test
    void testUpdateEvent() {
        EventEntity event = new EventEntity();
        EventRequest req = new EventRequest();
        req.setTitle("t");
        req.setDescription("d");
        req.setEventDate(null);
        req.setLocation("l");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        assertEquals(event, eventService.updateEvent(1L, req));
    }
} 