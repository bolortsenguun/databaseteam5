package com.fatenexx.backend.reservation.dto;

import java.util.List;

public record AdminReservationListResponse(
        List<AdminReservationResponse> reservations,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}