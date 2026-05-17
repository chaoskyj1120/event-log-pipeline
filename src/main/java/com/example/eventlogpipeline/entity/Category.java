package com.example.eventlogpipeline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @Column(name = "category_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID categoryId;

    @Column(name = "category_name", length = 255)
    private String categoryName;

    @Builder
    public Category(UUID categoryId, String categoryName) {
        this.categoryId = categoryId != null ? categoryId : UUID.randomUUID();
        this.categoryName = categoryName;
    }
}
