package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.model.FactureAchat;
import com.gestion.persistent.model.ReglementFournisseur;
import com.gestion.repository.ReglementFournisseurRepository;
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

    public ReglementFournisseurService(ReglementFournisseurRepository reglementFournisseurRepository,
                                     FactureAchatService factureAchatService) {
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.factureAchatService = factureAchatService;
    }

    public ReglementFournisseur enregistrerReglement(ReglementFournisseur reglement) {
        Long tenantId = TenantContext.getCurrentTenant();
        reglement.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        reglement.setDateReglement(LocalDateTime.now());
        reglement.setNumeroReglement(genererNumeroReglement());

        // Validate invoice
        FactureAchat facture = factureAchatService.getFactureAchatById(reglement.getFactureAchat().getId());
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

