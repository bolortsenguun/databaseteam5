package com.fatenexx.backend.payment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPaymentItemResponse(
        Long paymentId,
        Long userId,
        String loginId,
        String userName,
        String status,
        int amount,
        LocalDateTime paidAt,
        PurchaseType purchaseType,
        String productName,
        LocalDate startDate,
        LocalDate endDate
) {
}