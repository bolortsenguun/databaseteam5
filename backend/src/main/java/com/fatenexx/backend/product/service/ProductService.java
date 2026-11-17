package com.fatenexx.backend.product.service;

import com.fatenexx.backend.global.exception.BusinessException;
import com.fatenexx.backend.global.exception.ErrorCode;
import com.fatenexx.backend.product.dto.*;
import com.fatenexx.backend.product.entity.MembershipProduct;
import com.fatenexx.backend.product.entity.PtProduct;
import com.fatenexx.backend.product.repository.MembershipProductRepository;
import com.fatenexx.backend.product.repository.PtProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final MembershipProductRepository membershipProductRepository;
    private final PtProductRepository ptProductRepository;

    // 회원용: 판매 중인 상품만
    public ProductListResponse getActiveProducts() {
        return new ProductListResponse(
                membershipProductRepository.findByActiveTrueOrderByMonthsAsc()
                        .stream().map(MembershipProductResponse::from).toList(),
                ptProductRepository.findByActiveTrueOrderByIdAsc()
                        .stream().map(PtProductResponse::from).toList());
    }

    // 관리자용: 판매 중지 상품까지 전부
    public ProductListResponse getAllProducts() {
        return new ProductListResponse(
                membershipProductRepository.findAllByOrderByMonthsAsc()
                        .stream().map(MembershipProductResponse::from).toList(),
                ptProductRepository.findAllByOrderByIdAsc()
                        .stream().map(PtProductResponse::from).toList());
    }

    @Transactional
    public MembershipProductResponse createMembershipProduct(MembershipProductCreateRequest req) {
        MembershipProduct p = MembershipProduct.create(req.name(), req.months(), req.price());
        return MembershipProductResponse.from(membershipProductRepository.save(p));
    }

    // 가격을 바꿔도 이미 산 이용권에는 영향 없음 (구매 당시 가격을 이용권에 복사해 저장하기 때문)
    @Transactional
    public MembershipProductResponse updateMembershipProduct(Long id, MembershipProductUpdateRequest req) {
        MembershipProduct p = membershipProductRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        p.update(req.name() != null ? req.name() : p.getName(),
                req.months() != null ? req.months() : p.getMonths(),
                req.price() != null ? req.price() : p.getPrice(),
                req.active() != null ? req.active() : p.isActive());
        return MembershipProductResponse.from(p);
    }

    @Transactional
    public PtProductResponse createPtProduct(PtProductCreateRequest req) {
        PtProduct p = PtProduct.create(req.ptType(), req.name(), req.totalCount(), req.validWeeks(), req.price());
        return PtProductResponse.from(ptProductRepository.save(p));
    }

    @Transactional
    public PtProductResponse updatePtProduct(Long id, PtProductUpdateRequest req) {
        PtProduct p = ptProductRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        p.update(req.name() != null ? req.name() : p.getName(),
                req.totalCount() != null ? req.totalCount() : p.getTotalCount(),
                req.validWeeks() != null ? req.validWeeks() : p.getValidWeeks(),
                req.price() != null ? req.price() : p.getPrice(),
                req.active() != null ? req.active() : p.isActive());
        return PtProductResponse.from(p);
    }
}