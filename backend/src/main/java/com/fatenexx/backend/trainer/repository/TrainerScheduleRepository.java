package com.fatenexx.backend.trainer.repository;

import com.fatenexx.backend.trainer.entity.TrainerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalTime;
import java.util.List;

public interface TrainerScheduleRepository extends JpaRepository<TrainerSchedule, Long> {

    // 트레이너의 근무 목록 (요일 → 시작 시간 순)
    List<TrainerSchedule> findByTrainerIdOrderByDayOfWeekAscStartTimeAsc(Long trainerId);

    // 같은 요일에 겹치는 근무가 있는지
    boolean existsByTrainerIdAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
            Long trainerId, Integer dayOfWeek, LocalTime endTime, LocalTime startTime);
}