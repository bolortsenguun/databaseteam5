package com.fatenexx.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fatenexx.backend.global.converter.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PurchaseType implements LabeledEnum {
    MEMBERSHIP("회원권"), PT("PT");

    @JsonValue                  // API 응답은 한글 라벨로
    private final String label;

    /** 화면용 구분: 회원권 / 개인 PT / 그룹 PT */
    public static String displayName(PurchaseType type, com.fatenexx.backend.product.entity.PtType ptType) {
        if (type == null) return null;
        if (type == MEMBERSHIP) return MEMBERSHIP.getLabel();
        return ptType == null ? PT.getLabel() : ptType.getLabel() + " PT";
    }

    // JSON 요청: 한글 라벨과 영어 이름 둘 다 받는다
    @JsonCreator
    public static PurchaseType fromJson(String value) {
        return LabeledEnum.fromValue(PurchaseType.class, value);
    }
}
