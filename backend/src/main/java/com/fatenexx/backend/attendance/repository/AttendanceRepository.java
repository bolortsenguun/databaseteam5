package com.fatenexx.backend.attendance.repository;

import com.fatenexx.backend.attendance.entity.Attendance;
import com.fatenexx.backend.attendance.entity.EntryType;
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

    // 관리자 회원 상세: 최근 입장 기록 10건
    List<Attendance> findTop10ByUserIdOrderByCheckedInAtDesc(Long userId);

    // 관리자 출석 검색: 조건이 null이면 무시. 기간은 from 이상 ~ to 미만, 최신순
    @Query(value = "select a from Attendance a join fetch a.user u " +
            "where (:userId is null or u.id = :userId) " +
            "and (:entryType is null or a.entryType = :entryType) " +
            "and (:from is null or a.checkedInAt >= :from) " +
            "and (:to is null or a.checkedInAt < :to)",
            countQuery = "select count(a) from Attendance a " +
                    "where (:userId is null or a.user.id = :userId) " +
                    "and (:entryType is null or a.entryType = :entryType) " +
                    "and (:from is null or a.checkedInAt >= :from) " +
                    "and (:to is null or a.checkedInAt < :to)")
    Page<Attendance> searchForAdmin(@Param("userId") Long userId,
                                    @Param("entryType") EntryType entryType,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    Pageable pageable);
}