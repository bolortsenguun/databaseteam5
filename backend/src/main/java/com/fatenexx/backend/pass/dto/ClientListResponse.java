package com.fatenexx.backend.pass.dto;

import java.util.List;

public record ClientListResponse(
        List<ClientResponse> clients
) {
}