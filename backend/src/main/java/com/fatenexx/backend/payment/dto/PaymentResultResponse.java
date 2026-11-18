package com.fatenexx.backend.payment.dto;

import com.fatenexx.backend.payment.entity.Payment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResultResponse(
        Long paymentId,
        String status,
        int amount,
        LocalDateTime paidAt,
        PurchaseType purchaseType,
        String productName,
        LocalDate startDate,
        LocalDate endDate
) {
    public static PaymentResultResponse of(Payment p, PurchaseType type, String productName,
                                           LocalDate startDate, LocalDate endDate) {
        return new PaymentResultResponse(p.getId(), p.getStatus().name(), p.getAmount(),
                p.getPaidAt(), type, productName, startDate, endDate);
    }
}