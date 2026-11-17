package com.fatenexx.backend.product.repository;

import com.fatenexx.backend.product.entity.MembershipProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipProductRepository extends JpaRepository<MembershipProduct, Long> {
    List<MembershipProduct> findByActiveTrueOrderByMonthsAsc();
    List<MembershipProduct> findAllByOrderByMonthsAsc();
}