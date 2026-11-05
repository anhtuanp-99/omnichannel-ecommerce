package com.omni.identity.security;

import java.lang.annotation.*;

/** Inject UUID userId (từ JWT principal) vào controller method parameter. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {}