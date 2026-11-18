package com.fatenexx.backend.pass.repository;

import com.fatenexx.backend.pass.entity.PtPass;
import com.fatenexx.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PtPassRepository extends JpaRepository<PtPass, Long> {

    @EntityGraph(attributePaths = "product")
    List<PtPass> findByPaymentIn(Collection<Payment> payments);
}