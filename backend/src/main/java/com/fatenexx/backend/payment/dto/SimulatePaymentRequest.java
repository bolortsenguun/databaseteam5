package com.fatenexx.backend.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SimulatePaymentRequest(
        @NotNull PurchaseType purchaseType,
        @NotNull Long productId,
        Long trainerId,
        LocalDate startDate,
        Boolean simulateFail
) {
}