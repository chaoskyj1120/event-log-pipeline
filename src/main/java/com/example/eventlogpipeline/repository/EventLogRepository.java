package com.example.eventlogpipeline.repository;

import com.example.eventlogpipeline.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventLogRepository extends JpaRepository<EventLog, UUID> {
}