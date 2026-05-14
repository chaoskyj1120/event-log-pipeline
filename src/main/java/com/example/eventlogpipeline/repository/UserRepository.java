package com.example.eventlogpipeline.repository;

import com.example.eventlogpipeline.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}