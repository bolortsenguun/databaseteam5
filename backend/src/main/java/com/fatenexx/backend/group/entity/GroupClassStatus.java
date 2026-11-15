package com.fatenexx.backend.group.entity;

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
    private final String label;
}