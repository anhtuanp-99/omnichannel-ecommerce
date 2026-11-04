package com.omni.identity.config;

import com.omni.identity.security.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Đăng ký @CurrentUser argument resolver. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver resolver;

    public WebConfig(CurrentUserArgumentResolver resolver) { this.resolver = resolver; }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}