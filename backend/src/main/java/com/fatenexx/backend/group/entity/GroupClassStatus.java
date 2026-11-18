package com.fatenexx.backend.group.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GroupClassStatus implements LabeledEnum {
    OPEN("개설"),           // 화면엔 "모집"
    CONFIRMED("확정"),
    CLOSED("폐강"),
    CANCELED("취소");
    @JsonValue                  // API 응답은 한글 라벨로
    private final String label;

    // JSON 요청: 한글 라벨과 영어 이름 둘 다 받는다
    @JsonCreator
    public static GroupClassStatus fromJson(String value) {
        return LabeledEnum.fromValue(GroupClassStatus.class, value);
    }
}
