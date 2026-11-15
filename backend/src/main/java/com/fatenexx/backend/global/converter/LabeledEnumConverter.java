package com.fatenexx.backend.global.converter;

import jakarta.persistence.AttributeConverter;

public abstract class LabeledEnumConverter<E extends Enum<E> & LabeledEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected LabeledEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        for (E e : type.getEnumConstants()) {
            if (e.getLabel().equals(dbData)) return e;
        }
        throw new IllegalArgumentException("알 수 없는 값: " + dbData);
    }
}