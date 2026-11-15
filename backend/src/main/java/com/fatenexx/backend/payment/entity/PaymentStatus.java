package com.fatenexx.backend.payment.entity;

import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus implements LabeledEnum {
    SUCCESS("성공"), FAIL("실패");
    private final String label;
}