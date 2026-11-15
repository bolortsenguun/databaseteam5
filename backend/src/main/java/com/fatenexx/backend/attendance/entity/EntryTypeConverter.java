package com.fatenexx.backend.attendance.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class EntryTypeConverter extends LabeledEnumConverter<EntryType> {
    public EntryTypeConverter() { super(EntryType.class); }
}