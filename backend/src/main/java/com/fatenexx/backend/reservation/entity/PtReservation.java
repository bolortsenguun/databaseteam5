package com.fatenexx.backend.reservation.entity;

import com.fatenexx.backend.group.entity.GroupClass;
import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "pt_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pt_pass_id", nullable = false)
    private PtPass ptPass;              // 자동 선택된 이용권

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                  // 예약한 회원

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_class_id")
    private GroupClass groupClass;      // 그룹 PT일 때만, 개인 PT는 null

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Convert(converter = ReservationStatusConverter.class)
    @Column(name = "status", nullable = false, length = 10)
    private ReservationStatus status;

    @Convert(converter = CanceledByConverter.class)
    @Column(name = "canceled_by", length = 10)
    private CanceledBy canceledBy;      // 취소일 때만

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;        // 트레이너·관리자 취소 시 필수

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "is_deducted", nullable = false)
    private boolean deducted;           // 차감됐는지 (한 번만 차감)

    @CreationTimestamp
    @Column(name = "reserved_at", nullable = false, updatable = false)
    private LocalDateTime reservedAt;   // 10분 무료 취소 기준

    // 개인 PT 예약
    public static PtReservation createIndividual(PtPass pass, User user, User trainer,
                                                 LocalDate lessonDate, LocalTime startTime) {
        PtReservation r = new PtReservation();
        r.ptPass = pass;
        r.user = user;
        r.trainer = trainer;
        r.lessonDate = lessonDate;
        r.startTime = startTime;
        r.endTime = startTime.plusHours(1);
        r.status = ReservationStatus.RESERVED;
        r.deducted = false;
        return r;
    }

    // 그룹 PT 신청: 날짜·시간·트레이너는 수업에서 복사
    public static PtReservation createGroup(PtPass pass, User user, GroupClass groupClass) {
        PtReservation r = new PtReservation();
        r.ptPass = pass;
        r.user = user;
        r.trainer = groupClass.getTrainer();
        r.groupClass = groupClass;
        r.lessonDate = groupClass.getClassDate();
        r.startTime = groupClass.getStartTime();
        r.endTime = groupClass.getEndTime();
        r.status = ReservationStatus.RESERVED;
        r.deducted = false;
        return r;
    }

    public void cancel(CanceledBy by, String reason, boolean deduct) {
        this.status = ReservationStatus.CANCELED;
        this.canceledBy = by;
        this.cancelReason = reason;
        this.canceledAt = LocalDateTime.now();
        if (deduct) this.deducted = true;
    }

    public void markPendingResult() {
        this.status = ReservationStatus.PENDING_RESULT;
    }

    public void markResult(ReservationStatus result) {
        this.status = result;              // ATTENDED 또는 ABSENT
        this.deducted = true;
    }
}