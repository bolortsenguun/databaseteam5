package com.fatenexx.backend.product.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class PtTypeConverter extends LabeledEnumConverter<PtType> {
    public PtTypeConverter() { super(PtType.class); }
}