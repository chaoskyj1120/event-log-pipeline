package com.example.eventlogpipeline.controller;

import com.example.eventlogpipeline.entity.DeviceType;
import com.example.eventlogpipeline.entity.User;
import com.example.eventlogpipeline.entity.UserGrade;
import com.example.eventlogpipeline.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.userId(), request.deviceType());
        return ResponseEntity.status(HttpStatus.OK)
                .body(new LoginResponse(user.getUserId(), user.getUserGrade()));
    }

    public record LoginRequest(UUID userId, DeviceType deviceType) {}
    public record LoginResponse(UUID userId, UserGrade userGrade) {}
}