package com.fatenexx.backend.product.dto;

import com.fatenexx.backend.product.entity.PtType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PtProductCreateRequest(
        @NotNull PtType ptType,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Positive Integer totalCount,
        @NotNull @Positive Integer validWeeks,
        @NotNull @PositiveOrZero Integer price
) {}