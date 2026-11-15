package com.fatenexx.backend.group.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class GroupClassStatusConverter extends LabeledEnumConverter<GroupClassStatus> {
    public GroupClassStatusConverter() { super(GroupClassStatus.class); }
}