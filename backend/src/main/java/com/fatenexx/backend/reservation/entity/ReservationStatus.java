package com.fatenexx.backend.reservation.entity;

import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus implements LabeledEnum {
    RESERVED("예약"),
    PENDING_RESULT("결과대기"),
    ATTENDED("출석"),
    ABSENT("결석"),
    CANCELED("취소");
    private final String label;
}