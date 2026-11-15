package com.fatenexx.backend.attendance.entity;

import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EntryType implements LabeledEnum {
    MEMBERSHIP("회원권"), PT("PT");
    private final String label;
}