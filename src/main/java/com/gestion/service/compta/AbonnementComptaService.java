package com.gestion.service.compta;

import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.User;
import com.gestion.persistent.dto.compta.LivreDtos.AbonnementDTO;
import com.gestion.persistent.dto.compta.LivreDtos.DossierAbonnementDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.AbonnementRepository;
import com.gestion.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AbonnementComptaService {

    private final AbonnementRepository repository;
    private final LivreCourantService livreCourant;
    private final AuditService audit;

    public AbonnementComptaService(AbonnementRepository repository, LivreCourantService livreCourant, AuditService audit) {
        this.repository = repository;
        this.livreCourant = livreCourant;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AbonnementDTO lire(Long pointDeVenteId) {
        exigerPointDeVente(pointDeVenteId);
        return repository.lire(pointDeVenteId).orElse(new AbonnementDTO(pointDeVenteId, false, null, null, null));
    }

    @Transactional
    public AbonnementDTO modifier(Long pointDeVenteId, boolean actif) {
        exigerPointDeVente(pointDeVenteId);
        repository.enregistrer(pointDeVenteId, actif, livreCourant.auteur());
        audit.logAction(ActionAudit.MODIFICATION, "compta_abonnements", pointDeVenteId, actif ? "Abonnement activé" : "Abonnement coupé");
        return lire(pointDeVenteId);
    }

    // --- Ce que voit et change l'utilisateur connecté

    @Transactional(readOnly = true)
    public List<DossierAbonnementDTO> dossiersVisibles() {
        User u = utilisateur();
        return repository.dossiers(u == null ? null : u.getPointDeVenteId(), u == null ? null : u.getTenantId(), estSuperAdmin());
    }

    @Transactional(readOnly = true)
    public AbonnementDTO lireVisible(Long pointDeVenteId) {
        exigerVisible(pointDeVenteId);
        return lire(pointDeVenteId);
    }

    @Transactional
    public AbonnementDTO modifierVisible(Long pointDeVenteId, boolean actif) {
        exigerVisible(pointDeVenteId);
        return modifier(pointDeVenteId, actif);
    }

    /** Le dossier d'un autre tenant répond comme un dossier inconnu. */
    private void exigerVisible(Long pointDeVenteId) {
        if (estSuperAdmin()) {
            return;
        }
        if (dossiersVisibles().stream().noneMatch(d -> d.pointDeVenteId().equals(pointDeVenteId))) {
            throw new ResourceNotFoundException("Point de vente", "id", pointDeVenteId);
        }
    }

    private static boolean estSuperAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(x -> "ROLE_SUPERADMIN".equals(x.getAuthority()));
    }

    private static User utilisateur() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof User u ? u : null;
    }

    private void exigerPointDeVente(Long pointDeVenteId) {
        if (!repository.pointDeVenteExiste(pointDeVenteId)) {
            throw new ResourceNotFoundException("Point de vente", "id", pointDeVenteId);
        }
    }
}
