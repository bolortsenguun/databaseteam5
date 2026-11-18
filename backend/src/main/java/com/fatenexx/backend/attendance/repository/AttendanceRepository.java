package com.fatenexx.backend.attendance.repository;

import com.fatenexx.backend.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 가장 최근 입장 기록 (10분 재입장 판정)
    Optional<Attendance> findFirstByUserIdOrderByCheckedInAtDesc(Long userId);

    // 내 출석 기록 (기간: from 이상 ~ to 미만)
    Page<Attendance> findByUserIdAndCheckedInAtGreaterThanEqualAndCheckedInAtLessThan(
            Long userId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // 관리자: 하루치 입장 기록 (회원 정보 포함)
    @Query("select a from Attendance a join fetch a.user " +
            "where a.checkedInAt >= :from and a.checkedInAt < :to " +
            "order by a.checkedInAt desc")
    List<Attendance> findByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}