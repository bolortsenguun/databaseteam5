package com.fatenexx.backend.reservation.repository;

import com.fatenexx.backend.reservation.entity.PtReservation;
import com.fatenexx.backend.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PtReservationRepository extends JpaRepository<PtReservation, Long> {

    // 트레이너의 특정 상태 예약 중 date 이후 것
    List<PtReservation> findByTrainerIdAndStatusAndLessonDateGreaterThanEqual(
            Long trainerId, ReservationStatus status, LocalDate date);

    boolean existsByTrainerIdAndLessonDateAndStatus(Long trainerId, LocalDate lessonDate, ReservationStatus status);
}