package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.CurrentRequestContext;
import com.gestion.persistent.dto.DemandeAchatDTO;
import com.gestion.persistent.dto.DemandePrixDTO;
import com.gestion.persistent.dto.LigneDemandeAchatDTO;
import com.gestion.persistent.dto.LigneDemandePrixDTO;
import com.gestion.persistent.enums.PrioriteDemandeAchat;
import com.gestion.persistent.enums.StatutDemandeAchat;
import com.gestion.persistent.enums.TypeAchat;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.model.DemandeAchat;
import com.gestion.persistent.model.DemandePrix;
import com.gestion.persistent.model.LigneDemandeAchat;
import com.gestion.persistent.model.Produit;
import com.gestion.repository.DemandeAchatRepository;
import com.gestion.repository.DemandePrixRepository;
import com.gestion.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DemandeAchatService {

    private final DemandeAchatRepository demandeAchatRepository;
    private final DemandePrixRepository demandePrixRepository;
    private final DemandePrixService demandePrixService;
    private final ProduitRepository produitRepository;
    private final CodificationService codificationService;

    public DemandeAchatService(DemandeAchatRepository demandeAchatRepository,
                               DemandePrixRepository demandePrixRepository,
                               DemandePrixService demandePrixService,
                               ProduitRepository produitRepository,
                               CodificationService codificationService) {
        this.demandeAchatRepository = demandeAchatRepository;
        this.demandePrixRepository = demandePrixRepository;
        this.demandePrixService = demandePrixService;
        this.produitRepository = produitRepository;
        this.codificationService = codificationService;
    }

    private Long getTenantId() {
        Long tenant = CurrentRequestContext.getSocieteId();
        if (tenant == null) {
            tenant = TenantContext.getCurrentTenant();
        }
        return tenant != null ? tenant : 1L;
    }

    @Transactional
    public DemandeAchatDTO createDemandeAchat(DemandeAchatDTO dto) {
        Long tenantId = getTenantId();
        DemandeAchat da = new DemandeAchat();

        String numero = dto.getNumeroDa();
        if (numero == null || numero.isBlank()) {
            numero = codificationService.genererNumero(TypeDocumentCodification.DEMANDE_ACHAT, tenantId);
        }
        da.setNumeroDa(numero);
        da.setDemandeurNom(dto.getDemandeurNom());
        da.setServiceDemandeur(dto.getServiceDemandeur());
        da.setObjet(dto.getObjet() != null && !dto.getObjet().isBlank() ? dto.getObjet() : "Demande d'achat " + numero);
        da.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutDemandeAchat.BROUILLON);
        da.setPriorite(dto.getPriorite() != null ? dto.getPriorite() : PrioriteDemandeAchat.NORMALE);
        da.setDateDemande(dto.getDateDemande() != null ? dto.getDateDemande() : LocalDateTime.now());
        da.setDateSouhaitee(dto.getDateSouhaitee());
        da.setObservations(dto.getObservations());
        da.setPointDeVenteId(tenantId);

        if (dto.getLignes() != null) {
            for (LigneDemandeAchatDTO ligneDTO : dto.getLignes()) {
                LigneDemandeAchat ligne = new LigneDemandeAchat();
                ligne.setDemandeAchat(da);
                ligne.setPointDeVenteId(tenantId);
                ligne.setQuantiteDemandee(ligneDTO.getQuantiteDemandee() != null ? ligneDTO.getQuantiteDemandee() : BigDecimal.ONE);
                ligne.setUnite(ligneDTO.getUnite() != null ? ligneDTO.getUnite() : "U");
                ligne.setRemarques(ligneDTO.getRemarques());

                if (ligneDTO.getProduitId() != null) {
                    Produit prod = produitRepository.findById(ligneDTO.getProduitId()).orElse(null);
                    ligne.setProduit(prod);
                    ligne.setDesignation(prod != null && prod.getNom() != null ? prod.getNom() : ligneDTO.getDesignation());
                    ligne.setReference(prod != null ? prod.getReference() : ligneDTO.getReference());
                } else {
                    ligne.setDesignation(ligneDTO.getDesignation() != null ? ligneDTO.getDesignation() : "Article");
                    ligne.setReference(ligneDTO.getReference());
                }
                da.getLignes().add(ligne);
            }
        }

        DemandeAchat saved = demandeAchatRepository.save(da);
        return toDTO(saved);
    }

    @Transactional
    public DemandeAchatDTO updateDemandeAchat(Long id, DemandeAchatDTO dto) {
        Long tenantId = getTenantId();
        DemandeAchat da = demandeAchatRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'achat non trouvée avec l'id: " + id));

        if (da.getStatut() == StatutDemandeAchat.TOTALEMENT_TRANSFORMEE) {
            throw new IllegalStateException("Impossible de modifier une demande d'achat entièrement transformée en demande de prix.");
        }

        if (dto.getDemandeurNom() != null) da.setDemandeurNom(dto.getDemandeurNom());
        if (dto.getServiceDemandeur() != null) da.setServiceDemandeur(dto.getServiceDemandeur());
        if (dto.getObjet() != null) da.setObjet(dto.getObjet());
        if (dto.getPriorite() != null) da.setPriorite(dto.getPriorite());
        if (dto.getDateSouhaitee() != null) da.setDateSouhaitee(dto.getDateSouhaitee());
        if (dto.getObservations() != null) da.setObservations(dto.getObservations());
        if (dto.getStatut() != null) da.setStatut(dto.getStatut());

        if (dto.getLignes() != null) {
            da.getLignes().clear();
            for (LigneDemandeAchatDTO ligneDTO : dto.getLignes()) {
                LigneDemandeAchat ligne = new LigneDemandeAchat();
                ligne.setDemandeAchat(da);
                ligne.setPointDeVenteId(tenantId);
                ligne.setQuantiteDemandee(ligneDTO.getQuantiteDemandee() != null ? ligneDTO.getQuantiteDemandee() : BigDecimal.ONE);
                ligne.setUnite(ligneDTO.getUnite() != null ? ligneDTO.getUnite() : "U");
                ligne.setRemarques(ligneDTO.getRemarques());

                if (ligneDTO.getProduitId() != null) {
                    Produit prod = produitRepository.findById(ligneDTO.getProduitId()).orElse(null);
                    ligne.setProduit(prod);
                    ligne.setDesignation(prod != null && prod.getNom() != null ? prod.getNom() : ligneDTO.getDesignation());
                    ligne.setReference(prod != null ? prod.getReference() : ligneDTO.getReference());
                } else {
                    ligne.setDesignation(ligneDTO.getDesignation() != null ? ligneDTO.getDesignation() : "Article");
                    ligne.setReference(ligneDTO.getReference());
                }
                da.getLignes().add(ligne);
            }
        }

        DemandeAchat updated = demandeAchatRepository.save(da);
        return toDTO(updated);
    }

    @Transactional(readOnly = true)
    public List<DemandeAchatDTO> getAllDemandesAchat() {
        Long tenantId = getTenantId();
        return demandeAchatRepository.findByPointDeVenteIdOrderByDateDemandeDesc(tenantId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DemandeAchatDTO getDemandeAchatById(Long id) {
        Long tenantId = getTenantId();
        DemandeAchat da = demandeAchatRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'achat introuvable: " + id));
        return toDTO(da);
    }

    @Transactional
    public DemandeAchatDTO validerDemandeAchat(Long id) {
        Long tenantId = getTenantId();
        DemandeAchat da = demandeAchatRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'achat introuvable: " + id));
        da.setStatut(StatutDemandeAchat.VALIDEE);
        return toDTO(demandeAchatRepository.save(da));
    }

    @Transactional
    public DemandeAchatDTO annulerDemandeAchat(Long id) {
        Long tenantId = getTenantId();
        DemandeAchat da = demandeAchatRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'achat introuvable: " + id));
        da.setStatut(StatutDemandeAchat.ANNULEE);
        return toDTO(demandeAchatRepository.save(da));
    }

    /**
     * Action 1 : Transformer des articles choisis de la Demande d'Achat vers une Demande de Prix NATIONALE
     */
    @Transactional
    public DemandePrixDTO transformerEnDemandePrixNationale(Long id, List<Long> ligneIds, List<Long> fournisseurIds, LocalDateTime dateLimiteReponse) {
        return transformerEnDemandePrix(id, ligneIds, TypeAchat.NATIONAL, "MAD", null, null, fournisseurIds, dateLimiteReponse);
    }

    /**
     * Action 2 : Transformer des articles choisis de la Demande d'Achat vers une Demande de Prix INTERNATIONALE
     */
    @Transactional
    public DemandePrixDTO transformerEnDemandePrixInternationale(Long id,
                                                                List<Long> ligneIds,
                                                                String devise,
                                                                com.gestion.persistent.enums.Incoterm incoterm,
                                                                String lieuLivraison,
                                                                List<Long> fournisseurIds,
                                                                LocalDateTime dateLimiteReponse) {
        return transformerEnDemandePrix(id, ligneIds, TypeAchat.INTERNATIONAL, devise != null ? devise : "USD", incoterm, lieuLivraison, fournisseurIds, dateLimiteReponse);
    }

    private DemandePrixDTO transformerEnDemandePrix(Long daId,
                                                    List<Long> ligneIds,
                                                    TypeAchat typeAchat,
                                                    String devise,
                                                    com.gestion.persistent.enums.Incoterm incoterm,
                                                    String lieuLivraison,
                                                    List<Long> fournisseurIds,
                                                    LocalDateTime dateLimiteReponse) {
        Long tenantId = getTenantId();
        DemandeAchat da = demandeAchatRepository.findByIdAndPointDeVenteId(daId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'achat introuvable: " + daId));

        if (da.getStatut() == StatutDemandeAchat.ANNULEE) {
            throw new IllegalStateException("Impossible de transformer une demande d'achat annulée.");
        }

        // Filtrer les lignes à inclure dans cette DDP
        List<LigneDemandeAchat> lignesAConvertir;
        if (ligneIds != null && !ligneIds.isEmpty()) {
            lignesAConvertir = da.getLignes().stream()
                    .filter(l -> ligneIds.contains(l.getId()))
                    .collect(Collectors.toList());
        } else {
            // Par défaut toutes les lignes non encore entièrement traitées
            lignesAConvertir = da.getLignes().stream()
                    .filter(l -> l.getStatut() != com.gestion.persistent.enums.StatutLigneDemandeAchat.EN_COURS_DDP
                              && l.getStatut() != com.gestion.persistent.enums.StatutLigneDemandeAchat.COMMANDEE)
                    .collect(Collectors.toList());
        }

        if (lignesAConvertir.isEmpty()) {
            throw new IllegalStateException("Aucune ligne d'article sélectionnée ou disponible pour la création d'une demande de prix.");
        }

        DemandePrixDTO ddpDto = new DemandePrixDTO();
        ddpDto.setObjet((typeAchat == TypeAchat.INTERNATIONAL ? "[INTERNATIONAL] " : "[NATIONAL] ") + da.getObjet());
        ddpDto.setDemandeAchatId(da.getNumeroDa());
        ddpDto.setTypeDemandePrix(typeAchat);
        ddpDto.setDeviseSouhaitee(devise);
        ddpDto.setIncotermSouhaite(incoterm);
        ddpDto.setLieuLivraison(lieuLivraison);
        ddpDto.setDateLimiteReponse(dateLimiteReponse != null ? dateLimiteReponse : (da.getDateSouhaitee() != null ? da.getDateSouhaitee() : LocalDateTime.now().plusDays(15)));
        ddpDto.setObservations("Origine Demande d'Achat N° " + da.getNumeroDa() + " (Demandeur: " + (da.getDemandeurNom() != null ? da.getDemandeurNom() : "-") + ").\n" + (da.getObservations() != null ? da.getObservations() : ""));
        ddpDto.setFournisseurIds(fournisseurIds);

        // Copier les lignes sélectionnées
        for (LigneDemandeAchat lda : lignesAConvertir) {
            LigneDemandePrixDTO lDto = new LigneDemandePrixDTO();
            if (lda.getProduit() != null) {
                lDto.setProduitId(lda.getProduit().getId());
            }
            lDto.setReference(lda.getReference());
            lDto.setDesignation(lda.getDesignation());
            lDto.setQuantiteDemandee(lda.getQuantiteDemandee());
            lDto.setUnite(lda.getUnite());
            ddpDto.getLignes().add(lDto);

            // Marquer la ligne de la DA comme étant en cours de DDP
            lda.setQuantiteEnDdp(lda.getQuantiteDemandee());
            lda.setStatut(com.gestion.persistent.enums.StatutLigneDemandeAchat.EN_COURS_DDP);
        }

        DemandePrixDTO createdDdp = demandePrixService.createDemandePrix(ddpDto);

        // Mettre à jour la DA (dernière DDP et statut global)
        da.setDemandePrixId(createdDdp.getId());

        // Calculer si toutes les lignes de la DA sont maintenant traitées
        boolean toutesTraitees = da.getLignes().stream()
                .allMatch(l -> l.getStatut() == com.gestion.persistent.enums.StatutLigneDemandeAchat.EN_COURS_DDP
                            || l.getStatut() == com.gestion.persistent.enums.StatutLigneDemandeAchat.COMMANDEE
                            || l.getStatut() == com.gestion.persistent.enums.StatutLigneDemandeAchat.ANNULEE);

        if (toutesTraitees) {
            da.setStatut(StatutDemandeAchat.TOTALEMENT_TRANSFORMEE);
        } else {
            da.setStatut(StatutDemandeAchat.PARTIELLEMENT_TRANSFORMEE);
        }

        demandeAchatRepository.save(da);

        return createdDdp;
    }

    private DemandeAchatDTO toDTO(DemandeAchat da) {
        DemandeAchatDTO dto = new DemandeAchatDTO();
        dto.setId(da.getId());
        dto.setNumeroDa(da.getNumeroDa());
        dto.setDemandeurNom(da.getDemandeurNom());
        dto.setServiceDemandeur(da.getServiceDemandeur());
        dto.setObjet(da.getObjet());
        dto.setStatut(da.getStatut());
        dto.setPriorite(da.getPriorite());
        dto.setDateDemande(da.getDateDemande());
        dto.setDateSouhaitee(da.getDateSouhaitee());
        dto.setObservations(da.getObservations());
        dto.setDemandePrixId(da.getDemandePrixId());
        dto.setPointDeVenteId(da.getPointDeVenteId());

        if (da.getNumeroDa() != null) {
            List<DemandePrix> ddps = demandePrixRepository.findByPointDeVenteIdAndDemandeAchatId(da.getPointDeVenteId(), da.getNumeroDa());
            if (ddps != null && !ddps.isEmpty()) {
                dto.setDdpIds(ddps.stream().map(DemandePrix::getId).collect(Collectors.toList()));
                dto.setNumerosDdpAssociees(ddps.stream().map(DemandePrix::getNumeroDdp).collect(Collectors.toList()));
                dto.setNumeroDdpAssociee(ddps.get(ddps.size() - 1).getNumeroDdp());
            }
        }

        if (da.getLignes() != null) {
            dto.setLignes(da.getLignes().stream().map(l -> {
                LigneDemandeAchatDTO lDto = new LigneDemandeAchatDTO();
                lDto.setId(l.getId());
                lDto.setDemandeAchatId(da.getId());
                lDto.setProduitId(l.getProduit() != null ? l.getProduit().getId() : null);
                lDto.setReference(l.getReference());
                lDto.setDesignation(l.getDesignation());
                lDto.setQuantiteDemandee(l.getQuantiteDemandee());
                lDto.setQuantiteEnDdp(l.getQuantiteEnDdp());
                lDto.setStatut(l.getStatut());
                lDto.setUnite(l.getUnite());
                lDto.setRemarques(l.getRemarques());
                return lDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
