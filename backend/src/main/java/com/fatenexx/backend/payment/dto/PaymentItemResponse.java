package com.fatenexx.backend.payment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentItemResponse(
        Long paymentId,
        String status,
        int amount,
        LocalDateTime paidAt,
        PurchaseType purchaseType,
        String productName,
        LocalDate startDate,
        LocalDate endDate
) {
}