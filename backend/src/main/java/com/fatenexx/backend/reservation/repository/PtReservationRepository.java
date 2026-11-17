package com.fatenexx.backend.reservation.repository;

import com.fatenexx.backend.reservation.entity.PtReservation;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.fatenexx.backend.product.entity.PtType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PtReservationRepository extends JpaRepository<PtReservation, Long> {

    // 트레이너의 특정 상태 예약 중 date 이후 것
    List<PtReservation> findByTrainerIdAndStatusAndLessonDateGreaterThanEqual(
            Long trainerId, ReservationStatus status, LocalDate date);

    boolean existsByTrainerIdAndLessonDateAndStatus(Long trainerId, LocalDate lessonDate, ReservationStatus status);

    // 그날의 개인 PT 예약 (특정 상태 제외)
    List<PtReservation> findByTrainerIdAndLessonDateAndGroupClassIsNullAndStatusNot(
            Long trainerId, LocalDate lessonDate, ReservationStatus status);

    // 여러 그룹 수업의 참여자를 회원 정보까지 한 번에 (특정 상태 제외)
    @Query("select r from PtReservation r join fetch r.user " +
            "where r.groupClass.id in :classIds and r.status <> :status order by r.id")
    List<PtReservation> findWithUserByGroupClassIdIn(@Param("classIds") Collection<Long> classIds,
                                                     @Param("status") ReservationStatus status);

    // 수업의 참여자 수 (특정 상태 제외)
    long countByGroupClassIdAndStatusNot(Long groupClassId, ReservationStatus status);

    // 수업에 예약 기록이 하나라도 있는지 (취소 포함)
    boolean existsByGroupClassId(Long groupClassId);

    // 수업의 특정 상태 예약들
    List<PtReservation> findByGroupClassIdAndStatus(Long groupClassId, ReservationStatus status);

    // 이용권별 확보된 횟수 (예약·결과대기)
    long countByPtPassIdAndStatusIn(Long ptPassId, Collection<ReservationStatus> statuses);

    // 수업별 참여자 수 (특정 상태 제외)
    @Query("select r.groupClass.id, count(r) from PtReservation r " +
            "where r.groupClass.id in :classIds and r.status <> :status group by r.groupClass.id")
    List<Object[]> countByGroupClassIds(@Param("classIds") Collection<Long> classIds,
                                        @Param("status") ReservationStatus status);

    // 회원의 기간 내 예약 전부 (취소 포함)
    List<PtReservation> findByUserIdAndLessonDateBetween(Long userId, LocalDate start, LocalDate end);

    // 내 예약 목록 (조건 null이면 안 걸음)
    @Query("select r from PtReservation r join fetch r.ptPass p join fetch r.trainer left join fetch r.groupClass " +
            "where r.user.id = :userId " +
            "and (:status is null or r.status = :status) " +
            "and (:ptType is null or p.ptType = :ptType) " +
            "order by r.lessonDate desc, r.startTime desc, r.id desc")
    List<PtReservation> findMine(@Param("userId") Long userId,
                                 @Param("status") ReservationStatus status,
                                 @Param("ptType") PtType ptType);

    // 취소·출석 처리용: 예약 행 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PtReservation r where r.id = :id")
    Optional<PtReservation> findByIdForUpdate(@Param("id") Long id);

    // 트레이너 수업 일정: 날짜·상태 조건은 null이면 안 걸음
    @Query("select r from PtReservation r join fetch r.user left join fetch r.groupClass " +
            "where r.trainer.id = :trainerId " +
            "and (:start is null or r.lessonDate >= :start) " +
            "and (:end is null or r.lessonDate <= :end) " +
            "and (:status is null or r.status = :status) " +
            "order by r.lessonDate, r.startTime, r.id")
    List<PtReservation> findLessons(@Param("trainerId") Long trainerId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end,
                                    @Param("status") ReservationStatus status);

    // 관리자 예약 목록 (조건 null이면 안 걸음, 페이징)
    @Query(value = "select r from PtReservation r join fetch r.user join fetch r.trainer " +
            "join fetch r.ptPass p left join fetch r.groupClass " +
            "where (:userId is null or r.user.id = :userId) " +
            "and (:trainerId is null or r.trainer.id = :trainerId) " +
            "and (:ptType is null or p.ptType = :ptType) " +
            "and (:status is null or r.status = :status) " +
            "and (:start is null or r.lessonDate >= :start) " +
            "and (:end is null or r.lessonDate <= :end) " +
            "order by r.lessonDate desc, r.startTime desc, r.id desc",
            countQuery = "select count(r) from PtReservation r join r.ptPass p " +
                    "where (:userId is null or r.user.id = :userId) " +
                    "and (:trainerId is null or r.trainer.id = :trainerId) " +
                    "and (:ptType is null or p.ptType = :ptType) " +
                    "and (:status is null or r.status = :status) " +
                    "and (:start is null or r.lessonDate >= :start) " +
                    "and (:end is null or r.lessonDate <= :end)")
    Page<PtReservation> searchForAdmin(@Param("userId") Long userId,
                                       @Param("trainerId") Long trainerId,
                                       @Param("ptType") PtType ptType,
                                       @Param("status") ReservationStatus status,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end,
                                       Pageable pageable);

}