package com.fatenexx.backend.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record KioskCheckinResponse(
        String userName,          // 개인정보 보호를 위해 가운데 글자를 가린 이름
        String entryType,         // 회원권 / PT
        Long reservationId,       // PT 입장일 때만
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt,
        boolean recorded,         // false면 10분 안 재입장이라 새로 기록하지 않음
        String message
) {
}