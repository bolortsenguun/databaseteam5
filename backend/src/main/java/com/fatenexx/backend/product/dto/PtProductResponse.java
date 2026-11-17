package com.fatenexx.backend.product.dto;

import com.fatenexx.backend.product.entity.PtProduct;
import com.fatenexx.backend.product.entity.PtType;

public record PtProductResponse(Long productId, PtType ptType, String name,
                                int totalCount, int validWeeks, int price, boolean active) {
    public static PtProductResponse from(PtProduct p) {
        return new PtProductResponse(p.getId(), p.getPtType(), p.getName(),
                p.getTotalCount(), p.getValidWeeks(), p.getPrice(), p.isActive());
    }
}