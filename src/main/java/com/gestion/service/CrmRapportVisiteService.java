package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.CrmTacheDTO;
import com.gestion.persistent.enums.StatutRapportVisite;
import com.gestion.persistent.enums.StatutVisite;
import com.gestion.persistent.enums.TypeActiviteCrm;
import com.gestion.persistent.model.CrmRapportVisite;
import com.gestion.persistent.model.CrmTache;
import com.gestion.persistent.model.CrmVisite;
import com.gestion.repository.CrmRapportVisiteRepository;
import com.gestion.repository.CrmTacheRepository;
import com.gestion.repository.CrmVisiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CrmRapportVisiteService {

    private final CrmRapportVisiteRepository rapportVisiteRepository;
    private final CrmVisiteRepository visiteRepository;
    private final CrmTacheRepository tacheRepository;
    private final UserRepository userRepository;
    private final CrmActiviteService activiteService;

    public CrmRapportVisiteService(CrmRapportVisiteRepository rapportVisiteRepository,
                                  CrmVisiteRepository visiteRepository,
                                  CrmTacheRepository tacheRepository,
                                  UserRepository userRepository,
                                  CrmActiviteService activiteService) {
        this.rapportVisiteRepository = rapportVisiteRepository;
        this.visiteRepository = visiteRepository;
        this.tacheRepository = tacheRepository;
        this.userRepository = userRepository;
        this.activiteService = activiteService;
    }

    public CrmRapportVisite enregistrerBrouillon(Long visiteId, CrmRapportVisite rapport) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmVisite visite = visiteRepository.findByIdAndPointDeVenteId(visiteId, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Visite introuvable avec l'id : " + visiteId));

        if (visite.getStatut() == StatutVisite.ANNULEE) {
            throw new IllegalStateException("Impossible de rédiger un rapport pour une visite annulée.");
        }

        Optional<CrmRapportVisite> existingOpt = rapportVisiteRepository.findByVisiteId(visiteId);
        CrmRapportVisite entityToSave;

        if (existingOpt.isPresent()) {
            entityToSave = existingOpt.get();
            // Règle métier : Un rapport validé est verrouillé en lecture seule
            if (entityToSave.isVerrouille()) {
                throw new IllegalStateException("Ce rapport de visite a été validé par le manager et est désormais verrouillé en lecture seule.");
            }
            entityToSave.setResume(rapport.getResume());
            entityToSave.setInterlocuteursPresents(rapport.getInterlocuteursPresents());
            entityToSave.setBesoinsIdentifies(rapport.getBesoinsIdentifies());
            entityToSave.setObjectionsSoulevees(rapport.getObjectionsSoulevees());
            entityToSave.setResultat(rapport.getResultat());
            entityToSave.setProchaineEtape(rapport.getProchaineEtape());
            entityToSave.setDureeMinutes(rapport.getDureeMinutes());
            entityToSave.setDateModification(LocalDateTime.now());
        } else {
            entityToSave = rapport;
            entityToSave.setPointDeVenteId(tenantId != null ? tenantId : 1L);
            entityToSave.setVisite(visite);
            entityToSave.setStatut(StatutRapportVisite.BROUILLON);
            entityToSave.setDateCreation(LocalDateTime.now());
            entityToSave.setDateModification(LocalDateTime.now());
        }

        return rapportVisiteRepository.save(entityToSave);
    }

    public CrmRapportVisite soumettreRapport(Long visiteId, CrmRapportVisite rapport, List<CrmTacheDTO> actionsSuivi) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmVisite visite = visiteRepository.findByIdAndPointDeVenteId(visiteId, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Visite introuvable avec l'id : " + visiteId));

        if (visite.getStatut() == StatutVisite.ANNULEE) {
            throw new IllegalStateException("Impossible de soumettre un rapport pour une visite annulée.");
        }

        CrmRapportVisite savedRapport = enregistrerBrouillon(visiteId, rapport);
        savedRapport.setStatut(StatutRapportVisite.SOUMIS);
        savedRapport.setSoumisLe(LocalDateTime.now());
        savedRapport.setDateModification(LocalDateTime.now());
        CrmRapportVisite updatedRapport = rapportVisiteRepository.save(savedRapport);

        // Clôture ou passage en attente de la visite
        visite.setStatut(StatutVisite.REALISEE);
        visiteRepository.save(visite);

        // Création automatique des actions de suivi (tâches)
        if (actionsSuivi != null && !actionsSuivi.isEmpty()) {
            for (CrmTacheDTO tacheDto : actionsSuivi) {
                if (tacheDto.getTitre() != null && !tacheDto.getTitre().trim().isEmpty()) {
                    CrmTache tache = new CrmTache();
                    tache.setPointDeVenteId(tenantId != null ? tenantId : 1L);
                    tache.setTitre(tacheDto.getTitre().trim());
                    tache.setDescription(tacheDto.getDescription());
                    tache.setVisite(visite);
                    tache.setRapport(updatedRapport);
                    tache.setEntreprise(visite.getEntreprise());
                    tache.setContact(visite.getContact());
                    tache.setOpportunite(visite.getOpportunite());
                    tache.setPriorite(tacheDto.getPriorite());
                    tache.setDateEcheance(tacheDto.getDateEcheance());
                    tache.setCreePar(visite.getCommercial());

                    if (tacheDto.getAssigneAUserId() != null) {
                        userRepository.findById(tacheDto.getAssigneAUserId()).ifPresent(tache::setAssigneA);
                    }
                    if (tache.getAssigneA() == null) {
                        tache.setAssigneA(visite.getCommercial()); // fallback au commercial de la visite
                    }

                    tacheRepository.save(tache);
                }
            }
        }

        activiteService.enregistrerActivite(
                TypeActiviteCrm.RAPPORT,
                "Soumission du rapport de visite",
                "Rapport soumis par " + (visite.getCommercial() != null ? visite.getCommercial().getNomComplet() : "commercial") +
                        ". Résultat : " + updatedRapport.getResultat(),
                visite.getEntreprise(), visite.getContact(), visite.getOpportunite(), visite, visite.getCommercial()
        );

        return updatedRapport;
    }

    public CrmRapportVisite validerRapportParManager(Long rapportId, Long managerUserId, boolean approuve, String motifRejet) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmRapportVisite rapport = rapportVisiteRepository.findByIdAndPointDeVenteId(rapportId, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Rapport non trouvé avec l'id : " + rapportId));

        User manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur manager introuvable avec l'id : " + managerUserId));

        if (approuve) {
            rapport.setStatut(StatutRapportVisite.VALIDE);
            rapport.setValidePar(manager);
            rapport.setValideLe(LocalDateTime.now());
            rapport.setDateModification(LocalDateTime.now());

            // Clôture définitive de la visite
            CrmVisite visite = rapport.getVisite();
            if (visite != null) {
                visite.setStatut(StatutVisite.CLOTUREE);
                visiteRepository.save(visite);
            }

            activiteService.enregistrerActivite(
                    TypeActiviteCrm.RAPPORT,
                    "Validation du rapport par le manager",
                    "Rapport validé par " + manager.getNomComplet() + " et verrouillé en lecture seule.",
                    visite != null ? visite.getEntreprise() : null,
                    visite != null ? visite.getContact() : null,
                    visite != null ? visite.getOpportunite() : null,
                    visite, manager
            );
        } else {
            if (motifRejet == null || motifRejet.trim().isEmpty()) {
                throw new IllegalArgumentException("Un motif de rejet est obligatoire en cas de refus d'approbation.");
            }
            rapport.setStatut(StatutRapportVisite.REJETE);
            rapport.setMotifRejet(motifRejet.trim());
            rapport.setDateModification(LocalDateTime.now());

            activiteService.enregistrerActivite(
                    TypeActiviteCrm.RAPPORT,
                    "Rejet du rapport de visite",
                    "Rapport rejeté par le manager. Motif : " + motifRejet.trim(),
                    rapport.getVisite() != null ? rapport.getVisite().getEntreprise() : null,
                    rapport.getVisite() != null ? rapport.getVisite().getContact() : null,
                    rapport.getVisite() != null ? rapport.getVisite().getOpportunite() : null,
                    rapport.getVisite(), manager
            );
        }

        return rapportVisiteRepository.save(rapport);
    }

    @Transactional(readOnly = true)
    public Optional<CrmRapportVisite> getRapportByVisiteId(Long visiteId) {
        return rapportVisiteRepository.findByVisiteId(visiteId);
    }

    @Transactional(readOnly = true)
    public CrmRapportVisite getRapportById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return rapportVisiteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Rapport non trouvé avec l'id : " + id));
    }

    @Transactional(readOnly = true)
    public List<CrmRapportVisite> getRapportsEnAttenteValidation() {
        Long tenantId = TenantContext.getCurrentTenant();
        return rapportVisiteRepository.findRapportsEnAttenteValidation(tenantId != null ? tenantId : 1L);
    }
}
