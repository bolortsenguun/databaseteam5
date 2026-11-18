package com.fatenexx.backend.pass.dto;

import java.time.LocalDate;

public record MembershipItemResponse(
        Long membershipId,
        String productName,
        LocalDate startDate,
        LocalDate endDate,
        String status          // 이용예정 / 이용중 / 만료
) {
}