package com.acommon.config;


import com.acommon.persistant.model.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                jwt = tokenParam;
            }
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String userEmail = jwtUtil.extractUsername(jwt);
            final Long tenantId = jwtUtil.extractTenantId(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Charger l'utilisateur par email seulement
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    Long effectiveTenantId = tenantId;
                    String headerSociete = request.getHeader("X-Societe-Id");
                    if (headerSociete == null || headerSociete.isBlank()) {
                        headerSociete = request.getHeader("X-Tenant-Id");
                    }
                    if (headerSociete == null || headerSociete.isBlank()) {
                        headerSociete = request.getParameter("societeId");
                    }
                    if (headerSociete != null && !headerSociete.isBlank()) {
                        try {
                            effectiveTenantId = Long.parseLong(headerSociete.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    if (effectiveTenantId == null && userDetails instanceof com.acommon.persistant.model.User u) {
                        effectiveTenantId = u.getTenantId() != null ? u.getTenantId() : u.getPointDeVenteId();
                    }
                    // Définir le contexte du tenant
                    TenantContext.setCurrentTenant(effectiveTenantId);
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Session expirée, veuillez vous reconnecter.\"}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Token invalide.\"}");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}