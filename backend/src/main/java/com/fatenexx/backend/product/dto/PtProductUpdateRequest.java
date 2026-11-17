package com.fatenexx.backend.product.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PtProductUpdateRequest(
        @Size(min = 1, max = 50) String name,
        @Positive Integer totalCount,
        @Positive Integer validWeeks,
        @PositiveOrZero Integer price,
        Boolean active
) {}