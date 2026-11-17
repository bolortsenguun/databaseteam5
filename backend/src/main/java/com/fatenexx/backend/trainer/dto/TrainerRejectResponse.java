package com.fatenexx.backend.trainer.dto;

public record TrainerRejectResponse(
        Long trainerId,
        boolean deleted
) {
}