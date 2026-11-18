package com.fatenexx.backend.payment.repository;

import com.fatenexx.backend.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.fatenexx.backend.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    @Query(value = "select p from Payment p join fetch p.user",
            countQuery = "select count(p) from Payment p")
    Page<Payment> findAllWithUser(Pageable pageable);

    // 관리자 결제 검색: 조건이 null이면 그 조건은 무시. 기간은 from 이상 ~ to 미만
    @Query(value = "select p from Payment p join fetch p.user u " +
            "where (:userId is null or u.id = :userId) " +
            "and (:status is null or p.status = :status) " +
            "and (:from is null or p.paidAt >= :from) " +
            "and (:to is null or p.paidAt < :to)",
            countQuery = "select count(p) from Payment p " +
                    "where (:userId is null or p.user.id = :userId) " +
                    "and (:status is null or p.status = :status) " +
                    "and (:from is null or p.paidAt >= :from) " +
                    "and (:to is null or p.paidAt < :to)")
    Page<Payment> searchForAdmin(@Param("userId") Long userId,
                                 @Param("status") PaymentStatus status,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 Pageable pageable);
}