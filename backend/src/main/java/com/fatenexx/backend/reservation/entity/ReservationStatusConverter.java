package com.fatenexx.backend.reservation.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class ReservationStatusConverter extends LabeledEnumConverter<ReservationStatus> {
    public ReservationStatusConverter() { super(ReservationStatus.class); }
}
