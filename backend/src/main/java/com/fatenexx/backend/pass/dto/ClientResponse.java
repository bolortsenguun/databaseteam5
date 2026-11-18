package com.fatenexx.backend.pass.dto;

import java.time.LocalDate;
import java.util.List;

public record ClientResponse(
        Long userId,
        String name,
        String phone,
        LocalDate birthDate,
        String gender,
        List<ClientPassResponse> ptPasses
) {
}
