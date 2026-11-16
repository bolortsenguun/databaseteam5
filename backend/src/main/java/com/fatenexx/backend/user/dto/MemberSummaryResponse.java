package com.fatenexx.backend.user.dto;

import com.fatenexx.backend.user.entity.Gender;
import com.fatenexx.backend.user.entity.User;

import java.time.LocalDateTime;

public record MemberSummaryResponse(
        Long userId, String loginId, String name, String phone, Gender gender, LocalDateTime createdAt
) {
    public static MemberSummaryResponse from(User u) {
        return new MemberSummaryResponse(u.getId(), u.getLoginId(), u.getName(),
                u.getPhone(), u.getGender(), u.getCreatedAt());
    }
}