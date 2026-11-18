package com.fatenexx.backend.attendance.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EntryType implements LabeledEnum {
    MEMBERSHIP("회원권"), PT("PT");
    @JsonValue                  // API 응답은 한글 라벨로
    private final String label;

    // JSON 요청: 한글 라벨과 영어 이름 둘 다 받는다
    @JsonCreator
    public static EntryType fromJson(String value) {
        return LabeledEnum.fromValue(EntryType.class, value);
    }
}
