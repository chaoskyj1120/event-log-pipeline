package com.example.eventlogpipeline.service;

import com.example.eventlogpipeline.entity.DeviceType;
import com.example.eventlogpipeline.entity.EventType;
import com.example.eventlogpipeline.entity.User;
import com.example.eventlogpipeline.exception.UserNotFoundException;
import com.example.eventlogpipeline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EventLogService eventLogService;

    @Transactional
    public User login(UUID userId, DeviceType deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        eventLogService.log(user, EventType.LOGIN, null, deviceType);
        return user;
    }
}