package com.fatenexx.backend.group.dto;

import java.util.List;

public record TrainerGroupClassListResponse(
        List<TrainerGroupClassResponse> classes
) {
}