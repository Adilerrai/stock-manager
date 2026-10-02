package com.gestion.ai.security;

import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AiTenantContextEnricher {

    private static final Logger log = LoggerFactory.getLogger(AiTenantContextEnricher.class);

    private final UserRepository userRepository;
    private final PointDeVenteRepository pointDeVenteRepository;

    public AiTenantContextEnricher(UserRepository userRepository,
                                   PointDeVenteRepository pointDeVenteRepository) {
        this.userRepository = userRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    /**
     * Résout l'utilisateur connecté et déduit l'arbre de visibilité tenant (Holding vs Filiale)
     */
    public AiTenantScope resolveCurrentScope() {
        User currentUser = resolveCurrentUser();
        Long tenantId = null;
        Long mereId = null;
        String roleName = "UTILISATEUR";
        String nomEntreprise = "Point de Vente";

        if (currentUser != null) {
            tenantId = currentUser.getTenantId() != null ? currentUser.getTenantId() : currentUser.getPointDeVenteId();
            mereId = currentUser.getMereId();
            if (currentUser.getRole() != null) {
                roleName = currentUser.getRole().getNom();
            }
        }

        if (tenantId == null) {
            tenantId = TenantContext.getCurrentTenant();
        }

        if (tenantId == null) {
            tenantId = 1L; // Fallback par défaut sécurisé
        }

        PointDeVente pdv = pointDeVenteRepository.findByTenantId(tenantId).orElse(null);
        if (pdv != null) {
            nomEntreprise = pdv.getNomPointDeVente() != null ? pdv.getNomPointDeVente() : pdv.getNom();
            if (mereId == null) {
                mereId = pdv.getMereId();
            }
        }

        // Détection de l'arbre : si mereId est null, l'entreprise est la holding ou une société autonome
        boolean isHolding = (mereId == null);
        Set<Long> accessibleTenantIds = new HashSet<>();
        accessibleTenantIds.add(tenantId);

        if (isHolding) {
            // La société mère a visibilité sur ses filiales
            List<Long> filialeTenantIds = pointDeVenteRepository.findTenantIdsByMereId(tenantId);
            if (filialeTenantIds != null && !filialeTenantIds.isEmpty()) {
                accessibleTenantIds.addAll(filialeTenantIds);
            }
        }

        log.debug("AiTenantScope résolu: tenantId={}, mereId={}, isHolding={}, accessibleTenants={}",
                tenantId, mereId, isHolding, accessibleTenantIds);

        return new AiTenantScope(
                currentUser,
                tenantId,
                mereId,
                isHolding,
                accessibleTenantIds,
                roleName,
                nomEntreprise != null ? nomEntreprise : "Entreprise " + tenantId
        );
    }

    private User resolveCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
                return null;
            }
            if (auth.getPrincipal() instanceof User u) {
                return u;
            }
            return userRepository.findByEmail(auth.getName())
                    .or(() -> userRepository.findByUsername(auth.getName()))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Impossible de résoudre l'utilisateur connecté: {}", e.getMessage());
            return null;
        }
    }
}
