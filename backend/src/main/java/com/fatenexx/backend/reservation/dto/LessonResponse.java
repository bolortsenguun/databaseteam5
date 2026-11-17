package com.fatenexx.backend.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fatenexx.backend.product.entity.PtType;
import com.fatenexx.backend.reservation.entity.PtReservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record LessonResponse(
        String ptType,
        Long groupClassId,
        String className,
        LocalDate lessonDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        List<LessonParticipantResponse> participants
) {
    // 같은 수업으로 묶인 예약들 → 수업 하나
    public static LessonResponse of(List<PtReservation> rs) {
        PtReservation first = rs.get(0);
        boolean group = first.getGroupClass() != null;
        return new LessonResponse(
                group ? PtType.GROUP.getLabel() : PtType.PERSONAL.getLabel(),
                group ? first.getGroupClass().getId() : null,
                group ? first.getGroupClass().getName() : null,
                first.getLessonDate(), first.getStartTime(), first.getEndTime(),
                rs.stream().map(LessonParticipantResponse::from).toList());
    }
}