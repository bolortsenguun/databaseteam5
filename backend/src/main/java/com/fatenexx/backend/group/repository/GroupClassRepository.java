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
}