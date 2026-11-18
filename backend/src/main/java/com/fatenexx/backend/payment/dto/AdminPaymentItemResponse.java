package com.fatenexx.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.product.entity.PtType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPaymentItemResponse(
        Long paymentId,
        Long userId,
        String loginId,
        String userName,
        String status,
        int amount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime paidAt,
        PurchaseType purchaseType,      // 회원권 / PT
        PtType ptType,                  // PT일 때 개인 / 그룹, 회원권이면 null
        String productName,
        LocalDate startDate,
        LocalDate endDate
) {
}