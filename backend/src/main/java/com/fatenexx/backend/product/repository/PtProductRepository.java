package com.fatenexx.backend.product.repository;

import com.fatenexx.backend.product.entity.PtProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PtProductRepository extends JpaRepository<PtProduct, Long> {
    List<PtProduct> findByActiveTrueOrderByIdAsc();
    List<PtProduct> findAllByOrderByIdAsc();
}
