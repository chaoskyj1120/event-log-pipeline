package com.example.eventlogpipeline.service;

import com.example.eventlogpipeline.entity.DeviceType;
import com.example.eventlogpipeline.entity.EventType;
import com.example.eventlogpipeline.entity.Product;
import com.example.eventlogpipeline.entity.User;
import com.example.eventlogpipeline.exception.ProductNotFoundException;
import com.example.eventlogpipeline.exception.UserNotFoundException;
import com.example.eventlogpipeline.repository.ProductRepository;
import com.example.eventlogpipeline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EventLogService eventLogService;

    @Transactional
    public Product getProduct(UUID productId, UUID userId, DeviceType deviceType, Instant eventTime) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
            eventLogService.log(user, EventType.PAGE_VIEW, product, deviceType, eventTime);
        }

        return product;
    }
}