package com.fatenexx.backend.global.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.fatenexx.backend.global.converter.LabeledEnum;

@Getter
@RequiredArgsConstructor
public enum Role implements LabeledEnum {
    MEMBER("회원"), TRAINER("트레이너"), ADMIN("관리자");

    @JsonValue                  // API 응답은 한글 라벨로
    private final String label;

    public static Role fromLabel(String label) {
        for (Role r : values()) {
            if (r.label.equals(label)) return r;
        }
        throw new IllegalArgumentException("알 수 없는 역할: " + label);
    }

    // JSON 요청: 한글 라벨과 영어 이름 둘 다 받는다
    @JsonCreator
    public static Role fromJson(String value) {
        return LabeledEnum.fromValue(Role.class, value);
    }
}
