package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.EtapeOpportunite;
import com.gestion.persistent.enums.StatutContact;
import com.gestion.persistent.enums.StatutEntreprise;
import com.gestion.persistent.enums.TypeActiviteCrm;
import com.gestion.persistent.model.CrmContact;
import com.gestion.persistent.model.CrmEntreprise;
import com.gestion.persistent.model.CrmOpportunite;
import com.gestion.repository.CrmContactRepository;
import com.gestion.repository.CrmEntrepriseRepository;
import com.gestion.repository.CrmOpportuniteRepository;
import com.gestion.repository.DevisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CrmOpportuniteService {

    private final CrmOpportuniteRepository opportuniteRepository;
    private final CrmEntrepriseRepository entrepriseRepository;
    private final CrmContactRepository contactRepository;
    private final UserRepository userRepository;
    private final DevisRepository devisRepository;
    private final CrmActiviteService activiteService;

    public CrmOpportuniteService(CrmOpportuniteRepository opportuniteRepository,
                                 CrmEntrepriseRepository entrepriseRepository,
                                 CrmContactRepository contactRepository,
                                 UserRepository userRepository,
                                 DevisRepository devisRepository,
                                 CrmActiviteService activiteService) {
        this.opportuniteRepository = opportuniteRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.devisRepository = devisRepository;
        this.activiteService = activiteService;
    }

    public CrmOpportunite creerOpportunite(CrmOpportunite opp) {
        Long tenantId = TenantContext.getCurrentTenant();
        opp.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        opp.setDateCreation(LocalDateTime.now());
        opp.setDateModification(LocalDateTime.now());
        if (opp.getEtape() == null) {
            opp.setEtape(EtapeOpportunite.NOUVEAU);
        }

        if (opp.getEntrepriseId() != null) {
            entrepriseRepository.findByIdAndPointDeVenteId(opp.getEntrepriseId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(opp::setEntreprise);
        }

        if (opp.getContactId() != null) {
            contactRepository.findByIdAndPointDeVenteId(opp.getContactId(), tenantId != null ? tenantId : 1L)
                    .ifPresent(opp::setContact);
        }

        if (opp.getCommercialId() != null) {
            userRepository.findById(opp.getCommercialId())
                    .ifPresent(opp::setCommercial);
        } else if (opp.getEntreprise() != null && opp.getEntreprise().getCommercialResponsable() != null) {
            opp.setCommercial(opp.getEntreprise().getCommercialResponsable());
        }

        if (opp.getDevisId() != null) {
            devisRepository.findById(opp.getDevisId())
                    .ifPresent(opp::setDevis);
        }

        CrmOpportunite saved = opportuniteRepository.save(opp);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.ETAPE_OPPORTUNITE,
                "Création de l'opportunité : " + saved.getTitre(),
                "Montant estimé : " + saved.getMontantEstime() + " MAD, Probabilité : " + saved.getProbabilite() + "%",
                saved.getEntreprise(), saved.getContact(), saved, null, saved.getCommercial()
        );

        return saved;
    }

    public CrmOpportunite changerEtape(Long id, EtapeOpportunite nouvelleEtape, String motifPerte, String notes) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmOpportunite existing = opportuniteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Opportunité non trouvée avec l'id : " + id));

        EtapeOpportunite ancienneEtape = existing.getEtape();

        // Règle métier : Vérification de l'ordre de transition
        if (!ancienneEtape.canTransitionTo(nouvelleEtape)) {
            throw new IllegalStateException("Transition d'étape non autorisée de " + ancienneEtape + " vers " + nouvelleEtape);
        }

        // Règle métier : motif de perte obligatoire en cas de PERDU
        if (nouvelleEtape == EtapeOpportunite.PERDU) {
            if (motifPerte == null || motifPerte.trim().isEmpty()) {
                throw new IllegalArgumentException("Un motif de perte est obligatoire pour marquer une opportunité comme Perdue.");
            }
            existing.setMotifPerte(motifPerte.trim());
            existing.setDateClotureEffective(LocalDateTime.now());
        }

        // Règle métier : opportunité GAGNÉE
        if (nouvelleEtape == EtapeOpportunite.GAGNE) {
            existing.setDateClotureEffective(LocalDateTime.now());
            existing.setProbabilite(100);

            // Mettre à jour le statut du prospect en CLIENT
            if (existing.getEntreprise() != null && existing.getEntreprise().getStatut() == StatutEntreprise.PROSPECT) {
                existing.getEntreprise().setStatut(StatutEntreprise.CLIENT);
                existing.getEntreprise().setDateModification(LocalDateTime.now());
                entrepriseRepository.save(existing.getEntreprise());
            }
            if (existing.getContact() != null && existing.getContact().getStatut() == StatutContact.PROSPECT) {
                existing.getContact().setStatut(StatutContact.CLIENT);
                existing.getContact().setDateModification(LocalDateTime.now());
                contactRepository.save(existing.getContact());
            }
        }

        if (notes != null && !notes.trim().isEmpty()) {
            existing.setNotes(notes);
        }

        existing.setEtape(nouvelleEtape);
        existing.setDateModification(LocalDateTime.now());
        CrmOpportunite updated = opportuniteRepository.save(existing);

        activiteService.enregistrerActivite(
                TypeActiviteCrm.ETAPE_OPPORTUNITE,
                "Changement d'étape opportunité : " + nouvelleEtape,
                "Passage de l'étape " + ancienneEtape + " à " + nouvelleEtape +
                        (nouvelleEtape == EtapeOpportunite.PERDU ? " (Motif: " + motifPerte + ")" : ""),
                updated.getEntreprise(), updated.getContact(), updated, null, updated.getCommercial()
        );

        return updated;
    }

    public CrmOpportunite modifierOpportunite(Long id, CrmOpportunite details) {
        Long tenantId = TenantContext.getCurrentTenant();
        CrmOpportunite existing = opportuniteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Opportunité non trouvée avec l'id : " + id));

        existing.setTitre(details.getTitre());
        existing.setMontantEstime(details.getMontantEstime());
        existing.setProbabilite(details.getProbabilite());
        existing.setDateCloturePrevue(details.getDateCloturePrevue());
        existing.setNotes(details.getNotes());
        existing.setDateModification(LocalDateTime.now());

        if (details.getCommercialId() != null) {
            userRepository.findById(details.getCommercialId())
                    .ifPresent(existing::setCommercial);
        }

        return opportuniteRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<CrmOpportunite> getAllOpportunites() {
        Long tenantId = TenantContext.getCurrentTenant();
        return opportuniteRepository.findByPointDeVenteIdOrderByDateCreationDesc(tenantId != null ? tenantId : 1L);
    }

    @Transactional(readOnly = true)
    public List<CrmOpportunite> getOpportunitesByCommercial(Long commercialId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return opportuniteRepository.findByPointDeVenteIdAndCommercialIdOrderByDateCreationDesc(
                tenantId != null ? tenantId : 1L, commercialId);
    }

    @Transactional(readOnly = true)
    public CrmOpportunite getOpportuniteById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return opportuniteRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new IllegalArgumentException("Opportunité non trouvée avec l'id : " + id));
    }
}
