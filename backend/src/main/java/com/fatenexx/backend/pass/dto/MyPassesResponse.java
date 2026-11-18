package com.fatenexx.backend.pass.dto;

import java.util.List;

public record MyPassesResponse(
        List<MembershipItemResponse> memberships,
        List<PtPassItemResponse> ptPasses
) {
}