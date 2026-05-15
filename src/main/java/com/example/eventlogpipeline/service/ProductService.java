package com.example.eventlogpipeline.service;

import com.example.eventlogpipeline.entity.Product;
import com.example.eventlogpipeline.exception.ProductNotFoundException;
import com.example.eventlogpipeline.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}