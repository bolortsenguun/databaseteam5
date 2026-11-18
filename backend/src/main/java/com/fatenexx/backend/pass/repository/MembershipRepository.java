package com.fatenexx.backend.pass.repository;

import com.fatenexx.backend.pass.entity.Membership;
import com.fatenexx.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Query("select max(m.endDate) from Membership m where m.user.id = :userId and m.endDate >= :today")
    LocalDate findLatestEndDate(@Param("userId") Long userId, @Param("today") LocalDate today);

    @EntityGraph(attributePaths = "product")
    List<Membership> findByPaymentIn(Collection<Payment> payments);

    // 내 회원권 목록 (최신 시작일 순)
    @EntityGraph(attributePaths = "product")
    List<Membership> findByUserIdOrderByStartDateDescIdDesc(Long userId);
}