package com.fatenexx.backend.global.converter;

public interface LabeledEnum {
    String getLabel();

    /**
     * JSON 요청 값을 enum으로 바꾼다. 한글 라벨(회원)과 영어 이름(MEMBER) 둘 다 받는다.
     * API 응답은 한글 라벨로 통일하되, 기존 클라이언트가 보내는 영어 값도 깨지지 않게 하기 위함.
     */
    static <E extends Enum<E> & LabeledEnum> E fromValue(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        for (E e : type.getEnumConstants()) {
            if (e.getLabel().equals(value) || e.name().equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("알 수 없는 값: " + value);
    }
}
