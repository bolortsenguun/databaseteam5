package com.fatenexx.backend.user.dto;

import com.fatenexx.backend.user.entity.Gender;
import com.fatenexx.backend.user.entity.User;

import java.time.LocalDateTime;

public record MemberSummaryResponse(
        Long userId, String loginId, String name, String phone, Gender gender, LocalDateTime createdAt,
        String membershipStatus     // 이용중 / 미이용 (오늘 유효한 회원권 기준)
) {
    public static final String ACTIVE = "이용중";
    public static final String INACTIVE = "미이용";

    public static MemberSummaryResponse from(User u, boolean membershipActive) {
        return new MemberSummaryResponse(u.getId(), u.getLoginId(), u.getName(),
                u.getPhone(), u.getGender(), u.getCreatedAt(),
                membershipActive ? ACTIVE : INACTIVE);
    }
}
