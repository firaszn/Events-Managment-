package com.example.eventservice.service;

import com.example.eventservice.entity.EventEntity;
import com.example.eventservice.model.EventRequest;
import com.example.eventservice.repository.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public EventEntity createEvent(EventEntity event) {
        return eventRepository.save(event);
    }

    public List<EventEntity> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<EventEntity> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    public EventEntity updateEvent(Long id, EventRequest eventRequest) {
        EventEntity existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));

        existingEvent.setTitle(eventRequest.getTitle());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setEventDate(eventRequest.getEventDate());
        existingEvent.setLocation(eventRequest.getLocation());

        return eventRepository.save(existingEvent);
    }
} 