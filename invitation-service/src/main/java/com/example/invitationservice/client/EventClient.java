package com.example.invitationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "api-gateway",
    url = "${api-gateway.url:http://api-gateway:8093}",
    configuration = com.example.invitationservice.config.FeignConfig.class
)
public interface EventClient {
    
    @GetMapping("/events/{eventId}")
    ResponseEntity<EventDetails> getEventById(@PathVariable("eventId") Long eventId);
    
    public static class EventDetails {
        private Long id;
        private String title;
        private String location;
        private String eventDate;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getEventDate() { return eventDate; }
        public void setEventDate(String eventDate) { this.eventDate = eventDate; }
    }
} 