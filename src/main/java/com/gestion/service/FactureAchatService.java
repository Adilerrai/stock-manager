package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.mapper.FactureAchatMapper;
import com.gestion.persistent.dto.FactureAchatDTO;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.FactureAchat;
import com.gestion.persistent.model.Fournisseur;
import com.gestion.persistent.model.LigneFactureAchat;
import com.gestion.repository.FactureAchatRepository;
import com.gestion.repository.FournisseurRepository;
import com.gestion.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FactureAchatService {

    private final FactureAchatRepository factureAchatRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final FactureAchatMapper factureAchatMapper;

    public FactureAchatService(FactureAchatRepository factureAchatRepository,
                               FournisseurRepository fournisseurRepository,
                               ProduitRepository produitRepository,
                               FactureAchatMapper factureAchatMapper) {
        this.factureAchatRepository = factureAchatRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.factureAchatMapper = factureAchatMapper;
    }

    public FactureAchatDTO creerFactureAchat(FactureAchatDTO dto) {
        FactureAchat facture = factureAchatMapper.toEntity(dto);
        Long tenantId = TenantContext.getCurrentTenant();
        facture.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        facture.setDateFacture(dto.getDateFacture() != null ? dto.getDateFacture() : LocalDateTime.now());
        facture.setDateEcheance(dto.getDateEcheance());
        if (dto.getNumeroFacture() != null && !dto.getNumeroFacture().isBlank()) {
            facture.setNumeroFacture(dto.getNumeroFacture());
        } else {
            facture.setNumeroFacture(genererNumeroFactureAchat());
        }
        facture.setStatut(StatutFacture.EN_ATTENTE);
        facture.setObservations(dto.getObservations());

        // Load and validate supplier
        Long fournisseurId = dto.getFournisseurId();
        if (fournisseurId == null && facture.getFournisseur() != null) {
            fournisseurId = facture.getFournisseur().getId();
        }
        if (fournisseurId == null) {
            throw new IllegalArgumentException("Le fournisseurId est obligatoire");
        }
        final Long targetFournisseurId = fournisseurId;
        Fournisseur fournisseur = fournisseurRepository.findById(targetFournisseurId)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id: " + targetFournisseurId));
        facture.setFournisseur(fournisseur);

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        if (dto.getLignes() != null) {
            for (var ligneDto : dto.getLignes()) {
                LigneFactureAchat ligne = new LigneFactureAchat();
                ligne.setFactureAchat(facture);

                // Validate product
                if (ligneDto.getProduitId() != null) {
                    ligne.setProduit(produitRepository.findById(ligneDto.getProduitId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + ligneDto.getProduitId())));
                }

                ligne.setQuantite(ligneDto.getQuantite() != null ? ligneDto.getQuantite() : BigDecimal.ONE);
                ligne.setPrixUnitaireHt(ligneDto.getPrixUnitaireHt() != null ? ligneDto.getPrixUnitaireHt() : BigDecimal.ZERO);
                ligne.setTauxTva(ligneDto.getTauxTva() != null ? ligneDto.getTauxTva() : BigDecimal.valueOf(19.00));

                BigDecimal ht = ligne.getPrixUnitaireHt().multiply(ligne.getQuantite());
                BigDecimal tva = ht.multiply(ligne.getTauxTva()).divide(BigDecimal.valueOf(100));
                BigDecimal ttc = ht.add(tva);

                ligne.setMontantHt(ht);
                ligne.setMontantTva(tva);
                ligne.setMontantTtc(ttc);

                totalHt = totalHt.add(ht);
                totalTva = totalTva.add(tva);
                totalTtc = totalTtc.add(ttc);

                facture.getLignes().add(ligne);
            }
        }

        facture.setMontantHt(totalHt);
        facture.setMontantTva(totalTva);
        facture.setMontantTtc(totalTtc);

        FactureAchat saved = factureAchatRepository.save(facture);
        return factureAchatMapper.toDto(saved);
    }

    public List<FactureAchatDTO> getFacturesAchat() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<FactureAchat> factures = factureAchatRepository.findByPointDeVenteId(tenantId != null ? tenantId : 1L);
        return factures.stream()
                .map(factureAchatMapper::toDto)
                .collect(Collectors.toList());
    }

    public FactureAchat getFactureAchatEntityById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return factureAchatRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée avec l'id: " + id));
    }

    public FactureAchatDTO getFactureAchatById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        FactureAchat facture = factureAchatRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée avec l'id: " + id));
        return factureAchatMapper.toDto(facture);
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
