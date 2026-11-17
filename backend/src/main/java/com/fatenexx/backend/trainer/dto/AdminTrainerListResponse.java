package com.fatenexx.backend.trainer.dto;

import java.util.List;

public record AdminTrainerListResponse(
        List<AdminTrainerResponse> trainers,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}