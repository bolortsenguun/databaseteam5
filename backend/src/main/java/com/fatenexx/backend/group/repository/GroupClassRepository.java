package com.fatenexx.backend.group.repository;

import com.fatenexx.backend.group.entity.GroupClass;
import com.fatenexx.backend.group.entity.GroupClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface GroupClassRepository extends JpaRepository<GroupClass, Long> {

    // 트레이너의 특정 상태들(개설·확정 등) 수업 중 date 이후 것
    List<GroupClass> findByTrainerIdAndStatusInAndClassDateGreaterThanEqual(
            Long trainerId, Collection<GroupClassStatus> statuses, LocalDate date);

    boolean existsByTrainerIdAndClassDateAndStatusIn(Long trainerId, LocalDate classDate,
                                                     Collection<GroupClassStatus> statuses);

    // 그날의 그룹 수업 (특정 상태들만)
    List<GroupClass> findByTrainerIdAndClassDateAndStatusIn(
            Long trainerId, LocalDate classDate, Collection<GroupClassStatus> statuses);

    // 트레이너의 기간 내 수업 (날짜·시간 순)
    List<GroupClass> findByTrainerIdAndClassDateBetweenOrderByClassDateAscStartTimeAsc(
            Long trainerId, LocalDate start, LocalDate end);
}