package com.fatenexx.backend.user.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record MemberListResponse(
        List<MemberSummaryResponse> members, int page, int size, long totalCount, int totalPages
) {
    // page는 1부터 (Spring Page는 0부터라 +1)
    public static MemberListResponse from(Page<MemberSummaryResponse> p) {
        return new MemberListResponse(p.getContent(), p.getNumber() + 1, p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }
}