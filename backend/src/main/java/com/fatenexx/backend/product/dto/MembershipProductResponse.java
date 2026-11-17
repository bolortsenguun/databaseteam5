package com.fatenexx.backend.product.dto;

import com.fatenexx.backend.product.entity.MembershipProduct;

public record MembershipProductResponse(Long productId, String name, int months, int price, boolean active) {
    public static MembershipProductResponse from(MembershipProduct p) {
        return new MembershipProductResponse(p.getId(), p.getName(), p.getMonths(), p.getPrice(), p.isActive());
    }
}