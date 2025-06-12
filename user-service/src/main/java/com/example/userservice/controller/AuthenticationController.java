package com.example.userservice.controller;


import com.example.userservice.auth.*;
import com.example.userservice.entity.UserEntity;
import com.example.userservice.service.AuthenticationService;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request){
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> googleSignIn(@RequestBody GoogleSignInRequest request) {
        return ResponseEntity.ok(authenticationService.googleSignIn(request.getIdToken()));
    }




}
