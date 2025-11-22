package com.acommon.service;

import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PointDeVenteSecurityService {

    private final PointDeVenteRepository pointDeVenteRepository;

    public PointDeVenteSecurityService(PointDeVenteRepository pointDeVenteRepository) {
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    public boolean isPointDeVenteManager(Long pointDeVenteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        User user = (User) auth.getPrincipal();
        return user.getPointDeVente() != null && user.getPointDeVente().getId().equals(pointDeVenteId);
    }

    public boolean isCurrentPointDeVenteManager() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return false;
        }

        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId).orElse(null);
        if (pointDeVente == null) {
            return false;
        }

        return isPointDeVenteManager(pointDeVente.getId());
    }
}