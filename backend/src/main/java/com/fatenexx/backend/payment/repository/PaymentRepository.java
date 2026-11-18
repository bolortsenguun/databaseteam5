package com.fatenexx.backend.payment.repository;

import com.fatenexx.backend.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    @Query(value = "select p from Payment p join fetch p.user",
            countQuery = "select count(p) from Payment p")
    Page<Payment> findAllWithUser(Pageable pageable);
}