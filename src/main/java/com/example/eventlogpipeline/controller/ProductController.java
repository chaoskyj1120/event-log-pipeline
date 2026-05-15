package com.example.eventlogpipeline.controller;

import com.example.eventlogpipeline.entity.Product;
import com.example.eventlogpipeline.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID productId) {
        Product product = productService.getProduct(productId);
        return ResponseEntity.status(HttpStatus.OK).body(new ProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getPrice(),
                product.getDiscount(),
                product.getStock()
        ));
    }

    public record ProductResponse(UUID productId, String productName, Integer price, BigDecimal discount, int stock) {}
}