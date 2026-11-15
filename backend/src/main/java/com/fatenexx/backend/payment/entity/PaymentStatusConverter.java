package com.fatenexx.backend.payment.entity;

import com.fatenexx.backend.global.converter.LabeledEnumConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentStatusConverter extends LabeledEnumConverter<PaymentStatus> {
    public PaymentStatusConverter() { super(PaymentStatus.class); }
}
