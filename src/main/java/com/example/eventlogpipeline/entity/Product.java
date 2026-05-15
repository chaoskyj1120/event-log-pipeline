package com.example.eventlogpipeline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_category", columnList = "category_id"),
                @Index(name = "idx_products_price",    columnList = "price"),
                @Index(name = "idx_products_discount", columnList = "discount")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @Column(name = "product_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "price")
    private Integer price;

    @Column(name = "discount", precision = 4, scale = 3)
    private BigDecimal discount;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Builder
    public Product(UUID productId, String productName, Category category, Integer price, BigDecimal discount, int stock) {
        this.productId = productId != null ? productId : UUID.randomUUID();
        this.productName = productName;
        this.category = category;
        this.price = price;
        this.discount = discount;
        this.stock = stock;
    }
}
