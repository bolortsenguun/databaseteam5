package com.fatenexx.backend.trainer.dto;

public record TrainerApproveResponse(
        Long trainerId,
        String approvalStatus
) {
}