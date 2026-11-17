package com.fatenexx.backend.auth.dto;

public record ResetPasswordResponse(Long userId, String temporaryPassword) {}