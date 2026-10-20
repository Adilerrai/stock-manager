package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.StatutVisite;
import com.gestion.persistent.enums.TypeActiviteCrm;
import com.gestion.persistent.model.CrmVisite;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CrmVisiteService {

    private final CrmVisiteRepository visiteRepository;
    private final CrmRapportVisiteRepository rapportVisiteRepository;
    private final CrmEntrepriseRepository entrepriseRepository;
    private final CrmContactRepository contactRepository;
    private final CrmOpportuniteRepository opportuniteRepository;
    private final UserRepository userRepository;
    private final CrmActiviteService activiteService;

    public CrmVisiteService(CrmVisiteRepository visiteRepository,
                            CrmRapportVisiteRepository rapportVisiteRepository,
                            CrmEntrepriseRepository entrepriseRepository,
                            CrmContactRepository contactRepository,
                            CrmOpportuniteRepository opportuniteRepository,
                            UserRepository userRepository,
                            CrmActiviteService activiteService) {
        this.visiteRepository = visiteRepository;
        this.rapportVisiteRepository = rapportVisiteRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.contactRepository = contactRepository;
        this.opportuniteRepository = opportuniteRepository;
        this.userRepository = userRepository;
        this.activiteService = activiteService;
    }

    public CrmVisite planifierVisite(CrmVisite visite) {
        Long tenantId = TenantContext.getCurrentTenant();
        visite.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        visite.setDateCreation(LocalDateTime.now());
        visite.setStatut(StatutVisite.PLANIFIEE);

        if (visite.getCommercialId() != null) {
            userRepository.findById(visite.getCommercialId())
                    .ifPresent(visite::setCommercial);
        } else if (visite.getCommercial() == null) {
            throw new IllegalArgumentException("Le commercial assigné à la visite est obligatoire.");
        }

        if (visite.getEntrepriseId() != null) {
            entrepriseRepository.findByIdAndPointDeVenteId(visite.getEntrepriseId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(visite::setEntreprise);
        }

        if (visite.getContactId() != null) {
            contactRepository.findByIdAndPointDeVenteId(visite.getContactId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(visite::setContact);
        }

        if (visite.getOpportuniteId() != null) {
            opportuniteRepository.findByIdAndPointDeVenteId(visite.getOpportuniteId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(visite::setOpportunite);
        }

        CrmVisite saved = visiteRepository.save(visite);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.VISITE,
                "Planification d'une visite : " + saved.getTypeVisite(),
                "Visite prévue le " + saved.getDateVisite() + " à " + (saved.getLieu() != null ? saved.getLieu() : "lieu non spécifié"),
                saved.getEntreprise(), saved.getContact(), saved.getOpportunite(), saved, saved.getCommercial()
        );

        return saved;
    }

    public CrmVisite marquerRealisee(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmVisite existing = visiteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Visite non trouvée avec l'id : " + id));

        if (existing.getStatut() == StatutVisite.ANNULEE) {
            throw new IllegalStateException("Impossible de marquer comme réalisée une visite annulée.");
        }

        existing.setStatut(StatutVisite.REALISEE);
        CrmVisite updated = visiteRepository.save(existing);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.VISITE,
                "Visite terrain réalisée",
                "Visite effectuée. En attente de la rédaction du rapport.",
                updated.getEntreprise(), updated.getContact(), updated.getOpportunite(), updated, updated.getCommercial()
        );

        return updated;
    }

    public CrmVisite cloturerVisite(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmVisite existing = visiteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Visite non trouvée avec l'id : " + id));

        // Règle métier : Un rapport de visite est obligatoire pour clôturer la visite
        boolean hasRapport = rapportVisiteRepository.findByVisiteId(id).isPresent();
        if (!hasRapport) {
            throw new IllegalStateException("Impossible de clôturer la visite : un rapport de visite rédigé et soumis est obligatoire.");
        }

        existing.setStatut(StatutVisite.CLOTUREE);
        CrmVisite updated = visiteRepository.save(existing);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.VISITE,
                "Clôture de la visite commerciale",
                "La visite a été clôturée avec succès suite à la soumission du rapport.",
                updated.getEntreprise(), updated.getContact(), updated.getOpportunite(), updated, updated.getCommercial()
        );

        return updated;
    }

    public CrmVisite annulerVisite(Long id, String motif) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmVisite existing = visiteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Visite non trouvée avec l'id : " + id));

        // Règle métier : Une visite annulée exige obligatoirement un motif
        if (motif == null || motif.trim().isEmpty()) {
            throw new IllegalArgumentException("Un motif d'annulation est obligatoire pour annuler une visite.");
        }

        existing.setStatut(StatutVisite.ANNULEE);
        existing.setMotifAnnulation(motif.trim());
        CrmVisite updated = visiteRepository.save(existing);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.VISITE,
                "Annulation de la visite",
                "Motif d'annulation : " + motif.trim(),
                updated.getEntreprise(), updated.getContact(), updated.getOpportunite(), updated, updated.getCommercial()
        );

        return updated;
    }

    @Transactional(readOnly = true)
    public List<CrmVisite> getAllVisites() {
        Long tenantId = TenantContext.getCurrentTenant();
        return visiteRepository.findByPointDeVenteIdOrderByDateVisiteDesc(tenantId != null ? tenantId : 1L);
    }

    @Transactional(readOnly = true)
    public List<CrmVisite> getVisitesByCommercial(Long commercialId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return visiteRepository.findByPointDeVenteIdAndCommercialIdOrderByDateVisiteDesc(
                tenantId != null ? tenantId : 1L, commercialId);
    }

    @Transactional(readOnly = true)
    public List<CrmVisite> getVisitesByEntreprise(Long entrepriseId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return visiteRepository.findByPointDeVenteIdAndEntrepriseIdOrderByDateVisiteDesc(
                tenantId != null ? tenantId : 1L, entrepriseId);
    }

    @Transactional(readOnly = true)
    public CrmVisite getVisiteById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return visiteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Visite non trouvée avec l'id : " + id));
    }
}
