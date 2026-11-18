package com.fatenexx.backend.payment.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.payment.dto.*;
import com.fatenexx.backend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@Roles(Role.ADMIN)
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ApiResponse<PaymentPageResponse<AdminPaymentItemResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(paymentService.getAllPayments(page, size), "조회되었습니다.");
    }
}