package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.CrmDashboardKPIsDTO;
import com.gestion.persistent.enums.EtapeOpportunite;
import com.gestion.persistent.enums.StatutRapportVisite;
import com.gestion.persistent.enums.StatutTache;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

@Service
@Transactional(readOnly = true)
public class CrmDashboardService {

    private final CrmEntrepriseRepository entrepriseRepository;
    private final CrmContactRepository contactRepository;
    private final CrmOpportuniteRepository opportuniteRepository;
    private final CrmVisiteRepository visiteRepository;
    private final CrmRapportVisiteRepository rapportVisiteRepository;
    private final CrmTacheRepository tacheRepository;

    public CrmDashboardService(CrmEntrepriseRepository entrepriseRepository,
                               CrmContactRepository contactRepository,
                               CrmOpportuniteRepository opportuniteRepository,
                               CrmVisiteRepository visiteRepository,
                               CrmRapportVisiteRepository rapportVisiteRepository,
                               CrmTacheRepository tacheRepository) {
        this.entrepriseRepository = entrepriseRepository;
        this.contactRepository = contactRepository;
        this.opportuniteRepository = opportuniteRepository;
        this.visiteRepository = visiteRepository;
        this.rapportVisiteRepository = rapportVisiteRepository;
        this.tacheRepository = tacheRepository;
    }

    public CrmDashboardKPIsDTO getDashboardKPIs() {
        Long tenantId = TenantContext.getCurrentTenant();
        Long tId = tenantId != null ? tenantId : 1L;

        CrmDashboardKPIsDTO dto = new CrmDashboardKPIsDTO();

        // 1. Référentiel
        dto.setTotalEntreprises(entrepriseRepository.countByPointDeVenteId(tId));
        dto.setTotalContacts(contactRepository.countByPointDeVenteId(tId));
        dto.setTotalOpportunites(opportuniteRepository.countByPointDeVenteId(tId));

        // 2. Pipeline & Chiffres
        dto.setMontantPipelineTotal(opportuniteRepository.sumMontantPipelineEnCours(tId));

        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        dto.setMontantGagneMois(opportuniteRepository.sumMontantGagneMois(tId, debutMois));

        dto.setOpportunitesGagnees(opportuniteRepository.countByPointDeVenteIdAndEtape(tId, EtapeOpportunite.GAGNE));
        dto.setOpportunitesPerdues(opportuniteRepository.countByPointDeVenteIdAndEtape(tId, EtapeOpportunite.PERDU));

        // 3. Activité Visites
        LocalDateTime debutSemaine = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime finSemaine = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
        dto.setVisitesPlanifieesSemaine(visiteRepository.countVisitesPlanifieesSemaine(tId, debutSemaine, finSemaine));
        dto.setVisitesRealiseesMois(visiteRepository.countVisitesRealiseesMois(tId, debutMois));

        // 4. Validation Rapports & Retards
        dto.setRapportsEnAttenteValidation(rapportVisiteRepository.countRapportsEnAttenteValidation(tId));

        LocalDateTime seuil24h = LocalDateTime.now().minusHours(24);
        dto.setRapportsEnRetard(rapportVisiteRepository.countRapportsEnRetard(tId, seuil24h));

        // 5. Tâches
        dto.setTachesAFaire(tacheRepository.countByPointDeVenteIdAndStatut(tId, StatutTache.A_FAIRE));
        dto.setTachesEnRetard(tacheRepository.countTachesEnRetard(tId, LocalDate.now()));

        return dto;
    }
}
