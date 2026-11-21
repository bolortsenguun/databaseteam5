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
        String productType,             // 회원권 / 개인 PT / 그룹 PT (화면 표시용)
        PurchaseType purchaseType,      // 회원권 / PT
        PtType ptType,                  // PT일 때 개인 / 그룹, 회원권이면 null
        String productName,
        LocalDate startDate,
        LocalDate endDate
) {
}