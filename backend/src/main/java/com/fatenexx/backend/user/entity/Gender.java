package com.fatenexx.backend.user.entity;

import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender implements LabeledEnum {
    MALE("남"), FEMALE("여");
    private final String label;
}