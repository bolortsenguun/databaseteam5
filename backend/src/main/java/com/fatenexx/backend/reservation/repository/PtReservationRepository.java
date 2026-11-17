package com.fatenexx.backend.reservation.repository;

import com.fatenexx.backend.reservation.entity.PtReservation;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.time.LocalDate;
import java.util.List;

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
}