package com.fatenexx.backend.reservation.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CanceledBy implements LabeledEnum {
    MEMBER("회원"),
    TRAINER("트레이너"),
    ADMIN("관리자"),
    CLASS_CLOSED("폐강");
    @JsonValue                  // API 응답은 한글 라벨로
    private final String label;

    // JSON 요청: 한글 라벨과 영어 이름 둘 다 받는다
    @JsonCreator
    public static CanceledBy fromJson(String value) {
        return LabeledEnum.fromValue(CanceledBy.class, value);
    }
}
