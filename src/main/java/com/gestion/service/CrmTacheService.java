package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.StatutTache;
import com.gestion.persistent.enums.TypeActiviteCrm;
import com.gestion.persistent.model.CrmTache;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CrmTacheService {

    private final CrmTacheRepository tacheRepository;
    private final UserRepository userRepository;
    private final CrmEntrepriseRepository entrepriseRepository;
    private final CrmContactRepository contactRepository;
    private final CrmOpportuniteRepository opportuniteRepository;
    private final CrmVisiteRepository visiteRepository;
    private final CrmActiviteService activiteService;

    public CrmTacheService(CrmTacheRepository tacheRepository,
                           UserRepository userRepository,
                           CrmEntrepriseRepository entrepriseRepository,
                           CrmContactRepository contactRepository,
                           CrmOpportuniteRepository opportuniteRepository,
                           CrmVisiteRepository visiteRepository,
                           CrmActiviteService activiteService) {
        this.tacheRepository = tacheRepository;
        this.userRepository = userRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.contactRepository = contactRepository;
        this.opportuniteRepository = opportuniteRepository;
        this.visiteRepository = visiteRepository;
        this.activiteService = activiteService;
    }

    public CrmTache creerTache(CrmTache tache) {
        Long tenantId = TenantContext.getCurrentTenant();
        tache.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        tache.setDateCreation(LocalDateTime.now());
        tache.setStatut(StatutTache.A_FAIRE);

        if (tache.getAssigneAUserId() != null) {
            userRepository.findById(tache.getAssigneAUserId()).ifPresent(tache::setAssigneA);
        }
        if (tache.getCreeParUserId() != null) {
            userRepository.findById(tache.getCreeParUserId()).ifPresent(tache::setCreePar);
        }
        if (tache.getEntrepriseId() != null) {
            entrepriseRepository.findByIdAndPointDeVenteId(tache.getEntrepriseId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(tache::setEntreprise);
        }
        if (tache.getContactId() != null) {
            contactRepository.findByIdAndPointDeVenteId(tache.getContactId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(tache::setContact);
        }
        if (tache.getOpportuniteId() != null) {
            opportuniteRepository.findByIdAndPointDeVenteId(tache.getOpportuniteId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(tache::setOpportunite);
        }
        if (tache.getVisiteId() != null) {
            visiteRepository.findByIdAndPointDeVenteId(tache.getVisiteId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(tache::setVisite);
        }

        CrmTache saved = tacheRepository.save(tache);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.TACHE,
                "Nouvelle tâche assignée : " + saved.getTitre(),
                "Priorité : " + saved.getPriorite() + ", Échéance : " + saved.getDateEcheance(),
                saved.getEntreprise(), saved.getContact(), saved.getOpportunite(), saved.getVisite(), saved.getAssigneA()
        );

        return saved;
    }

    public CrmTache marquerTerminee(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmTache existing = tacheRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Tâche non trouvée avec l'id : " + id));

        existing.setStatut(StatutTache.TERMINEE);
        existing.setDateRealisation(LocalDateTime.now());
        CrmTache updated = tacheRepository.save(existing);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.TACHE,
                "Tâche terminée : " + updated.getTitre(),
                "Tâche réalisée le " + updated.getDateRealisation(),
                updated.getEntreprise(), updated.getContact(), updated.getOpportunite(), updated.getVisite(), updated.getAssigneA()
        );

        return updated;
    }

    @Transactional(readOnly = true)
    public List<CrmTache> getMesTaches(Long userId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return tacheRepository.findByPointDeVenteIdAndAssigneAIdOrderByDateEcheanceAsc(
                tenantId != null ? tenantId : 1L, userId);
    }

    @Transactional(readOnly = true)
    public List<CrmTache> getTachesEnRetard() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tacheRepository.findTachesEnRetard(tenantId != null ? tenantId : 1L, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<CrmTache> getTachesByRapport(Long rapportId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return tacheRepository.findByPointDeVenteIdAndRapportId(tenantId != null ? tenantId : 1L, rapportId);
    }
}
