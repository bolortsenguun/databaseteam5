package com.fatenexx.backend.reservation.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
    @JsonValue                  // API 응답은 한글 라벨로
    private final String label;

    // JSON 요청: 한글 라벨과 영어 이름 둘 다 받는다
    @JsonCreator
    public static ReservationStatus fromJson(String value) {
        return LabeledEnum.fromValue(ReservationStatus.class, value);
    }
}
