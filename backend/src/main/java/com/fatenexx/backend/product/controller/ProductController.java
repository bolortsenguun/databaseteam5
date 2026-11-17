package com.fatenexx.backend.product.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.product.dto.ProductListResponse;
import com.fatenexx.backend.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @Roles(Role.MEMBER)
    @GetMapping
    public ApiResponse<ProductListResponse> getProducts() {
        return ApiResponse.ok(productService.getActiveProducts(), "상품 목록을 조회했습니다.");
    }
}