package com.example.eventlogpipeline.service;

import com.example.eventlogpipeline.entity.*;
import com.example.eventlogpipeline.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogRepository eventLogRepository;

    @Transactional
    public void log(User user, EventType eventType, Product product, DeviceType deviceType, Instant eventTime) {
        eventLogRepository.save(EventLog.builder()
                .user(user)
                .eventType(eventType)
                .product(product)
                .deviceType(deviceType)
                .eventTime(eventTime)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(User user, EventType eventType, Product product, DeviceType deviceType, String failedReason, Instant eventTime) {
        eventLogRepository.save(EventLog.builder()
                .user(user)
                .eventType(eventType)
                .product(product)
                .deviceType(deviceType)
                .isSucceeded(false)
                .failedReason(failedReason)
                .eventTime(eventTime)
                .build());
    }
}