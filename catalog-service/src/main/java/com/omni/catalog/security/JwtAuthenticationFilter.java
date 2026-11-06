package com.omni.catalog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtParserService parser;

    public JwtAuthenticationFilter(JwtParserService parser) { this.parser = parser; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        try {
            Claims c = parser.parse(header.substring(7));
            UUID userId = UUID.fromString(c.getSubject());
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) c.get("roles", List.class);
            var authorities = roles == null ? List.<SimpleGrantedAuthority>of()
                    : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(req, res);
    }
}