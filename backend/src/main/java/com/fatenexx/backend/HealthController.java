package com.fatenexx.backend;

import com.fatenexx.backend.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("살아있음", "서버 정상");
    }
}