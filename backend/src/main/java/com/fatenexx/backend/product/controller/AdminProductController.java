package com.fatenexx.backend.product.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.product.dto.*;
import com.fatenexx.backend.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Roles(Role.ADMIN)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public ApiResponse<ProductListResponse> getAllProducts() {
        return ApiResponse.ok(productService.getAllProducts(), "상품 목록을 조회했습니다.");
    }

    @PostMapping("/membership-products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MembershipProductResponse> createMembershipProduct(
            @Valid @RequestBody MembershipProductCreateRequest request) {
        return ApiResponse.ok(productService.createMembershipProduct(request), "회원권 상품이 등록되었습니다.");
    }

    @PatchMapping("/membership-products/{productId}")
    public ApiResponse<MembershipProductResponse> updateMembershipProduct(
            @PathVariable Long productId, @Valid @RequestBody MembershipProductUpdateRequest request) {
        return ApiResponse.ok(productService.updateMembershipProduct(productId, request), "회원권 상품이 수정되었습니다.");
    }

    @PostMapping("/pt-products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PtProductResponse> createPtProduct(@Valid @RequestBody PtProductCreateRequest request) {
        return ApiResponse.ok(productService.createPtProduct(request), "PT 상품이 등록되었습니다.");
    }

    @PatchMapping("/pt-products/{productId}")
    public ApiResponse<PtProductResponse> updatePtProduct(
            @PathVariable Long productId, @Valid @RequestBody PtProductUpdateRequest request) {
        return ApiResponse.ok(productService.updatePtProduct(productId, request), "PT 상품이 수정되었습니다.");
    }
}