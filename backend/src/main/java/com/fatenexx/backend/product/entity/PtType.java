package com.fatenexx.backend.product.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PtType implements LabeledEnum {
    PERSONAL("개인"), GROUP("그룹");
    @JsonValue                  // API 응답은 한글 라벨로
    private final String label;

    // JSON 요청: 한글 라벨과 영어 이름 둘 다 받는다
    @JsonCreator
    public static PtType fromJson(String value) {
        return LabeledEnum.fromValue(PtType.class, value);
    }
}
