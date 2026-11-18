package com.fatenexx.backend.pass.dto;

import java.util.List;

public record ClientListResponse(
        int totalClients,
        List<ClientResponse> clients
) {
}
