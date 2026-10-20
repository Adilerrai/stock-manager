package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.TypeActiviteCrm;
import com.gestion.persistent.model.*;
import com.gestion.repository.CrmActiviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CrmActiviteService {

    private final CrmActiviteRepository activiteRepository;

    public CrmActiviteService(CrmActiviteRepository activiteRepository) {
        this.activiteRepository = activiteRepository;
    }

    public CrmActivite enregistrerActivite(TypeActiviteCrm type, String titre, String description,
                                          CrmEntreprise entreprise, CrmContact contact,
                                          CrmOpportunite opportunite, CrmVisite visite,
                                          User user) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmActivite activite = new CrmActivite();
        activite.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        activite.setTypeActivite(type);
        activite.setTitre(titre);
        activite.setDescription(description);
        activite.setEntreprise(entreprise);
        activite.setContact(contact);
        activite.setOpportunite(opportunite);
        activite.setVisite(visite);
        activite.setUser(user);
        activite.setDateActivite(LocalDateTime.now());
        return activiteRepository.save(activite);
    }

    @Transactional(readOnly = true)
    public List<CrmActivite> getHistoriqueEntreprise(Long entrepriseId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return activiteRepository.findByPointDeVenteIdAndEntrepriseIdOrderByDateActiviteDesc(
                tenantId != null ? tenantId : 1L, entrepriseId);
    }

    @Transactional(readOnly = true)
    public List<CrmActivite> getHistoriqueContact(Long contactId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return activiteRepository.findByPointDeVenteIdAndContactIdOrderByDateActiviteDesc(
                tenantId != null ? tenantId : 1L, contactId);
    }

    @Transactional(readOnly = true)
    public List<CrmActivite> getHistoriqueOpportunite(Long opportuniteId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return activiteRepository.findByPointDeVenteIdAndOpportuniteIdOrderByDateActiviteDesc(
                tenantId != null ? tenantId : 1L, opportuniteId);
    }

    @Transactional(readOnly = true)
    public List<CrmActivite> getTimelineGlobale() {
        Long tenantId = TenantContext.getCurrentTenant();
        return activiteRepository.findByPointDeVenteIdOrderByDateActiviteDesc(tenantId != null ? tenantId : 1L);
    }
}
