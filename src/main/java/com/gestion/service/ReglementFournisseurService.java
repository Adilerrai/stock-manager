package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.model.FactureAchat;
import com.gestion.persistent.model.ReglementFournisseur;
import com.gestion.repository.ReglementFournisseurRepository;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.dto.ReglementFournisseurSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class ReglementFournisseurService {

    private final ReglementFournisseurRepository reglementFournisseurRepository;
    private final FactureAchatService factureAchatService;
    private final com.gestion.repository.ChequeEffetRepository chequeEffetRepository;
    private final ComptabiliteService comptabiliteService;
    private final com.gestion.repository.CommandeRepository commandeRepository;

    public ReglementFournisseurService(ReglementFournisseurRepository reglementFournisseurRepository,
                                     FactureAchatService factureAchatService,
                                     com.gestion.repository.ChequeEffetRepository chequeEffetRepository,
                                     @org.springframework.context.annotation.Lazy ComptabiliteService comptabiliteService,
                                     com.gestion.repository.CommandeRepository commandeRepository) {
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.factureAchatService = factureAchatService;
        this.chequeEffetRepository = chequeEffetRepository;
        this.comptabiliteService = comptabiliteService;
        this.commandeRepository = commandeRepository;
    }

    public ReglementFournisseur enregistrerReglement(ReglementFournisseur reglement) {
        Long tenantId = TenantContext.getCurrentTenant();
        reglement.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        if (reglement.getDateReglement() == null) {
            reglement.setDateReglement(LocalDateTime.now());
        }
        if (reglement.getNumeroReglement() == null || reglement.getNumeroReglement().trim().isEmpty()) {
            reglement.setNumeroReglement(genererNumeroReglement());
        }

        // Validate invoice (support both nested object and factureAchatId)
        Long factureId = null;
        if (reglement.getFactureAchat() != null && reglement.getFactureAchat().getId() != null) {
            factureId = reglement.getFactureAchat().getId();
        } else if (reglement.getFactureAchatId() != null) {
            factureId = reglement.getFactureAchatId();
        }

        Long commandeId = null;
        if (reglement.getCommande() != null && reglement.getCommande().getId() != null) {
            commandeId = reglement.getCommande().getId();
        } else if (reglement.getCommandeId() != null) {
            commandeId = reglement.getCommandeId();
        }

        if (factureId == null && commandeId == null) {
            throw new IllegalArgumentException("L'identifiant de la facture d'achat ou de la commande est obligatoire pour enregistrer un règlement");
        }

        FactureAchat facture = null;
        if (factureId != null) {
            facture = factureAchatService.getFactureAchatEntityById(factureId);
            reglement.setFactureAchat(facture);
            reglement.setFactureAchatId(facture.getId());
            if (reglement.getTypeReglement() == null || reglement.getTypeReglement().isBlank()) {
                reglement.setTypeReglement("FACTURE");
            }
        }

        if (commandeId != null) {
            final Long targetCommandeId = commandeId;
            com.gestion.persistent.model.Commande commande = commandeRepository.findById(targetCommandeId)
                    .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée avec l'id: " + targetCommandeId));
            reglement.setCommande(commande);
            reglement.setCommandeId(commande.getId());
            if (factureId == null) {
                reglement.setTypeReglement("ACOMPTE_COMMANDE");
            }
        }

        if (reglement.getModePaiement() == null && reglement.getModeReglement() != null) {
            reglement.setModePaiement(reglement.getModeReglement());
        }
        if (reglement.getModePaiement() == null) {
            reglement.setModePaiement(ModePaiement.ESPECES);
        }

        // Si montant en devises fourni et montant non saisi, calculer la contre-valeur en MAD
        if (reglement.getMontantDevise() != null && reglement.getTauxChange() != null && (reglement.getMontant() == null || reglement.getMontant().compareTo(BigDecimal.ZERO) <= 0)) {
            reglement.setMontant(reglement.getMontantDevise().multiply(reglement.getTauxChange()));
        }

        if (reglement.getMontant() == null || reglement.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du règlement doit être supérieur à zéro");
        }

        // Save payment
        ReglementFournisseur savedReglement = reglementFournisseurRepository.save(reglement);

        // Recalculate total payments for this invoice (uniquement si rattaché à une facture)
        if (facture != null) {
            List<ReglementFournisseur> reglements = reglementFournisseurRepository
                    .findByFactureAchatIdAndPointDeVenteId(facture.getId(), tenantId != null ? tenantId : 1L);

            BigDecimal totalPaye = reglements.stream()
                    .map(ReglementFournisseur::getMontant)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Update invoice status
            factureAchatService.updateStatutFacture(facture, totalPaye);
        }

        // Si paiement par chèque ou traite, insérer automatiquement dans le portefeuille Trésorerie (Décaissement)
        if (savedReglement.getModePaiement() == ModePaiement.CHEQUE || savedReglement.getModePaiement() == ModePaiement.TRAITE) {
            com.gestion.persistent.model.Fournisseur fournisseurBeneficiaire = (facture != null) 
                    ? facture.getFournisseur() 
                    : (reglement.getCommande() != null ? reglement.getCommande().getFournisseur() : null);

            com.gestion.persistent.model.ChequeEffet cheque = new com.gestion.persistent.model.ChequeEffet();
            cheque.setNumeroPiece(savedReglement.getNumeroCheque() != null && !savedReglement.getNumeroCheque().trim().isEmpty()
                    ? savedReglement.getNumeroCheque().trim() : savedReglement.getNumeroReglement());
            cheque.setTypeEffet(savedReglement.getModePaiement() == ModePaiement.TRAITE 
                    ? com.gestion.persistent.enums.TypeEffet.TRAITE 
                    : com.gestion.persistent.enums.TypeEffet.CHEQUE);
            cheque.setSens(com.gestion.persistent.enums.SensEffet.DECAISSEMENT_FOURNISSEUR);
            cheque.setStatut(com.gestion.persistent.enums.StatutEffet.EN_PORTEFEUILLE);
            cheque.setMontant(savedReglement.getMontant());
            cheque.setDateEmission(savedReglement.getDateReglement() != null ? savedReglement.getDateReglement().toLocalDate() : java.time.LocalDate.now());
            cheque.setDateEcheance(savedReglement.getDateEcheance() != null ? savedReglement.getDateEcheance().toLocalDate() : cheque.getDateEmission());
            cheque.setFournisseur(fournisseurBeneficiaire);
            cheque.setBeneficiaire(fournisseurBeneficiaire != null ? fournisseurBeneficiaire.getNom() : "Fournisseur");
            cheque.setBanqueEmettrice(savedReglement.getNomBanque());
            cheque.setReferencePaiement(savedReglement.getNumeroReglement());
            cheque.setNotes(savedReglement.getNotes());
            cheque.setPointDeVenteId(tenantId != null ? tenantId : 1L);
            cheque.setDateCreation(LocalDateTime.now());
            chequeEffetRepository.save(cheque);
        }
        return savedReglement;
    }

    public List<ReglementFournisseur> getReglements() {
        Long tenantId = TenantContext.getCurrentTenant();
        return reglementFournisseurRepository.findByPointDeVenteId(tenantId != null ? tenantId : 1L);
    }

    public List<ReglementFournisseur> getReglementsByFacture(Long factureId) {
        Long tenantId = TenantContext.getCurrentTenant();
        return reglementFournisseurRepository.findByFactureAchatIdAndPointDeVenteId(factureId, tenantId != null ? tenantId : 1L);
    }

    public Page<ReglementFournisseur> searchReglements(ReglementFournisseurSearchCriteria criteria, Pageable pageable) {
        return reglementFournisseurRepository.findByCriteria(criteria, pageable);
    }

    public List<ReglementFournisseur> searchReglements(ReglementFournisseurSearchCriteria criteria) {
        return reglementFournisseurRepository.findByCriteria(criteria);
    }

    private String genererNumeroReglement() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = reglementFournisseurRepository.count() + 1;
        return "REG-FOUR-" + dateStr + "-" + String.format("%04d", count);
    }
}

