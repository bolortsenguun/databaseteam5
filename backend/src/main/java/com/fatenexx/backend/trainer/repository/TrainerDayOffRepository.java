package com.fatenexx.backend.trainer.repository;

import com.fatenexx.backend.trainer.entity.TrainerDayOff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TrainerDayOffRepository extends JpaRepository<TrainerDayOff, Long> {

    // 트레이너의 휴무 목록 (날짜 순)
    List<TrainerDayOff> findByTrainerIdOrderByOffDateAsc(Long trainerId);

    boolean existsByTrainerIdAndOffDate(Long trainerId, LocalDate offDate);
}
