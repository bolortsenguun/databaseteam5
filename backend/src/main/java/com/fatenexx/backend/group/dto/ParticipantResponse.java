package com.fatenexx.backend.group.dto;

import com.fatenexx.backend.reservation.entity.PtReservation;

public record ParticipantResponse(
        Long reservationId,
        Long userId,
        String name,
        String reservationStatus
) {
    public static ParticipantResponse from(PtReservation r) {
        return new ParticipantResponse(r.getId(), r.getUser().getId(), r.getUser().getName(),
                r.getStatus().getLabel());
    }
}