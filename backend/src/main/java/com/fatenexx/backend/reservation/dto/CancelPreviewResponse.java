package com.fatenexx.backend.reservation.dto;

public record CancelPreviewResponse(
        Long reservationId,
        boolean canCancel,
        boolean willDeduct,
        int deductionCount,
        String message
) {
}