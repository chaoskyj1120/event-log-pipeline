package com.example.eventlogpipeline.service;

import com.example.eventlogpipeline.entity.*;
import com.example.eventlogpipeline.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogRepository eventLogRepository;

    @Transactional
    public void log(User user, EventType eventType, Product product, DeviceType deviceType) {
        eventLogRepository.save(EventLog.builder()
                .user(user)
                .eventType(eventType)
                .product(product)
                .deviceType(deviceType)
                .build());
    }

    @Transactional
    public void logFailure(User user, EventType eventType, Product product, DeviceType deviceType, String failedReason) {
        eventLogRepository.save(EventLog.builder()
                .user(user)
                .eventType(eventType)
                .product(product)
                .deviceType(deviceType)
                .isSucceeded(false)
                .failedReason(failedReason)
                .build());
    }
}