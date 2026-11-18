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

    // 오늘 유효한 회원권이 있는지 (시작일 <= 오늘 <= 종료일). 호출: (userId, today, today)
    boolean existsByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate start, LocalDate end);

    // 관리자 회원 목록: 주어진 회원들 중 오늘 유효한 회원권이 있는 회원 id (한 번에 조회, N+1 방지)
    @Query("select distinct m.user.id from Membership m " +
            "where m.user.id in :userIds and m.startDate <= :today and m.endDate >= :today")
    List<Long> findActiveUserIds(@Param("userIds") Collection<Long> userIds,
                                 @Param("today") LocalDate today);
}