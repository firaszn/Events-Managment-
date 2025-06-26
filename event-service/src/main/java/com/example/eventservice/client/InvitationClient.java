package com.example.eventservice.client;

import com.example.eventservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "api-gateway",
    url = "${api-gateway.url:http://localhost:8093}",
    configuration = FeignClientConfig.class
)
public interface InvitationClient {
    
    @GetMapping("/invitations/check/{eventId}/{userEmail}")
    ResponseEntity<Boolean> isUserRegisteredForEvent(@PathVariable("eventId") Long eventId, @PathVariable("userEmail") String userEmail);
} 