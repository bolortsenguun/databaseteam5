package com.fatenexx.backend.attendance.entity;

import com.fatenexx.backend.reservation.entity.PtReservation;
import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private LocalDateTime checkedInAt;

    @Convert(converter = EntryTypeConverter.class)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;        // 회원권 / PT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private PtReservation reservation;  // PT로 입장했을 때만

    public static Attendance create(User user, EntryType entryType, PtReservation reservation) {
        Attendance a = new Attendance();
        a.user = user;
        a.entryType = entryType;
        a.reservation = reservation;
        return a;
    }
}