package com.fatenexx.backend.reservation.dto;

import java.util.List;

public record MyReservationListResponse(
        List<MyReservationResponse> reservations
) {
}