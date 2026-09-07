package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.model.FactureAchat;
import com.gestion.persistent.model.ReglementFournisseur;
import com.gestion.repository.ReglementFournisseurRepository;
import com.gestion.persistent.enums.ModePaiement;
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

    public ReglementFournisseurService(ReglementFournisseurRepository reglementFournisseurRepository,
                                     FactureAchatService factureAchatService,
                                     com.gestion.repository.ChequeEffetRepository chequeEffetRepository,
                                     @org.springframework.context.annotation.Lazy ComptabiliteService comptabiliteService) {
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.factureAchatService = factureAchatService;
        this.chequeEffetRepository = chequeEffetRepository;
        this.comptabiliteService = comptabiliteService;
    }

    public ReglementFournisseur enregistrerReglement(ReglementFournisseur reglement) {
        Long tenantId = TenantContext.getCurrentTenant();
        reglement.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        reglement.setDateReglement(LocalDateTime.now());
        reglement.setNumeroReglement(genererNumeroReglement());

        // Validate invoice
        FactureAchat facture = factureAchatService.getFactureAchatEntityById(reglement.getFactureAchat().getId());
        reglement.setFactureAchat(facture);

        // Save payment
        ReglementFournisseur savedReglement = reglementFournisseurRepository.save(reglement);

        // Recalculate total payments for this invoice
        List<ReglementFournisseur> reglements = reglementFournisseurRepository
                .findByFactureAchatIdAndPointDeVenteId(facture.getId(), tenantId != null ? tenantId : 1L);

        BigDecimal totalPaye = reglements.stream()
                .map(ReglementFournisseur::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Update invoice status
        factureAchatService.updateStatutFacture(facture, totalPaye);

        // Si paiement par chèque, insérer automatiquement dans le portefeuille Trésorerie (Décaissement)
        if (savedReglement.getModePaiement() == ModePaiement.CHEQUE) {
            com.gestion.persistent.model.ChequeEffet cheque = new com.gestion.persistent.model.ChequeEffet();
            cheque.setNumeroPiece(savedReglement.getNumeroCheque() != null && !savedReglement.getNumeroCheque().trim().isEmpty()
                    ? savedReglement.getNumeroCheque().trim() : savedReglement.getNumeroReglement());
            cheque.setTypeEffet(com.gestion.persistent.enums.TypeEffet.CHEQUE);
            cheque.setSens(com.gestion.persistent.enums.SensEffet.DECAISSEMENT_FOURNISSEUR);
            cheque.setStatut(com.gestion.persistent.enums.StatutEffet.EN_PORTEFEUILLE);
            cheque.setMontant(savedReglement.getMontant());
            cheque.setDateEmission(savedReglement.getDateReglement() != null ? savedReglement.getDateReglement().toLocalDate() : java.time.LocalDate.now());
            cheque.setDateEcheance(savedReglement.getDateEcheance() != null ? savedReglement.getDateEcheance().toLocalDate() : cheque.getDateEmission());
            cheque.setFournisseur(facture.getFournisseur());
            cheque.setBeneficiaire(facture.getFournisseur() != null ? facture.getFournisseur().getNom() : "Fournisseur");
            cheque.setBanqueEmettrice(savedReglement.getNomBanque());
            cheque.setReferencePaiement(savedReglement.getNumeroReglement());
            cheque.setNotes(savedReglement.getNotes());
            cheque.setPointDeVenteId(tenantId != null ? tenantId : 1L);
            cheque.setDateCreation(LocalDateTime.now());
            chequeEffetRepository.save(cheque);
        }

        // Génération écriture comptable
        try {
            comptabiliteService.genererEcritureReglementFournisseur(savedReglement);
        } catch (Exception e) {
            // Ne pas bloquer l'enregistrement
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

    private String genererNumeroReglement() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = reglementFournisseurRepository.count() + 1;
        return "REG-FOUR-" + dateStr + "-" + String.format("%04d", count);
    }
}

