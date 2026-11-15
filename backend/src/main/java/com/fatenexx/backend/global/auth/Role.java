package com.fatenexx.backend.global.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.fatenexx.backend.global.converter.LabeledEnum;

@Getter
@RequiredArgsConstructor
public enum Role implements LabeledEnum {
    MEMBER("회원"), TRAINER("트레이너"), ADMIN("관리자");

    private final String label;

    public static Role fromLabel(String label) {
        for (Role r : values()) {
            if (r.label.equals(label)) return r;
        }
        throw new IllegalArgumentException("알 수 없는 역할: " + label);
    }
}