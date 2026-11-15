package com.fatenexx.backend.user.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class ApprovalStatusConverter extends LabeledEnumConverter<ApprovalStatus> {
    public ApprovalStatusConverter() { super(ApprovalStatus.class); }
}
