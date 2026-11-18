package com.fatenexx.backend.attendance.dto;

import java.time.LocalDateTime;

public record KioskCheckinResponse(
        String userName,          // 개인정보 보호를 위해 가운데 글자를 가린 이름
        String entryType,         // MEMBERSHIP / PT
        Long reservationId,       // PT 입장일 때만
        LocalDateTime checkedInAt,
        boolean recorded,         // false면 10분 안 재입장이라 새로 기록하지 않음
        String message
) {
}