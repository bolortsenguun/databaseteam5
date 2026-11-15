package com.fatenexx.backend.global.auth;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleConverter extends LabeledEnumConverter<Role> {
    public RoleConverter() { super(Role.class); }
}