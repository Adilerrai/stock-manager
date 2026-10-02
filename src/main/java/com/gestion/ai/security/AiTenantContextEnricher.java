package com.gestion.ai.security;

import com.acommon.persistant.model.CurrentRequestContext;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.model.Societe;
import com.gestion.repository.SocieteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class AiTenantContextEnricher {

    private static final Logger log = LoggerFactory.getLogger(AiTenantContextEnricher.class);

    private final UserRepository userRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final SocieteRepository societeRepository;

    public AiTenantContextEnricher(UserRepository userRepository,
                                   PointDeVenteRepository pointDeVenteRepository,
                                   SocieteRepository societeRepository) {
        this.userRepository = userRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.societeRepository = societeRepository;
    }

    /**
     * Résout l'utilisateur connecté et déduit l'arbre de visibilité tenant (Holding vs Filiale / Société Active)
     */
    public AiTenantScope resolveCurrentScope() {
        User currentUser = resolveCurrentUser();
        Long mereId = null;
        String roleName = "UTILISATEUR";
        String nomEntreprise = null;

        if (currentUser != null) {
            mereId = currentUser.getMereId();
            if (currentUser.getRole() != null) {
                roleName = currentUser.getRole().getNom();
            }
        }

        // 1. PRIORITÉ ABSOLUE : La société active envoyée par le frontend (X-Societe-Id ou TenantContext)
        Long activeTenantId = CurrentRequestContext.getSocieteId();
        if (activeTenantId == null) {
            activeTenantId = TenantContext.getCurrentTenant();
        }
        if (activeTenantId == null && currentUser != null) {
            activeTenantId = currentUser.getPointDeVenteId() != null 
                    ? currentUser.getPointDeVenteId() 
                    : currentUser.getTenantId();
        }
        if (activeTenantId == null) {
            activeTenantId = 1L; // Fallback par défaut
        }

        // 2. Recherche du profil légal dans Societe
        Societe societe = societeRepository.findById(activeTenantId).orElse(null);
        if (societe != null) {
            nomEntreprise = societe.getRaisonSociale() + " (" + societe.getCode() + ")";
            if (mereId == null) {
                mereId = societe.getMereId();
            }
        }

        // 2b. Recherche dans PointDeVente si non trouvé dans Societe
        if (nomEntreprise == null) {
            final Long pdvLookupId = activeTenantId;
            PointDeVente pdv = pointDeVenteRepository.findById(pdvLookupId)
                    .or(() -> pointDeVenteRepository.findByTenantId(pdvLookupId))
                    .orElse(null);
            if (pdv != null) {
                nomEntreprise = pdv.getNomPointDeVente() != null ? pdv.getNomPointDeVente() : pdv.getNom();
                if (mereId == null) {
                    mereId = pdv.getMereId();
                }
            }
        }

        if (nomEntreprise == null) {
            nomEntreprise = "Société #" + activeTenantId;
        }

        // 3. Détermination de l'arbre accessible
        boolean isHolding = (mereId == null);
        Set<Long> accessibleTenantIds = new HashSet<>();
        accessibleTenantIds.add(activeTenantId);

        if (mereId != null) {
            // Société fille / filiale : ajouter la mère et les sociétés sœurs du même cabinet
            accessibleTenantIds.add(mereId);
            List<Societe> soeurs = societeRepository.findByMereIdAndActifTrueOrderByRaisonSocialeAsc(mereId);
            for (Societe s : soeurs) {
                accessibleTenantIds.add(s.getId());
            }
        } else {
            // Cabinet mère / Holding : accès consolidé à toutes les sociétés filles
            accessibleTenantIds.add(activeTenantId);
            List<Societe> filiales = societeRepository.findByMereIdAndActifTrueOrderByRaisonSocialeAsc(activeTenantId);
            for (Societe s : filiales) {
                accessibleTenantIds.add(s.getId());
            }
            List<Long> pdvIds = pointDeVenteRepository.findTenantIdsByMereId(activeTenantId);
            if (pdvIds != null) {
                accessibleTenantIds.addAll(pdvIds);
            }
        }

        log.debug("AiTenantScope résolu: activeTenantId={}, mereId={}, isHolding={}, accessibleTenants={}",
                activeTenantId, mereId, isHolding, accessibleTenantIds);

        return new AiTenantScope(
                currentUser,
                activeTenantId,
                mereId,
                isHolding,
                accessibleTenantIds,
                roleName,
                nomEntreprise
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
