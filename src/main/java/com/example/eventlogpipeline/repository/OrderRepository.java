package com.example.eventlogpipeline.repository;

import com.example.eventlogpipeline.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}