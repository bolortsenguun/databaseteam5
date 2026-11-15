package com.fatenexx.backend.product.entity;

import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PtType implements LabeledEnum {
    PERSONAL("개인"), GROUP("그룹");
    private final String label;
}