package com.fatenexx.backend.statistics.dto;

public record TrainerPerformanceItem(
        Long trainerId,
        String trainerName,
        long attendedCount,    // 출석 (개인 + 그룹)
        long absentCount,      // 결석 (개인 + 그룹)
        long canceledCount,    // 취소
        long upcomingCount,    // 예정 (예약 + 결과대기)
        // 개인·그룹 나눠서
        long individualAttendanceCount,   // 개인 PT 출석
        long individualAbsenceCount,      // 개인 PT 결석
        long groupClassesHeld,            // 진행된(확정) 그룹 수업 수
        long groupAttendanceCount,        // 그룹 PT 출석 (참여자 기준)
        long groupAbsenceCount,           // 그룹 PT 결석 (참여자 기준)
        long totalProcessedCount          // 출석 + 결석 (개인 + 그룹)
) {
}
