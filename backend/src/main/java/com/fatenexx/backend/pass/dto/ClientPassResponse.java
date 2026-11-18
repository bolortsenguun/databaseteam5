package com.fatenexx.backend.pass.dto;

import java.time.LocalDate;

public record ClientPassResponse(
        Long ptPassId,
        String productName,
        LocalDate startDate,
        LocalDate endDate,
        int totalCount,
        int remainingCount
) {
}