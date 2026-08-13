package com.ceramique.service;

import com.acommon.persistant.model.TenantContext;
import com.ceramique.persistent.enums.StatutFacture;
import com.ceramique.persistent.model.FactureAchat;
import com.ceramique.persistent.model.LigneFactureAchat;
import com.ceramique.persistent.model.Fournisseur;
import com.ceramique.repository.FactureAchatRepository;
import com.ceramique.repository.FournisseurRepository;
import com.ceramique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class FactureAchatService {

    private final FactureAchatRepository factureAchatRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;

    public FactureAchatService(FactureAchatRepository factureAchatRepository,
                               FournisseurRepository fournisseurRepository,
                               ProduitRepository produitRepository) {
        this.factureAchatRepository = factureAchatRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
    }

    public FactureAchat creerFactureAchat(FactureAchat facture) {
        Long tenantId = TenantContext.getCurrentTenant();
        facture.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        facture.setDateFacture(LocalDateTime.now());
        facture.setNumeroFacture(genererNumeroFactureAchat());
        facture.setStatut(StatutFacture.EN_ATTENTE);

        // Load and validate supplier
        Fournisseur fournisseur = fournisseurRepository.findById(facture.getFournisseur().getId())
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));
        facture.setFournisseur(fournisseur);

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        for (LigneFactureAchat ligne : facture.getLignes()) {
            ligne.setFactureAchat(facture);
            
            // Validate product
            ligne.setProduit(produitRepository.findById(ligne.getProduit().getId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé")));

            // Default TVA rate is 19% if not provided
            if (ligne.getTauxTva() == null) {
                ligne.setTauxTva(BigDecimal.valueOf(19.00));
            }

            // Calculate line amounts
            BigDecimal ht = ligne.getPrixUnitaireHt().multiply(ligne.getQuantite());
            BigDecimal tva = ht.multiply(ligne.getTauxTva()).divide(BigDecimal.valueOf(100));
            BigDecimal ttc = ht.add(tva);

            ligne.setMontantHt(ht);
            ligne.setMontantTva(tva);
            ligne.setMontantTtc(ttc);

            totalHt = totalHt.add(ht);
            totalTva = totalTva.add(tva);
            totalTtc = totalTtc.add(ttc);
        }

        facture.setMontantHt(totalHt);
        facture.setMontantTva(totalTva);
        facture.setMontantTtc(totalTtc);

        return factureAchatRepository.save(facture);
    }

    public List<FactureAchat> getFacturesAchat() {
        Long tenantId = TenantContext.getCurrentTenant();
        return factureAchatRepository.findByPointDeVenteId(tenantId != null ? tenantId : 1L);
    }

    public FactureAchat getFactureAchatById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return factureAchatRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée"));
    }

    public void updateStatutFacture(FactureAchat facture, BigDecimal montantRegleTotal) {
        if (montantRegleTotal.compareTo(BigDecimal.ZERO) == 0) {
            facture.setStatut(StatutFacture.EN_ATTENTE);
        } else if (montantRegleTotal.compareTo(facture.getMontantTtc()) >= 0) {
            facture.setStatut(StatutFacture.PAYEE_TOTALEMENT);
        } else {
            facture.setStatut(StatutFacture.PAYEE_PARTIELLEMENT);
        }
        factureAchatRepository.save(facture);
    }

    private String genererNumeroFactureAchat() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = factureAchatRepository.count() + 1;
        return "FAC-ACH-" + dateStr + "-" + String.format("%04d", count);
    }
}
