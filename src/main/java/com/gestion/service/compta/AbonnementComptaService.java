package com.gestion.service.compta;

import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.LivreDtos.AbonnementDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.AbonnementRepository;
import com.gestion.service.AuditService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Long cible = resoudreEtControlerPointDeVente(pointDeVenteId, true);
        return repository.lire(cible).orElse(new AbonnementDTO(cible, false, null, null, null));
    }

    @Transactional
    public AbonnementDTO modifier(Long pointDeVenteId, boolean actif) {
        Long cible = resoudreEtControlerPointDeVente(pointDeVenteId, actif);
        repository.enregistrer(cible, actif, livreCourant.auteur());
        audit.logAction(ActionAudit.MODIFICATION, "compta_abonnements", cible, actif ? "Abonnement activé" : "Abonnement coupé", cible);
        return lire(cible);
    }

    private Long resoudreEtControlerPointDeVente(Long pointDeVenteId, boolean actif) {
        Long duContexte = livreCourant.contexte()
                .map(LivreCourantService.Contexte::pointDeVenteAbonne)
                .filter(id -> id != null && repository.pointDeVenteExiste(id))
                .orElse(null);

        Long cible = (pointDeVenteId != null && pointDeVenteId > 0 && repository.pointDeVenteExiste(pointDeVenteId))
                ? pointDeVenteId
                : duContexte;

        if (cible == null) {
            throw new ResourceNotFoundException("Point de vente", "id", pointDeVenteId);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean superAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SUPERADMIN".equalsIgnoreCase(a.getAuthority()) || "SUPERADMIN".equalsIgnoreCase(a.getAuthority()));
            if (!superAdmin) {
                if (duContexte == null || !cible.equals(duContexte) || !actif) {
                    throw new AccessDeniedException("Seul le Super Administrateur peut gérer l'abonnement d'un autre point de vente ou couper un abonnement.");
                }
            }
        }

        return cible;
    }
}
