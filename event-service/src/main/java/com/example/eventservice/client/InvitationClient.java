package com.example.eventservice.client;

import com.example.eventservice.config.FeignClientConfig;
import com.example.eventservice.model.InvitationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
    name = "api-gateway",
    url = "${api-gateway.url:http://api-gateway:8093}",
    configuration = FeignClientConfig.class
)
public interface InvitationClient {
    
    @GetMapping("/invitations/check/{eventId}/{userEmail}")
    ResponseEntity<Boolean> isUserRegisteredForEvent(@PathVariable("eventId") Long eventId, @PathVariable("userEmail") String userEmail);

    @GetMapping("/invitations/check-pending/{eventId}/{userEmail}")
    ResponseEntity<Boolean> hasUserPendingInvitation(@PathVariable("eventId") Long eventId, @PathVariable("userEmail") String userEmail);

    @GetMapping("/invitations")
    ResponseEntity<List<InvitationResponse>> getAllInvitations();

    @org.springframework.web.bind.annotation.PatchMapping("/invitations/cancel/{eventId}/{userEmail}")
    ResponseEntity<Void> cancelUserRegistration(@PathVariable("eventId") Long eventId, @PathVariable("userEmail") String userEmail);
} 