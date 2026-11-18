package com.fatenexx.backend.statistics.dto;

public record TrainerPerformanceItem(
        Long trainerId,
        String trainerName,
        long attendedCount,    // 출석
        long absentCount,      // 결석
        long canceledCount,    // 취소
        long upcomingCount     // 예정 (예약 + 결과대기)
) {
}