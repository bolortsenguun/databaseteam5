package com.fatenexx.backend.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MembershipProductCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Positive Integer months,
        @NotNull @PositiveOrZero Integer price
) {}