package com.fatenexx.backend.payment.controller;

import com.fatenexx.backend.global.auth.Login;
import com.fatenexx.backend.global.auth.LoginUser;
import com.fatenexx.backend.global.auth.Role;
import com.fatenexx.backend.global.auth.Roles;
import com.fatenexx.backend.global.response.ApiResponse;
import com.fatenexx.backend.payment.dto.*;
import com.fatenexx.backend.payment.entity.PaymentStatus;
import com.fatenexx.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Roles(Role.MEMBER)
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<PaymentResultResponse>> simulate(
            @Login LoginUser loginUser, @Valid @RequestBody SimulatePaymentRequest request) {
        PaymentResultResponse result = paymentService.simulate(loginUser.userId(), request);
        if (PaymentStatus.SUCCESS.getLabel().equals(result.status())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "결제가 완료되었습니다."));
        }
        return ResponseEntity.ok(ApiResponse.ok(result, "결제에 실패했습니다."));
    }

    @GetMapping("/me")
    public ApiResponse<PaymentPageResponse<PaymentItemResponse>> myPayments(
            @Login LoginUser loginUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(paymentService.getMyPayments(loginUser.userId(), page, size), "조회되었습니다.");
    }
}