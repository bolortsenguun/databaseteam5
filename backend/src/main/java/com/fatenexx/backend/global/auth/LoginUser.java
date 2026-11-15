package com.fatenexx.backend.global.auth;

import java.io.Serializable;

public record LoginUser(
        Long userId,
        Role role,
        boolean approved,
        boolean mustChangePassword
) implements Serializable {
}
