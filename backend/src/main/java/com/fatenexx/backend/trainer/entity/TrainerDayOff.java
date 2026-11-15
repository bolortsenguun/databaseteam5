package com.fatenexx.backend.trainer.entity;

import com.fatenexx.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "trainer_days_off")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainerDayOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "day_off_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @Column(name = "off_date", nullable = false)
    private LocalDate offDate;

    public static TrainerDayOff create(User trainer, LocalDate offDate) {
        TrainerDayOff d = new TrainerDayOff();
        d.trainer = trainer;
        d.offDate = offDate;
        return d;
    }
}