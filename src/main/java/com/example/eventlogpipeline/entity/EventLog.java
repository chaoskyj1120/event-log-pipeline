package com.example.eventlogpipeline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "event_logs",
        indexes = {
                @jakarta.persistence.Index(name = "idx_event_logs_user",      columnList = "user_id"),
                @jakarta.persistence.Index(name = "idx_event_logs_type",      columnList = "event_type"),
                @jakarta.persistence.Index(name = "idx_event_logs_time",      columnList = "event_time"),
                @jakarta.persistence.Index(name = "idx_event_logs_product",   columnList = "product_id"),
                @jakarta.persistence.Index(name = "idx_event_logs_user_time", columnList = "user_id, event_time")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventLog {

    @Id
    @Column(name = "event_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", length = 50, nullable = false)
    private EventType eventType;

    @CreationTimestamp
    @Column(name = "event_time", nullable = false, updatable = false)
    private Instant eventTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "page_url", length = 255)
    private String pageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    private DeviceType deviceType;

    @Column(name = "price_at_event", precision = 10, scale = 2)
    private BigDecimal priceAtEvent;

    @Column(name = "discount_at_event", precision = 4, scale = 3)
    private BigDecimal discountAtEvent;
}