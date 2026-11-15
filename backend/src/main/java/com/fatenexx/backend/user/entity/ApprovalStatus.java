package com.fatenexx.backend.user.entity;

import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalStatus implements LabeledEnum {
    PENDING("승인대기"), APPROVED("승인");
    private final String label;
}