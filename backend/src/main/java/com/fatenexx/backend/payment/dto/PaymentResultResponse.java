package com.fatenexx.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.payment.entity.Payment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResultResponse(
        Long paymentId,
        String status,
        int amount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime paidAt,
        PurchaseType purchaseType,
        String productName,
        LocalDate startDate,
        LocalDate endDate,
        PassResult pass
) {
    // [추가] 결제로 생긴 이용권 요약
    public record PassResult(PurchaseType type, String name, LocalDate startDate, LocalDate endDate) {
    }

    public static PaymentResultResponse of(Payment p, PurchaseType type, String productName,
                                           LocalDate startDate, LocalDate endDate) {
        PassResult pass = (startDate == null) ? null                              // [추가] 실패하면 이용권 없음
                : new PassResult(type, productName, startDate, endDate);
        return new PaymentResultResponse(p.getId(), p.getStatus().getLabel(), p.getAmount(),
                p.getPaidAt(), type, productName, startDate, endDate, pass);
    }
}