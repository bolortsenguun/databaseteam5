package com.fatenexx.backend.attendance.dto;

public record KioskPtInfo(
        String name,
        int remaining,       // 차감 후 남은 횟수
        long reserved,       // 예약 + 결과대기로 확보된 횟수
        long available,      // 새로 예약할 수 있는 횟수
        int total
) {
}