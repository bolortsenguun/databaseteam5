package com.fatenexx.backend.payment.controller;

import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.payment.dto.*;
import com.fatenexx.backend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@Roles(Role.ADMIN)
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ApiResponse<PaymentPageResponse<AdminPaymentItemResponse>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,          // 성공 / 실패 (없으면 전체)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(paymentService.getAllPayments(userId, status, startDate, endDate, page, size),
                "조회되었습니다.");
    }
}