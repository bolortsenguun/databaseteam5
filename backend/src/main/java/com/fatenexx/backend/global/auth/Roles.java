package com.fatenexx.backend.global.auth;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Roles {
    Role[] value();
}