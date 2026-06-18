package com.sheemab.shardedsagawallet.services.security;


import com.sheemab.shardedsagawallet.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    // OncePerRequestFilter = ek request mein sirf ONCE chalega

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ── Step 1: Authorization header nikalo ───────────────────────────────
        String authHeader = request.getHeader("Authorization");

        // Header hona chahiye aur "Bearer " se start hona chahiye
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Token nahi hai — aage bhejo (SecurityConfig decide karega allow/reject)
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Token extract karo ────────────────────────────────────────
        // "Bearer eyJhbGc..." → "eyJhbGc..."
        String token = authHeader.substring(7);

        // ── Step 3: Email nikalo token se ─────────────────────────────────────
        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            log.warn("[JwtFilter] Could not extract email from token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 4: Agar email hai aur already authenticated nahi hai ─────────
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // DB se user load karo
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Token valid hai?
            if (jwtService.isTokenValid(token, userDetails.getUsername())) {

                // ── Step 5: SecurityContext mein set karo ─────────────────────
                // Yahi "authentication" hai Spring Security ke liye
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // credentials — JWT ke baad zaroorat nahi
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("[JwtFilter] Authenticated user: {}", email);
            }
        }

        // ── Step 6: Aage bhejo (controller tak) ──────────────────────────────
        filterChain.doFilter(request, response);
    }
}