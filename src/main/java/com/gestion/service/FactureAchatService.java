package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.mapper.FactureAchatMapper;
import com.gestion.persistent.dto.FactureAchatDTO;
import com.gestion.persistent.dto.FactureAchatSearchCriteria;
import com.gestion.persistent.enums.StatutFacture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final CodificationService codificationService;
    private final com.gestion.repository.ReglementFournisseurRepository reglementFournisseurRepository;
    private final com.gestion.repository.CommandeRepository commandeRepository;

    public FactureAchatService(FactureAchatRepository factureAchatRepository,
                               FournisseurRepository fournisseurRepository,
                               ProduitRepository produitRepository,
                               FactureAchatMapper factureAchatMapper,
                               CodificationService codificationService,
                               com.gestion.repository.ReglementFournisseurRepository reglementFournisseurRepository,
                               com.gestion.repository.CommandeRepository commandeRepository) {
        this.factureAchatRepository = factureAchatRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.factureAchatMapper = factureAchatMapper;
        this.codificationService = codificationService;
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.commandeRepository = commandeRepository;
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

        if (dto.getCommandeId() != null) {
            com.gestion.persistent.model.Commande commande = commandeRepository.findById(dto.getCommandeId()).orElse(null);
            facture.setCommande(commande);
        }

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
                ligne.setTauxTva(ligneDto.getTauxTva() != null ? ligneDto.getTauxTva() : BigDecimal.valueOf(20.00));

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

        // Imputation automatique des acomptes réservés sur cette commande
        BigDecimal totalAcomptes = BigDecimal.ZERO;
        if (saved.getCommande() != null) {
            List<com.gestion.persistent.model.ReglementFournisseur> acomptes = reglementFournisseurRepository
                    .findByCommandeIdAndFactureAchatIsNull(saved.getCommande().getId());
            for (var acompte : acomptes) {
                acompte.setFactureAchat(saved);
                acompte.setTypeReglement("ACOMPTE_IMPUTE");
                reglementFournisseurRepository.save(acompte);
                if (acompte.getMontant() != null) {
                    totalAcomptes = totalAcomptes.add(acompte.getMontant());
                }
            }
            if (totalAcomptes.compareTo(BigDecimal.ZERO) > 0) {
                updateStatutFacture(saved, totalAcomptes);
            }
        }

        FactureAchatDTO resDto = factureAchatMapper.toDto(saved);
        resDto.setMontantPaye(totalAcomptes);
        resDto.setMontantRestant(saved.getMontantTtc() != null ? saved.getMontantTtc().subtract(totalAcomptes).max(BigDecimal.ZERO) : BigDecimal.ZERO);
        return resDto;
    }

    public List<FactureAchatDTO> getFacturesAchat() {
        return getFacturesAchat(null, null);
    }

    public List<FactureAchatDTO> getFacturesAchat(java.time.LocalDate dateDebut, java.time.LocalDate dateFin) {
        FactureAchatSearchCriteria criteria = new FactureAchatSearchCriteria();
        criteria.setDateDebut(dateDebut);
        criteria.setDateFin(dateFin);
        return searchFacturesAchat(criteria);
    }

    public Page<FactureAchatDTO> searchFacturesAchat(FactureAchatSearchCriteria criteria, Pageable pageable) {
        Page<FactureAchat> page = factureAchatRepository.findByCriteria(criteria, pageable);
        List<FactureAchatDTO> dtos = page.getContent().stream()
                .map(factureAchatMapper::toDto)
                .collect(Collectors.toList());
        enrichirSoldes(dtos);
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public List<FactureAchatDTO> searchFacturesAchat(FactureAchatSearchCriteria criteria) {
        List<FactureAchat> factures = factureAchatRepository.findByCriteria(criteria);
        List<FactureAchatDTO> dtos = factures.stream()
                .map(factureAchatMapper::toDto)
                .collect(Collectors.toList());
        enrichirSoldes(dtos);
        return dtos;
    }

    public List<FactureAchatDTO> getFacturesAchatImpayees() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<FactureAchat> factures = factureAchatRepository.findByPointDeVenteId(tenantId != null ? tenantId : 1L);
        List<FactureAchatDTO> dtos = factures.stream()
                .filter(f -> f.getStatut() != StatutFacture.PAYEE_TOTALEMENT && f.getStatut() != StatutFacture.ANNULEE)
                .map(factureAchatMapper::toDto)
                .collect(Collectors.toList());
        enrichirSoldes(dtos);
        return dtos.stream()
                .filter(dto -> dto.getMontantRestant() != null && dto.getMontantRestant().compareTo(BigDecimal.ZERO) > 0)
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
        FactureAchatDTO dto = factureAchatMapper.toDto(facture);
        enrichirSoldes(java.util.Collections.singletonList(dto));
        return dto;
    }

    private void enrichirSoldes(List<FactureAchatDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) return;
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;

        java.util.Set<Long> ids = dtoList.stream()
                .map(FactureAchatDTO::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return;

        java.util.Map<Long, BigDecimal> mapPaye = new java.util.HashMap<>();
        try {
            List<Object[]> sums = reglementFournisseurRepository.sumMontantPayeByFactureIds(ids, effectiveTenantId);
            if (sums != null) {
                for (Object[] row : sums) {
                    if (row != null && row.length >= 2 && row[0] != null) {
                        Long fId = (Long) row[0];
                        BigDecimal sum = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                        mapPaye.put(fId, sum);
                    }
                }
            }
        } catch (Exception e) {
            // Log fallback without failing request
        }

        for (FactureAchatDTO dto : dtoList) {
            BigDecimal paye = mapPaye.getOrDefault(dto.getId(), BigDecimal.ZERO);
            dto.setMontantPaye(paye);
            BigDecimal ttc = dto.getMontantTtc() != null ? dto.getMontantTtc() : BigDecimal.ZERO;
            dto.setMontantRestant(ttc.subtract(paye).max(BigDecimal.ZERO));
        }
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
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        String numero;
        int attempts = 0;
        do {
            numero = codificationService.genererNumero(com.gestion.persistent.enums.TypeDocumentCodification.FACTURE_FOURNISSEUR, effectiveTenantId);
            attempts++;
        } while (factureAchatRepository.findByNumeroFactureAndPointDeVenteId(numero, effectiveTenantId).isPresent() && attempts < 1000);
        return numero;
    }
}
