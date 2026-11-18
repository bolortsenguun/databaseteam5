package com.fatenexx.backend.pass.dto;

import java.time.LocalDate;

public record MembershipItemResponse(
        Long membershipId,
        String productName,
        LocalDate startDate,
        LocalDate endDate,
        String status          // BEFORE_START / ACTIVE / EXPIRED
) {
}