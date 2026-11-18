package com.fatenexx.backend.pass.dto;

import java.time.LocalDate;

public record ClientPassResponse(
        Long ptPassId,
        String productName,
        LocalDate startDate,
        LocalDate endDate,
        int totalCount,
        int remainingCount,
        int usedCount,          // 차감된 횟수 = total - remaining
        long reservedCount,     // 예약 + 결과대기로 확보된 횟수
        long availableCount,    // 새로 예약할 수 있는 횟수 (만료·소진이면 0)
        String status           // BEFORE_START / ACTIVE / EXHAUSTED / EXPIRED
) {
}
