package com.fatenexx.backend.payment.dto;

import java.util.List;

public record PaymentPageResponse<T>(
        List<T> payments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}