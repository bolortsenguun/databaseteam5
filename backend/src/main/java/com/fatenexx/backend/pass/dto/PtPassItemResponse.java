package com.fatenexx.backend.pass.dto;

import java.time.LocalDate;

public record PtPassItemResponse(
        Long ptPassId,
        String ptType,          // 개인 / 그룹
        String productName,
        Long trainerId,         // 그룹 PT는 null
        String trainerName,
        LocalDate startDate,
        LocalDate endDate,
        int totalCount,
        int remainingCount,     // 차감 후 남은 횟수
        long reservedCount,     // 예약 + 결과대기로 확보된 횟수
        long availableCount,    // 새로 예약할 수 있는 횟수 = remaining - reserved
        String status           // 이용예정 / 이용중 / 소진 / 만료
) {
}