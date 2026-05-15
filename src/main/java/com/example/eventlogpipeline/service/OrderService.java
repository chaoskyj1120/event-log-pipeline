package com.example.eventlogpipeline.service;

import com.example.eventlogpipeline.entity.*;
import com.example.eventlogpipeline.exception.OutOfStockException;
import com.example.eventlogpipeline.exception.ProductNotFoundException;
import com.example.eventlogpipeline.exception.UserNotFoundException;
import com.example.eventlogpipeline.repository.OrderRepository;
import com.example.eventlogpipeline.repository.ProductRepository;
import com.example.eventlogpipeline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EventLogService eventLogService;

    @Transactional
    public Order createOrder(UUID userId, UUID productId, int quantity, DeviceType deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        try {
            product.decreaseStock(quantity);
        } catch (OutOfStockException e) {
            eventLogService.logFailure(user, EventType.ORDER_FAILED, product, deviceType, "OUT_OF_STOCK");
            throw e;
        }

        Order order = Order.builder()
                .user(user)
                .product(product)
                .orderStatus(OrderStatus.PENDING)
                .quantity(quantity)
                .priceAtOrder(BigDecimal.valueOf(product.getPrice()))
                .discountAtOrder(product.getDiscount())
                .build();

        orderRepository.save(order);
        eventLogService.log(user, EventType.ORDER_CREATED, product, deviceType);
        return order;
    }
}