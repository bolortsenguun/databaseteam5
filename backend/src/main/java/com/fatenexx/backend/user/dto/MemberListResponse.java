package com.fatenexx.backend.user.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record MemberListResponse(
        List<MemberSummaryResponse> members, int page, int size, long totalElements, int totalPages
) {
    public static MemberListResponse from(Page<MemberSummaryResponse> p) {
        return new MemberListResponse(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }
}