package com.fatenexx.backend.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record KioskCheckinResponse(
        String userName,          // 개인정보 보호를 위해 가운데 글자를 가린 이름
        String entryType,         // 회원권 / PT
        Long reservationId,       // PT 입장일 때만
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt,
        boolean recorded,         // false면 10분 안 재입장이라 새로 기록하지 않음
        String message,
        String name,                      // 입장 완료 화면용: 가리지 않은 이름
        KioskMembershipInfo membership,   // 오늘 유효한 회원권 (없으면 null)
        KioskPtInfo pt,                   // 쓰고 있는 PT 이용권 (없으면 null)
        long monthAttendance,             // 이번 달 출석 일수
        KioskNextLesson nextLesson        // 가장 가까운 PT 수업 (없으면 null)
) {
}