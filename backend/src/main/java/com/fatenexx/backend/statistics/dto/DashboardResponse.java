package com.fatenexx.backend.statistics.dto;

import java.time.LocalDate;

public record DashboardResponse(
        LocalDate date,
        long todayEntryCount,         // 오늘 입장 수
        long activeMemberCount,       // 오늘 유효한 회원권을 가진 회원 수
        long todayPtCount,            // 오늘 PT 수업 수 (취소 제외)
        long pendingResultCount,      // 결과대기 예약 수
        long closingSoonClassCount,   // 마감 임박(개설 + 최소 인원 미달) 그룹 수업 수
        long todayRevenue,            // 오늘 매출 (성공 결제)
        long monthlyRevenue,          // 이번 달 1일 ~ 오늘 매출 (성공 결제)
        long totalMembers,            // 전체 회원 수
        long approvedTrainers,        // 승인된 트레이너 수
        long pendingTrainers          // 승인 대기 트레이너 수
) {
}