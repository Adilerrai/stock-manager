package com.acommon.config;

import com.acommon.persistant.model.CurrentRequestContext;
import com.acommon.persistant.model.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre HTTP interceptant les en-têtes personnalisés injectés par le frontend :
 * - X-Exercice-Year : Année de l'exercice comptable sélectionné (ex: 2025)
 * - X-Exercice-Id : ID technique de l'exercice comptable
 * - X-Societe-Id / X-Tenant-Id : ID de la société / tenant actif
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class ExerciceContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String yearHeader = request.getHeader("X-Exercice-Year");
            String idHeader = request.getHeader("X-Exercice-Id");
            String societeHeader = request.getHeader("X-Societe-Id");
            if (societeHeader == null || societeHeader.isBlank()) {
                societeHeader = request.getHeader("X-Tenant-Id");
            }

            Long exerciceId = null;
            Integer exerciceYear = null;

            if (idHeader != null && !idHeader.isBlank()) {
                try {
                    exerciceId = Long.parseLong(idHeader.trim());
                } catch (NumberFormatException ignored) {}
            }

            if (yearHeader != null && !yearHeader.isBlank()) {
                try {
                    exerciceYear = Integer.parseInt(yearHeader.trim());
                } catch (NumberFormatException ignored) {}
            }

            if (exerciceId != null || exerciceYear != null) {
                CurrentRequestContext.setExercice(exerciceId, exerciceYear);
            }

            if (societeHeader != null && !societeHeader.isBlank()) {
                try {
                    Long societeId = Long.parseLong(societeHeader.trim());
                    CurrentRequestContext.setSocieteId(societeId);
                    TenantContext.setCurrentTenant(societeId);
                } catch (NumberFormatException ignored) {}
            }

            filterChain.doFilter(request, response);
        } finally {
            CurrentRequestContext.clear();
        }
    }
}
