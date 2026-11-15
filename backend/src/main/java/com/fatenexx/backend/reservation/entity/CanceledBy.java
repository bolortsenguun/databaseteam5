package com.fatenexx.backend.reservation.entity;

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
    private final String label;
}