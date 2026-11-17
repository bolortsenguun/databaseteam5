package com.fatenexx.backend.trainer.dto;

import java.util.List;

public record TrainerListResponse(
        List<TrainerSummaryResponse> trainers
) {
}
