package com.fatenexx.backend.group.entity;

import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "group_classes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;               // 개설한 트레이너

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 200)
    private String description;         // 선택 입력

    @Column(name = "workout", length = 20)                              // [추가]
    private String workout;             // 운동 종류(서킷/하체/...), 선택 입력. 없으면 null   // [추가]

    @Column(name = "image_url", columnDefinition = "MEDIUMTEXT")        // [추가]
    private String imageUrl;            // 수업 사진 data URL(720px JPEG), 선택 입력. 없으면 null   // [추가]

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "capacity", nullable = false)
    private Integer capacity;           // 4, 5, 6

    @Convert(converter = GroupClassStatusConverter.class)
    @Column(name = "status", nullable = false, length = 10)
    private GroupClassStatus status;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수업 시간은 1시간 고정 (BR-GRP)
    public static GroupClass create(User trainer, String name, String description,
                                    LocalDate classDate, LocalTime startTime, int capacity) {
        GroupClass g = new GroupClass();
        g.trainer = trainer;
        g.name = name;
        g.description = description;
        g.classDate = classDate;
        g.startTime = startTime;
        g.endTime = startTime.plusHours(1);
        g.capacity = capacity;
        g.status = GroupClassStatus.OPEN;
        return g;
    }

    // 신청자 0명일 때만 수정 가능 (검사는 서비스에서)
    public void update(String name, String description, LocalDate classDate, LocalTime startTime, int capacity) {
        this.name = name;
        this.description = description;
        this.classDate = classDate;
        this.startTime = startTime;
        this.endTime = startTime.plusHours(1);
        this.capacity = capacity;
    }

    // [추가] 운동 종류·사진 변경 (null이면 비움). 값 검사는 서비스에서
    public void changeVisual(String workout, String imageUrl) {
        this.workout = workout;
        this.imageUrl = imageUrl;
    }

    public void confirm() { this.status = GroupClassStatus.CONFIRMED; }   // 24시간 전 2명 이상
    public void close()   { this.status = GroupClassStatus.CLOSED; }      // 24시간 전 2명 미만 → 폐강

    public void cancel(String reason) {                                   // 트레이너 사정 취소
        this.status = GroupClassStatus.CANCELED;
        this.cancelReason = reason;
        this.canceledAt = LocalDateTime.now();
    }
}