package com.fatenexx.backend.trainer.entity;

import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "trainer_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainerSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;          // 1=월 ~ 7=일

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public static TrainerSchedule create(User trainer, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        TrainerSchedule s = new TrainerSchedule();
        s.trainer = trainer;
        s.dayOfWeek = dayOfWeek;
        s.startTime = startTime;
        s.endTime = endTime;
        return s;
    }
}
