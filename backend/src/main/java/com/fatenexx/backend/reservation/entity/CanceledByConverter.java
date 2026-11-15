package com.fatenexx.backend.reservation.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class CanceledByConverter extends LabeledEnumConverter<CanceledBy> {
    public CanceledByConverter() { super(CanceledBy.class); }
}