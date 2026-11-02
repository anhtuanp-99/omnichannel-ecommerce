package com.omni.identity.config;

import com.omni.common.web.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Đăng ký common-web filter với servlet container. */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        var reg = new FilterRegistrationBean<>(new CorrelationIdFilter());
        reg.setOrder(1);
        reg.addUrlPatterns("/*");
        return reg;
    }
}