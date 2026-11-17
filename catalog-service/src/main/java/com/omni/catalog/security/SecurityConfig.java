/**
 * Cấu hình bảo mật Spring Security (Stateless JWT) cho Catalog Service.
 * Mở quyền truy cập công khai (chỉ phương thức GET) cho các API duyệt/tìm kiếm sản phẩm,
 * danh mục, thương hiệu và hình ảnh; yêu cầu xác thực JWT cho toàn bộ thao tác ghi/quản trị.
 */
package com.omni.catalog.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/catalog/products/**",
                                "/api/v1/catalog/categories/**",
                                "/api/v1/catalog/brands/**",
                                "/api/v1/catalog/images/**"
                        ).permitAll()
                        .requestMatchers(
                                "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}