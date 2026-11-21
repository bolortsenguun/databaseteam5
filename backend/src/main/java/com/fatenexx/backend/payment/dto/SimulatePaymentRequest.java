package com.fatenexx.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SimulatePaymentRequest(
        @JsonAlias("productType") @NotNull PurchaseType purchaseType,   // [추가] 명세서 이름(productType)도 받음
        @NotNull Long productId,
        Long trainerId,
        LocalDate startDate,
        Boolean simulateFail,
        String simulationResult                                          // [추가] 명세서 방식: "성공" / "실패"
) {
}