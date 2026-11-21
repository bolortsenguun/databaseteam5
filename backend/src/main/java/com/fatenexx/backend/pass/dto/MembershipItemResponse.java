package com.fatenexx.backend.pass.dto;

import java.time.LocalDate;

public record MembershipItemResponse(
        Long membershipId,
        String productName,
        LocalDate startDate,
        LocalDate endDate,
        String status,         // 이용예정 / 이용중 / 만료
        Integer price,         // 구매 당시 금액 (회원권 가격이 바뀌어도 그대로)
        int remainingDays      // 오늘부터 종료일까지 남은 일수 (이용중일 때만 계산, 그 외 0, 종료 당일은 0)
) {
}