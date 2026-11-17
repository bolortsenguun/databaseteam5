package com.fatenexx.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    TRAINER_NOT_APPROVED(HttpStatus.FORBIDDEN, "관리자 승인 대기 중입니다."),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "비밀번호를 변경해 주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // Auth
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 등록된 전화번호입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INCORRECT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),

    SETTING_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "설정값을 찾을 수 없습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),

    // Trainer
    TRAINER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 트레이너를 찾을 수 없습니다."),
    SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "이미 등록된 근무 시간과 겹칩니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 근무 일정을 찾을 수 없습니다."),
    SCHEDULE_HAS_BOOKINGS(HttpStatus.CONFLICT, "예약된 PT 또는 그룹 수업이 있어 근무 일정을 삭제할 수 없습니다."),
    DAY_OFF_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 휴무로 등록된 날짜입니다."),
    DAY_OFF_CONFLICT(HttpStatus.CONFLICT, "해당 날짜에 예약된 수업이 있어 휴무를 등록할 수 없습니다."),
    DAY_OFF_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 휴무일을 찾을 수 없습니다."),
    ALREADY_APPROVED(HttpStatus.CONFLICT, "이미 승인된 트레이너입니다."),

    // Group Class
    INVALID_CLASS_DATE(HttpStatus.BAD_REQUEST, "그룹 수업을 개설할 수 없는 날짜입니다."),
    TRAINER_UNAVAILABLE(HttpStatus.CONFLICT, "해당 시간에는 그룹 수업을 개설할 수 없습니다."),
    CLASS_TIME_CONFLICT(HttpStatus.CONFLICT, "해당 시간에 다른 PT 예약 또는 그룹 수업이 있습니다."),
    GROUP_CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 그룹 수업을 찾을 수 없습니다."),
    CLASS_HAS_PARTICIPANTS(HttpStatus.CONFLICT, "신청한 회원이 있어 그룹 수업을 수정할 수 없습니다."),
    CLASS_HAS_RESERVATIONS(HttpStatus.CONFLICT, "예약 이력이 있는 그룹 수업은 삭제할 수 없습니다. 수업 취소 기능을 이용해 주세요."),
    CLASS_NOT_CANCELABLE(HttpStatus.CONFLICT, "이미 취소되었거나 시작된 그룹 수업은 취소할 수 없습니다."),
    CANCEL_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "취소 사유를 입력해 주세요."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "조회 가능한 날짜 범위를 확인해 주세요."),

    // Reservation
    SLOT_UNAVAILABLE(HttpStatus.CONFLICT, "선택한 시간에는 예약할 수 없습니다."),
    BOOKING_CLOSED(HttpStatus.CONFLICT, "수업 시작 1시간 전까지만 예약할 수 있습니다."),
    DAILY_LIMIT_REACHED(HttpStatus.CONFLICT, "하루에 한 번만 PT를 예약할 수 있습니다."),
    NO_VALID_PERSONAL_PASS(HttpStatus.CONFLICT, "해당 트레이너의 유효한 개인 PT 이용권이 없습니다."),
    NO_VALID_GROUP_PASS(HttpStatus.CONFLICT, "유효한 그룹 PT 이용권이 없습니다."),
    NO_AVAILABLE_COUNT(HttpStatus.CONFLICT, "예약 가능한 PT 횟수가 부족합니다."),
    REBOOK_NOT_ALLOWED(HttpStatus.CONFLICT, "이미 차감 취소한 수업은 다시 예약할 수 없습니다."),
    CLASS_FULL(HttpStatus.CONFLICT, "그룹 수업 정원이 마감되었습니다."),
    ALREADY_RESERVED(HttpStatus.CONFLICT, "이미 신청한 그룹 수업입니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예약을 찾을 수 없습니다."),
    RESERVATION_NOT_CANCELABLE(HttpStatus.CONFLICT, "이미 취소되었거나 수업이 시작된 예약은 취소할 수 없습니다."),
    DEDUCTION_CONFIRMATION_REQUIRED(HttpStatus.CONFLICT, "취소 시 PT 1회가 차감됩니다. 확인 후 다시 요청해 주세요."),
    INVALID_RESERVATION_STATE(HttpStatus.CONFLICT, "해당 예약은 출석·결석 처리할 수 없습니다."),
    LESSON_NOT_STARTED(HttpStatus.CONFLICT, "수업 시작 후 출석·결석을 처리할 수 있습니다."),
    INVALID_RESERVATION_TYPE(HttpStatus.CONFLICT, "그룹 PT 수업은 그룹 수업 취소 기능을 이용해 주세요.");


    private final HttpStatus status;
    private final String message;
}