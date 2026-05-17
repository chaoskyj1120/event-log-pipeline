package com.example.eventlogpipeline.repository;

import com.example.eventlogpipeline.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}