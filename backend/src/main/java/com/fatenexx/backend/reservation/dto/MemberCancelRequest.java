package com.fatenexx.backend.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record MemberCancelRequest(
        @NotNull Boolean acceptDeduction
) {
}