package com.example.eventlogpipeline.controller;

import com.example.eventlogpipeline.entity.DeviceType;
import com.example.eventlogpipeline.entity.Order;
import com.example.eventlogpipeline.entity.OrderStatus;
import com.example.eventlogpipeline.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request.userId(), request.productId(), request.quantity(), request.deviceType(),
                request.eventTime() != null ? request.eventTime() : Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(new OrderResponse(
                order.getOrderId(),
                order.getOrderStatus(),
                order.getQuantity(),
                order.getPriceAtOrder(),
                order.getDiscountAtOrder(),
                order.getCreatedAt()
        ));
    }

    public record CreateOrderRequest(UUID userId, UUID productId, int quantity, DeviceType deviceType, Instant eventTime) {}
    public record OrderResponse(UUID orderId, OrderStatus orderStatus, int quantity, BigDecimal priceAtOrder,
                                BigDecimal discountAtOrder, Instant createdAt) {}
}