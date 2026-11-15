package com.fatenexx.backend.user.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter extends LabeledEnumConverter<Gender> {
    public GenderConverter() { super(Gender.class); }
}