package com.fatenexx.backend.group.dto;

import java.util.List;

public record MemberGroupClassListResponse(
        List<MemberGroupClassResponse> classes
) {
}