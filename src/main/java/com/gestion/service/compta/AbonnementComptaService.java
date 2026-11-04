package com.gestion.service.compta;

import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.LivreDtos.AbonnementDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.AbonnementRepository;
import com.gestion.service.AuditService;
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

    private void exigerPointDeVente(Long pointDeVenteId) {
        if (!repository.pointDeVenteExiste(pointDeVenteId)) {
            throw new ResourceNotFoundException("Point de vente", "id", pointDeVenteId);
        }
    }
}
