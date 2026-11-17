package com.fatenexx.backend.group.repository;

import com.fatenexx.backend.group.entity.GroupClass;
import com.fatenexx.backend.group.entity.GroupClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

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

    // 회원용 목록: 기간 내 개설·확정 수업 + 트레이너 정보
    @Query("select g from GroupClass g join fetch g.trainer " +
            "where g.status in :statuses and g.classDate between :start and :end " +
            "order by g.classDate, g.startTime")
    List<GroupClass> findForMember(@Param("statuses") Collection<GroupClassStatus> statuses,
                                   @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GroupClass g where g.id = :id")
    Optional<GroupClass> findByIdForUpdate(@Param("id") Long id);
}