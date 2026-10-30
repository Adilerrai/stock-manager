package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.CurrentRequestContext;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutDemandePrix;
import com.gestion.persistent.enums.StatutOffreFournisseur;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DemandePrixService {

    private static final Logger log = LoggerFactory.getLogger(DemandePrixService.class);

    private final DemandePrixRepository demandePrixRepository;
    private final OffreFournisseurRepository offreFournisseurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final CodificationService codificationService;
    private final CommandeService commandeService;
    private final EntrepriseProfileService entrepriseProfileService;

    public DemandePrixService(DemandePrixRepository demandePrixRepository,
                              OffreFournisseurRepository offreFournisseurRepository,
                              FournisseurRepository fournisseurRepository,
                              ProduitRepository produitRepository,
                              CodificationService codificationService,
                              CommandeService commandeService,
                              @Lazy EntrepriseProfileService entrepriseProfileService) {
        this.demandePrixRepository = demandePrixRepository;
        this.offreFournisseurRepository = offreFournisseurRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.codificationService = codificationService;
        this.commandeService = commandeService;
        this.entrepriseProfileService = entrepriseProfileService;
    }

    private Long getTenantId() {
        Long tenant = CurrentRequestContext.getSocieteId();
        if (tenant == null) {
            tenant = TenantContext.getCurrentTenant();
        }
        return tenant != null ? tenant : 1L;
    }

    @Transactional
    public DemandePrixDTO createDemandePrix(DemandePrixDTO dto) {
        Long tenantId = getTenantId();

        DemandePrix ddp = new DemandePrix();
        String numero = dto.getNumeroDdp();
        if (numero == null || numero.isBlank()) {
            numero = codificationService.genererNumero(TypeDocumentCodification.DEMANDE_PRIX, tenantId);
        }
        ddp.setNumeroDdp(numero);
        ddp.setObjet(dto.getObjet() != null ? dto.getObjet() : "Demande de prix " + numero);
        ddp.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutDemandePrix.BROUILLON);
        ddp.setDateCreation(LocalDateTime.now());
        ddp.setDateLimiteReponse(dto.getDateLimiteReponse());
        ddp.setDemandeAchatId(dto.getDemandeAchatId());
        ddp.setTypeDemandePrix(dto.getTypeDemandePrix() != null ? dto.getTypeDemandePrix() : com.gestion.persistent.enums.TypeAchat.NATIONAL);
        ddp.setDeviseSouhaitee(dto.getDeviseSouhaitee() != null ? dto.getDeviseSouhaitee() : "MAD");
        ddp.setIncotermSouhaite(dto.getIncotermSouhaite());
        ddp.setLieuLivraison(dto.getLieuLivraison());
        ddp.setObservations(dto.getObservations());
        ddp.setPointDeVenteId(tenantId);

        // Lignes d'articles demandés
        if (dto.getLignes() != null) {
            for (LigneDemandePrixDTO ligneDTO : dto.getLignes()) {
                LigneDemandePrix ligne = new LigneDemandePrix();
                ligne.setDemandePrix(ddp);
                ligne.setPointDeVenteId(tenantId);
                ligne.setQuantiteDemandee(ligneDTO.getQuantiteDemandee() != null ? ligneDTO.getQuantiteDemandee() : BigDecimal.ONE);
                ligne.setUnite(ligneDTO.getUnite() != null ? ligneDTO.getUnite() : "U");

                if (ligneDTO.getProduitId() != null) {
                    Produit prod = produitRepository.findById(ligneDTO.getProduitId()).orElse(null);
                    ligne.setProduit(prod);
                    ligne.setDesignation(prod != null && prod.getNom() != null ? prod.getNom() : ligneDTO.getDesignation());
                    ligne.setReference(prod != null ? prod.getReference() : ligneDTO.getReference());
                } else {
                    ligne.setDesignation(ligneDTO.getDesignation() != null ? ligneDTO.getDesignation() : "Article");
                    ligne.setReference(ligneDTO.getReference());
                }
                ddp.getLignes().add(ligne);
            }
        }

        // Fournisseurs invités (offres initiales en attente)
        if (dto.getFournisseurIds() != null && !dto.getFournisseurIds().isEmpty()) {
            for (Long fournisseurId : dto.getFournisseurIds()) {
                fournisseurRepository.findById(fournisseurId).ifPresent(frn -> {
                    OffreFournisseur offre = new OffreFournisseur();
                    offre.setDemandePrix(ddp);
                    offre.setFournisseur(frn);
                    offre.setStatut(StatutOffreFournisseur.EN_ATTENTE);
                    offre.setPointDeVenteId(tenantId);
                    ddp.getOffres().add(offre);
                });
            }
            if (ddp.getStatut() == StatutDemandePrix.BROUILLON) {
                ddp.setStatut(StatutDemandePrix.ENVOYEE);
            }
        }

        DemandePrix saved = demandePrixRepository.save(ddp);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DemandePrixDTO> getAllDemandesPrix() {
        Long tenantId = getTenantId();
        return demandePrixRepository.findByPointDeVenteIdOrderByDateCreationDesc(tenantId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DemandePrixDTO getDemandePrixById(Long id) {
        Long tenantId = getTenantId();
        DemandePrix ddp = demandePrixRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemandePrix", "id", id));
        return toDTO(ddp);
    }

    @Transactional
    public DemandePrixDTO updateDemandePrix(Long id, DemandePrixDTO dto) {
        Long tenantId = getTenantId();
        DemandePrix ddp = demandePrixRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemandePrix", "id", id));

        if (ddp.getStatut() == StatutDemandePrix.CLOTUREE || ddp.getStatut() == StatutDemandePrix.ANNULEE) {
            throw new CommonException("Impossible de modifier une demande de prix clôturée ou annulée.", HttpStatus.BAD_REQUEST);
        }

        boolean hasRetenue = ddp.getOffres().stream().anyMatch(o -> o.getStatut() == StatutOffreFournisseur.RETENUE);
        if (hasRetenue) {
            throw new CommonException("Impossible de modifier une demande de prix dont une offre a déjà été retenue et commandée.", HttpStatus.BAD_REQUEST);
        }

        // Modification manuelle du numéro de document
        if (dto.getNumeroDdp() != null && !dto.getNumeroDdp().isBlank()) {
            String nouveauNum = dto.getNumeroDdp().trim();
            if (!nouveauNum.equalsIgnoreCase(ddp.getNumeroDdp())) {
                if (entrepriseProfileService != null && !entrepriseProfileService.isModificationNumeroDocumentActive()) {
                    throw new CommonException("La modification du numéro de document n'est pas autorisée par la configuration de l'entreprise.", HttpStatus.BAD_REQUEST);
                }
                var existant = demandePrixRepository.findByPointDeVenteIdAndNumeroDdp(tenantId, nouveauNum);
                if (existant.isPresent() && !existant.get().getId().equals(ddp.getId())) {
                    throw new CommonException("Une demande de prix avec le numéro '" + nouveauNum + "' existe déjà.", HttpStatus.BAD_REQUEST);
                }
                ddp.setNumeroDdp(nouveauNum);
            }
        }

        if (dto.getObjet() != null) {
            ddp.setObjet(dto.getObjet());
        }
        if (dto.getDateLimiteReponse() != null) {
            ddp.setDateLimiteReponse(dto.getDateLimiteReponse());
        }
        ddp.setDemandeAchatId(dto.getDemandeAchatId());
        if (dto.getTypeDemandePrix() != null) {
            ddp.setTypeDemandePrix(dto.getTypeDemandePrix());
        }
        if (dto.getDeviseSouhaitee() != null) {
            ddp.setDeviseSouhaitee(dto.getDeviseSouhaitee());
        }
        ddp.setIncotermSouhaite(dto.getIncotermSouhaite());
        ddp.setLieuLivraison(dto.getLieuLivraison());
        ddp.setObservations(dto.getObservations());

        // 1. Synchronisation des lignes d'articles
        if (dto.getLignes() != null) {
            Set<Long> dtoLigneIds = dto.getLignes().stream()
                    .map(LigneDemandePrixDTO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Suppression des anciennes lignes
            List<LigneDemandePrix> linesToRemove = ddp.getLignes().stream()
                    .filter(l -> l.getId() != null && !dtoLigneIds.contains(l.getId()))
                    .collect(Collectors.toList());

            for (LigneDemandePrix lRem : linesToRemove) {
                for (OffreFournisseur ofr : ddp.getOffres()) {
                    ofr.getLignes().removeIf(lof -> lof.getLigneDemandePrix() != null
                            && lRem.getId().equals(lof.getLigneDemandePrix().getId()));
                }
            }
            ddp.getLignes().removeAll(linesToRemove);

            // Mise à jour ou ajout
            for (LigneDemandePrixDTO ligneDTO : dto.getLignes()) {
                LigneDemandePrix ligne;
                if (ligneDTO.getId() != null) {
                    ligne = ddp.getLignes().stream()
                            .filter(l -> ligneDTO.getId().equals(l.getId()))
                            .findFirst()
                            .orElse(null);
                    if (ligne == null) {
                        ligne = new LigneDemandePrix();
                        ligne.setDemandePrix(ddp);
                        ligne.setPointDeVenteId(tenantId);
                        ddp.getLignes().add(ligne);
                    }
                } else {
                    ligne = new LigneDemandePrix();
                    ligne.setDemandePrix(ddp);
                    ligne.setPointDeVenteId(tenantId);
                    ddp.getLignes().add(ligne);
                }

                ligne.setQuantiteDemandee(ligneDTO.getQuantiteDemandee() != null ? ligneDTO.getQuantiteDemandee() : BigDecimal.ONE);
                ligne.setUnite(ligneDTO.getUnite() != null ? ligneDTO.getUnite() : "U");

                if (ligneDTO.getProduitId() != null) {
                    Produit prod = produitRepository.findById(ligneDTO.getProduitId()).orElse(null);
                    ligne.setProduit(prod);
                    ligne.setDesignation(prod != null && prod.getNom() != null ? prod.getNom() : ligneDTO.getDesignation());
                    ligne.setReference(prod != null ? prod.getReference() : ligneDTO.getReference());
                } else {
                    ligne.setProduit(null);
                    ligne.setDesignation(ligneDTO.getDesignation() != null ? ligneDTO.getDesignation() : "Article");
                    ligne.setReference(ligneDTO.getReference());
                }
            }
        }

        // 2. Synchronisation des Fournisseurs / Offres
        if (dto.getFournisseurIds() != null) {
            Set<Long> targetFrnIds = new HashSet<>(dto.getFournisseurIds());

            List<OffreFournisseur> offresToRemove = ddp.getOffres().stream()
                    .filter(o -> o.getFournisseur() != null
                            && !targetFrnIds.contains(o.getFournisseur().getId())
                            && o.getStatut() == StatutOffreFournisseur.EN_ATTENTE)
                    .collect(Collectors.toList());
            ddp.getOffres().removeAll(offresToRemove);

            Set<Long> existingFrnIds = ddp.getOffres().stream()
                    .filter(o -> o.getFournisseur() != null)
                    .map(o -> o.getFournisseur().getId())
                    .collect(Collectors.toSet());

            for (Long frnId : targetFrnIds) {
                if (!existingFrnIds.contains(frnId)) {
                    fournisseurRepository.findById(frnId).ifPresent(frn -> {
                        OffreFournisseur offre = new OffreFournisseur();
                        offre.setDemandePrix(ddp);
                        offre.setFournisseur(frn);
                        offre.setStatut(StatutOffreFournisseur.EN_ATTENTE);
                        offre.setPointDeVenteId(tenantId);
                        ddp.getOffres().add(offre);
                    });
                }
            }

            if (!ddp.getOffres().isEmpty() && ddp.getStatut() == StatutDemandePrix.BROUILLON) {
                ddp.setStatut(StatutDemandePrix.ENVOYEE);
            }
        }

        DemandePrix saved = demandePrixRepository.save(ddp);
        return toDTO(saved);
    }

    @Transactional
    public void deleteDemandePrix(Long id) {
        Long tenantId = getTenantId();
        DemandePrix ddp = demandePrixRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemandePrix", "id", id));

        if (ddp.getStatut() == StatutDemandePrix.CLOTUREE) {
            throw new CommonException("Impossible de supprimer une demande de prix déjà clôturée.", HttpStatus.BAD_REQUEST);
        }
        boolean hasRetenue = ddp.getOffres().stream().anyMatch(o -> o.getStatut() == StatutOffreFournisseur.RETENUE);
        if (hasRetenue) {
            throw new CommonException("Impossible de supprimer une demande de prix dont une offre a déjà été retenue et commandée.", HttpStatus.BAD_REQUEST);
        }

        demandePrixRepository.delete(ddp);
    }

    @Transactional
    public OffreFournisseurDTO enregistrerOffreFournisseur(Long ddpId, OffreFournisseurDTO offreDTO) {
        Long tenantId = getTenantId();
        DemandePrix ddp = demandePrixRepository.findByIdAndPointDeVenteId(ddpId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemandePrix", "id", ddpId));

        Fournisseur fournisseur = fournisseurRepository.findById(offreDTO.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", offreDTO.getFournisseurId()));

        // Recherche d'une offre déjà initiée ou création d'une nouvelle
        OffreFournisseur offre = ddp.getOffres().stream()
                .filter(o -> o.getFournisseur().getId().equals(fournisseur.getId()))
                .findFirst()
                .orElseGet(() -> {
                    OffreFournisseur nouv = new OffreFournisseur();
                    nouv.setDemandePrix(ddp);
                    nouv.setFournisseur(fournisseur);
                    nouv.setPointDeVenteId(tenantId);
                    ddp.getOffres().add(nouv);
                    return nouv;
                });

        offre.setNumeroDevis(offreDTO.getNumeroDevis());
        offre.setDateReception(offreDTO.getDateReception() != null ? offreDTO.getDateReception() : LocalDateTime.now());
        offre.setDelaiLivraisonJours(offreDTO.getDelaiLivraisonJours() != null ? offreDTO.getDelaiLivraisonJours() : 0);
        offre.setFraisPort(offreDTO.getFraisPort() != null ? offreDTO.getFraisPort() : BigDecimal.ZERO);
        offre.setConditionsPaiement(offreDTO.getConditionsPaiement());
        offre.setObservations(offreDTO.getObservations());
        offre.setStatut(StatutOffreFournisseur.RECUE);

        if (offreDTO.getDevise() != null) offre.setDevise(offreDTO.getDevise());
        if (offreDTO.getTauxChangeEstime() != null) offre.setTauxChangeEstime(offreDTO.getTauxChangeEstime());
        if (offreDTO.getIncoterm() != null) offre.setIncoterm(offreDTO.getIncoterm());
        offre.setLieuIncoterm(offreDTO.getLieuIncoterm());
        offre.setPortEmbarquement(offreDTO.getPortEmbarquement());
        offre.setValiditeOffre(offreDTO.getValiditeOffre());
        offre.setCodeDouanierSh(offreDTO.getCodeDouanierSh());
        if (offreDTO.getAcompteExigePct() != null) offre.setAcompteExigePct(offreDTO.getAcompteExigePct());

        // Synchronisation des lignes de prix proposées
        offre.getLignes().clear();
        if (offreDTO.getLignes() != null) {
            for (LigneOffreFournisseurDTO lDto : offreDTO.getLignes()) {
                LigneDemandePrix ligneDdp = ddp.getLignes().stream()
                        .filter(l -> l.getId().equals(lDto.getLigneDemandePrixId()))
                        .findFirst()
                        .orElse(null);

                if (ligneDdp != null) {
                    LigneOffreFournisseur lOffre = new LigneOffreFournisseur();
                    lOffre.setOffreFournisseur(offre);
                    lOffre.setLigneDemandePrix(ligneDdp);
                    lOffre.setPrixUnitaireHT(lDto.getPrixUnitaireHT() != null ? lDto.getPrixUnitaireHT() : BigDecimal.ZERO);
                    lOffre.setRemisePourcentage(lDto.getRemisePourcentage() != null ? lDto.getRemisePourcentage() : BigDecimal.ZERO);
                    lOffre.setTauxTVA(lDto.getTauxTVA() != null ? lDto.getTauxTVA() : new BigDecimal("20.00"));
                    lOffre.setQuantiteProposee(lDto.getQuantiteProposee() != null ? lDto.getQuantiteProposee() : ligneDdp.getQuantiteDemandee());
                    lOffre.setPrixUnitaireDevise(lDto.getPrixUnitaireDevise());
                    lOffre.setCodeSh(lDto.getCodeSh());
                    lOffre.setDisponibleStock(lDto.getDisponibleStock() != null ? lDto.getDisponibleStock() : true);
                    lOffre.setPointDeVenteId(tenantId);
                    offre.getLignes().add(lOffre);
                }
            }
        }

        // Passage de statut DDP à REPONSES_RECUES
        if (ddp.getStatut() == StatutDemandePrix.BROUILLON || ddp.getStatut() == StatutDemandePrix.ENVOYEE) {
            ddp.setStatut(StatutDemandePrix.REPONSES_RECUES);
        }

        demandePrixRepository.save(ddp);
        return toOffreDTO(offre);
    }

    /**
     * Génère la matrice d'alignement des offres pour le comparateur UI
     */
    @Transactional(readOnly = true)
    public ComparateurOffresDTO getComparateur(Long ddpId) {
        Long tenantId = getTenantId();
        DemandePrix ddp = demandePrixRepository.findByIdAndPointDeVenteId(ddpId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemandePrix", "id", ddpId));

        ComparateurOffresDTO dto = new ComparateurOffresDTO();
        dto.setDemandePrixId(ddp.getId());
        dto.setNumeroDdp(ddp.getNumeroDdp());
        dto.setObjet(ddp.getObjet());
        dto.setStatut(ddp.getStatut().name());

        // 1. Articles en lignes
        List<ComparateurOffresDTO.ArticleLigneComparatif> articles = new ArrayList<>();
        Map<Long, ComparateurOffresDTO.ArticleLigneComparatif> articleMap = new HashMap<>();

        for (LigneDemandePrix ldp : ddp.getLignes()) {
            ComparateurOffresDTO.ArticleLigneComparatif art = new ComparateurOffresDTO.ArticleLigneComparatif();
            art.setLigneDdpId(ldp.getId());
            art.setProduitId(ldp.getProduit() != null ? ldp.getProduit().getId() : null);
            art.setReference(ldp.getReference());
            art.setDesignation(ldp.getDesignation());
            art.setQuantiteDemandee(ldp.getQuantiteDemandee());
            art.setUnite(ldp.getUnite());
            articles.add(art);
            articleMap.put(ldp.getId(), art);
        }
        dto.setArticles(articles);

        // 2. Offres fournisseurs en colonnes
        List<ComparateurOffresDTO.FournisseurColonneComparatif> colonnes = new ArrayList<>();
        Long meilleureOffreId = null;
        BigDecimal minTotalGlobal = null;
        BigDecimal maxTotalGlobal = null;

        for (OffreFournisseur offre : ddp.getOffres()) {
            ComparateurOffresDTO.FournisseurColonneComparatif col = new ComparateurOffresDTO.FournisseurColonneComparatif();
            col.setOffreId(offre.getId());
            col.setFournisseurId(offre.getFournisseur().getId());
            col.setNomFournisseur(offre.getFournisseur().getNom());
            col.setNumeroDevis(offre.getNumeroDevis());
            col.setDelaiJours(offre.getDelaiLivraisonJours());
            col.setFraisPort(offre.getFraisPort());
            col.setConditionsPaiement(offre.getConditionsPaiement());
            col.setStatut(offre.getStatut().name());

            BigDecimal totalHT = offre.calculateTotalHT();
            col.setTotalHT(totalHT);

            // Détection offre globale moins-disante
            if (offre.getStatut() == StatutOffreFournisseur.RECUE || offre.getStatut() == StatutOffreFournisseur.RETENUE) {
                if (minTotalGlobal == null || totalHT.compareTo(minTotalGlobal) < 0) {
                    minTotalGlobal = totalHT;
                    meilleureOffreId = offre.getId();
                }
                if (maxTotalGlobal == null || totalHT.compareTo(maxTotalGlobal) > 0) {
                    maxTotalGlobal = totalHT;
                }
            }

            // Cellules de prix par article
            List<ComparateurOffresDTO.PrixArticleCellule> cellules = new ArrayList<>();
            for (LigneOffreFournisseur lof : offre.getLignes()) {
                ComparateurOffresDTO.PrixArticleCellule cel = new ComparateurOffresDTO.PrixArticleCellule();
                cel.setLigneDdpId(lof.getLigneDemandePrix().getId());
                cel.setPrixUnitaireHT(lof.getPrixUnitaireHT());
                cel.setRemisePourcentage(lof.getRemisePourcentage());
                cel.setQuantiteProposee(lof.getQuantiteProposee());
                cel.setDisponibleStock(lof.getDisponibleStock());

                // Mettre à jour le prix minimum par article (Moins-disant unitaire)
                ComparateurOffresDTO.ArticleLigneComparatif artComp = articleMap.get(lof.getLigneDemandePrix().getId());
                if (artComp != null && lof.getPrixUnitaireHT() != null && lof.getPrixUnitaireHT().compareTo(BigDecimal.ZERO) > 0) {
                    if (artComp.getPrixUnitaireMinimum() == null || lof.getPrixUnitaireHT().compareTo(artComp.getPrixUnitaireMinimum()) < 0) {
                        artComp.setPrixUnitaireMinimum(lof.getPrixUnitaireHT());
                        artComp.setMeilleurFournisseurId(offre.getFournisseur().getId());
                    }
                }
                cellules.add(cel);
            }
            col.setPropositions(cellules);
            colonnes.add(col);
        }

        // Marquer les cellules d'articles les moins chers
        for (ComparateurOffresDTO.FournisseurColonneComparatif col : colonnes) {
            for (ComparateurOffresDTO.PrixArticleCellule cel : col.getPropositions()) {
                ComparateurOffresDTO.ArticleLigneComparatif artComp = articleMap.get(cel.getLigneDdpId());
                if (artComp != null && artComp.getPrixUnitaireMinimum() != null && cel.getPrixUnitaireHT() != null) {
                    cel.setEstMoinsCher(cel.getPrixUnitaireHT().compareTo(artComp.getPrixUnitaireMinimum()) == 0);
                } else {
                    cel.setEstMoinsCher(false);
                }
            }
        }

        dto.setOffres(colonnes);
        dto.setMeilleureOffreGlobaleId(meilleureOffreId);
        dto.setMeilleurMontantTotalHT(minTotalGlobal);
        if (maxTotalGlobal != null && minTotalGlobal != null && maxTotalGlobal.compareTo(minTotalGlobal) > 0) {
            dto.setEconomieEstimee(maxTotalGlobal.subtract(minTotalGlobal).setScale(2, RoundingMode.HALF_UP));
        } else {
            dto.setEconomieEstimee(BigDecimal.ZERO);
        }

        return dto;
    }

    /**
     * Valide l'offre fournisseur gagnante et instancie automatiquement le Bon de Commande Fournisseur (ou l'ajoute à une commande brouillon existante)
     */
    @Transactional
    public Commande validerEtTransformerEnCommande(Long ddpId, Long offreId) {
        return validerEtTransformerEnCommande(ddpId, offreId, null);
    }

    /**
     * Valide l'offre fournisseur et l'intègre dans une commande fournisseur existante au statut BROUILLON (si commandeId != null),
     * ou instancie une nouvelle commande fournisseur (si commandeId == null).
     */
    @Transactional
    public Commande validerEtTransformerEnCommande(Long ddpId, Long offreId, Long commandeId) {
        Long tenantId = getTenantId();
        DemandePrix ddp = demandePrixRepository.findByIdAndPointDeVenteId(ddpId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemandePrix", "id", ddpId));

        OffreFournisseur offreRetenue = ddp.getOffres().stream()
                .filter(o -> o.getId().equals(offreId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("OffreFournisseur", "id", offreId));

        // 1. Mettre à jour les statuts de l'offre et de la DDP
        for (OffreFournisseur o : ddp.getOffres()) {
            if (o.getId().equals(offreId)) {
                o.setStatut(StatutOffreFournisseur.RETENUE);
                if (o.getLignes() != null) {
                    o.getLignes().forEach(l -> l.setEstRetenue(true));
                }
            } else if (o.getStatut() == StatutOffreFournisseur.RECUE || o.getStatut() == StatutOffreFournisseur.EN_ATTENTE) {
                o.setStatut(StatutOffreFournisseur.REJETEE);
            }
        }
        ddp.setStatut(StatutDemandePrix.CLOTUREE);
        demandePrixRepository.save(ddp);

        // 2. Extraire les lignes
        List<LigneCommandeDTO> lignesCommande = extraireLignesCommande(offreRetenue);
        String refObs = "DDP " + ddp.getNumeroDdp() + (offreRetenue.getNumeroDevis() != null ? " (Devis: " + offreRetenue.getNumeroDevis() + ")" : "");

        // 3. Intégration dans commande existante brouillon
        if (commandeId != null) {
            Commande commandeExistante = commandeService.getCommandeById(commandeId);
            if (commandeExistante.getStatut() != StatutCommande.BROUILLON) {
                throw new CommonException("Impossible d'ajouter à la commande #" + commandeExistante.getNumeroCommande() + 
                        " car elle n'est pas au statut BROUILLON.", HttpStatus.BAD_REQUEST);
            }
            if (!commandeExistante.getFournisseur().getId().equals(offreRetenue.getFournisseur().getId())) {
                throw new CommonException("Le fournisseur de la commande (" + commandeExistante.getFournisseur().getNom() + 
                        ") ne correspond pas au fournisseur de l'offre (" + offreRetenue.getFournisseur().getNom() + ").", HttpStatus.BAD_REQUEST);
            }
            return commandeService.ajouterLignesACommandeBrouillon(commandeId, lignesCommande, refObs);
        }

        // 4. Création d'une nouvelle commande fournisseur
        CommandeDTO cmdDTO = new CommandeDTO();
        cmdDTO.setFournisseurId(offreRetenue.getFournisseur().getId());
        cmdDTO.setAppliquerTVA(true);
        cmdDTO.setObservations("Émis suite à la " + refObs);
        cmdDTO.setLignesCommande(lignesCommande);

        if (ddp.getTypeDemandePrix() == com.gestion.persistent.enums.TypeAchat.INTERNATIONAL ||
            offreRetenue.getIncoterm() != null || 
            (offreRetenue.getDevise() != null && !offreRetenue.getDevise().equals("MAD"))) {
            cmdDTO.setTypeAchat(com.gestion.persistent.enums.TypeAchat.INTERNATIONAL);
            cmdDTO.setDevise(offreRetenue.getDevise() != null ? offreRetenue.getDevise() : ddp.getDeviseSouhaitee());
            cmdDTO.setIncoterm(offreRetenue.getIncoterm() != null ? offreRetenue.getIncoterm() : ddp.getIncotermSouhaite());
            cmdDTO.setLieuIncoterm(offreRetenue.getLieuIncoterm() != null ? offreRetenue.getLieuIncoterm() : ddp.getLieuLivraison());
            cmdDTO.setPortEmbarquement(offreRetenue.getPortEmbarquement());
            if (offreRetenue.getTauxChangeEstime() != null) {
                cmdDTO.setTauxChangeCommande(offreRetenue.getTauxChangeEstime());
            }
        }

        return commandeService.createCommande(cmdDTO);
    }

    /**
     * Ajoute plusieurs offres fournisseurs (issues d'une ou plusieurs demandes de prix) dans une seule commande fournisseur au statut BROUILLON
     */
    @Transactional
    public Commande ajouterOffresACommandeBrouillon(Long commandeId, List<Long> offreIds) {
        if (commandeId == null) {
            throw new CommonException("L'identifiant de la commande fournisseur brouillon est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        if (offreIds == null || offreIds.isEmpty()) {
            throw new CommonException("Veuillez sélectionner au moins une offre fournisseur à intégrer.", HttpStatus.BAD_REQUEST);
        }

        Long tenantId = getTenantId();
        Commande commande = commandeService.getCommandeById(commandeId);
        if (commande.getStatut() != StatutCommande.BROUILLON) {
            throw new CommonException("La commande #" + commande.getNumeroCommande() + " n'est pas au statut BROUILLON (statut actuel: " + commande.getStatut() + ").", HttpStatus.BAD_REQUEST);
        }

        Long fournisseurId = commande.getFournisseur().getId();
        List<LigneCommandeDTO> toutesLesLignes = new ArrayList<>();
        List<String> refsDdp = new ArrayList<>();

        for (Long offreId : offreIds) {
            OffreFournisseur offre = offreFournisseurRepository.findByIdAndPointDeVenteId(offreId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("OffreFournisseur", "id", offreId));

            if (!offre.getFournisseur().getId().equals(fournisseurId)) {
                throw new CommonException("L'offre #" + offreId + " (Fournisseur: " + offre.getFournisseur().getNom() + 
                        ") ne correspond pas au fournisseur de la commande (" + commande.getFournisseur().getNom() + ").", HttpStatus.BAD_REQUEST);
            }

            // Marquer l'offre comme retenue et clôturer la DDP
            DemandePrix ddp = offre.getDemandePrix();
            if (ddp != null) {
                for (OffreFournisseur o : ddp.getOffres()) {
                    if (o.getId().equals(offreId)) {
                        o.setStatut(StatutOffreFournisseur.RETENUE);
                        if (o.getLignes() != null) {
                            o.getLignes().forEach(l -> l.setEstRetenue(true));
                        }
                    } else if (o.getStatut() == StatutOffreFournisseur.RECUE || o.getStatut() == StatutOffreFournisseur.EN_ATTENTE) {
                        o.setStatut(StatutOffreFournisseur.REJETEE);
                    }
                }
                ddp.setStatut(StatutDemandePrix.CLOTUREE);
                demandePrixRepository.save(ddp);

                String ref = "DDP " + ddp.getNumeroDdp() + (offre.getNumeroDevis() != null ? " (Devis: " + offre.getNumeroDevis() + ")" : "");
                if (!refsDdp.contains(ref)) {
                    refsDdp.add(ref);
                }
            }

            toutesLesLignes.addAll(extraireLignesCommande(offre));
        }

        String complementObs = String.join(" | ", refsDdp);
        return commandeService.ajouterLignesACommandeBrouillon(commandeId, toutesLesLignes, complementObs);
    }

    /**
     * Fusionne plusieurs offres fournisseurs vers une commande fournisseur :
     * - Si dto.getCommandeId() est renseigné : ajoute les offres à cette commande brouillon existante
     * - Si dto.getCommandeId() est null : crée une nouvelle commande brouillon pour le fournisseur et y ajoute toutes les offres
     */
    @Transactional
    public Commande fusionnerOffresVersCommande(FusionOffresFournisseurDTO dto) {
        if (dto.getOffreIds() == null || dto.getOffreIds().isEmpty()) {
            throw new CommonException("Veuillez sélectionner au moins une offre fournisseur à fusionner.", HttpStatus.BAD_REQUEST);
        }

        Long tenantId = getTenantId();

        // 1. Si une commande brouillon existe déjà :
        if (dto.getCommandeId() != null) {
            return ajouterOffresACommandeBrouillon(dto.getCommandeId(), dto.getOffreIds());
        }

        // 2. Sinon, créer d'abord la commande brouillon avec la 1ère offre, puis ajouter les autres
        Long premiereOffreId = dto.getOffreIds().get(0);
        OffreFournisseur premiereOffre = offreFournisseurRepository.findByIdAndPointDeVenteId(premiereOffreId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("OffreFournisseur", "id", premiereOffreId));

        DemandePrix premiereDdp = premiereOffre.getDemandePrix();
        Commande nouvelleCommande = validerEtTransformerEnCommande(premiereDdp.getId(), premiereOffreId, null);

        // Si d'autres offres sont sélectionnées, les ajouter à la commande créée
        if (dto.getOffreIds().size() > 1) {
            List<Long> autresOffres = dto.getOffreIds().subList(1, dto.getOffreIds().size());
            nouvelleCommande = ajouterOffresACommandeBrouillon(nouvelleCommande.getId(), autresOffres);
        }

        return nouvelleCommande;
    }

    /**
     * Liste les offres fournisseurs disponibles pour un fournisseur donné (avec référence DDP)
     */
    public List<OffreFournisseurDTO> getOffresParFournisseur(Long fournisseurId) {
        Long tenantId = getTenantId();
        List<OffreFournisseur> offres = offreFournisseurRepository.findByFournisseurIdAndPointDeVenteId(fournisseurId, tenantId);
        return offres.stream().map(this::toOffreDTO).collect(Collectors.toList());
    }

    private List<LigneCommandeDTO> extraireLignesCommande(OffreFournisseur offre) {
        List<LigneCommandeDTO> lignesCommande = new ArrayList<>();
        if (offre.getLignes() == null) {
            return lignesCommande;
        }

        for (LigneOffreFournisseur lof : offre.getLignes()) {
            LigneDemandePrix ldp = lof.getLigneDemandePrix();
            if (ldp != null && ldp.getProduit() != null) {
                LigneCommandeDTO lcmd = new LigneCommandeDTO();
                lcmd.setProduitId(ldp.getProduit().getId());
                lcmd.setDesignation(ldp.getDesignation());
                lcmd.setProduitReference(ldp.getReference());
                lcmd.setQuantiteCommandee(lof.getQuantiteProposee() != null ? lof.getQuantiteProposee().intValue() : 1);
                lcmd.setPrixUnitaire(lof.getPrixUnitaireHT());
                lcmd.setPrixUnitaireHT(lof.getPrixUnitaireHT());
                lcmd.setTauxTVA(lof.getTauxTVA());
                lcmd.setPrixUnitaireDevise(lof.getPrixUnitaireDevise());
                lignesCommande.add(lcmd);
            }
        }
        return lignesCommande;
    }

    // ─── Helpers de mapping ──────────────────────────────────────────────────
    private DemandePrixDTO toDTO(DemandePrix entity) {
        DemandePrixDTO dto = new DemandePrixDTO();
        dto.setId(entity.getId());
        dto.setNumeroDdp(entity.getNumeroDdp());
        dto.setObjet(entity.getObjet());
        dto.setStatut(entity.getStatut());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateLimiteReponse(entity.getDateLimiteReponse());
        dto.setDemandeAchatId(entity.getDemandeAchatId());
        dto.setTypeDemandePrix(entity.getTypeDemandePrix());
        dto.setDeviseSouhaitee(entity.getDeviseSouhaitee());
        dto.setIncotermSouhaite(entity.getIncotermSouhaite());
        dto.setLieuLivraison(entity.getLieuLivraison());
        dto.setObservations(entity.getObservations());
        dto.setPointDeVenteId(entity.getPointDeVenteId());

        if (entity.getLignes() != null) {
            dto.setLignes(entity.getLignes().stream().map(this::toLigneDTO).collect(Collectors.toList()));
        }
        if (entity.getOffres() != null) {
            dto.setOffres(entity.getOffres().stream().map(this::toOffreDTO).collect(Collectors.toList()));
            dto.setFournisseurIds(entity.getOffres().stream()
                    .filter(o -> o.getFournisseur() != null)
                    .map(o -> o.getFournisseur().getId())
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private LigneDemandePrixDTO toLigneDTO(LigneDemandePrix l) {
        LigneDemandePrixDTO dto = new LigneDemandePrixDTO();
        dto.setId(l.getId());
        dto.setDemandePrixId(l.getDemandePrix() != null ? l.getDemandePrix().getId() : null);
        dto.setProduitId(l.getProduit() != null ? l.getProduit().getId() : null);
        dto.setReference(l.getReference());
        dto.setDesignation(l.getDesignation());
        dto.setQuantiteDemandee(l.getQuantiteDemandee());
        dto.setUnite(l.getUnite());
        return dto;
    }

    private OffreFournisseurDTO toOffreDTO(OffreFournisseur o) {
        OffreFournisseurDTO dto = new OffreFournisseurDTO();
        dto.setId(o.getId());
        dto.setDemandePrixId(o.getDemandePrix() != null ? o.getDemandePrix().getId() : null);
        if (o.getDemandePrix() != null) {
            dto.setDdpNumero(o.getDemandePrix().getNumeroDdp());
            dto.setDdpObjet(o.getDemandePrix().getObjet());
        }
        dto.setFournisseurId(o.getFournisseur() != null ? o.getFournisseur().getId() : null);
        dto.setFournisseurNom(o.getFournisseur() != null ? o.getFournisseur().getNom() : null);
        dto.setNumeroDevis(o.getNumeroDevis());
        dto.setDateReception(o.getDateReception());
        dto.setDelaiLivraisonJours(o.getDelaiLivraisonJours());
        dto.setFraisPort(o.getFraisPort());
        dto.setStatut(o.getStatut());
        dto.setConditionsPaiement(o.getConditionsPaiement());
        dto.setObservations(o.getObservations());
        dto.setTotalHT(o.calculateTotalHT());

        dto.setDevise(o.getDevise());
        dto.setTauxChangeEstime(o.getTauxChangeEstime());
        dto.setIncoterm(o.getIncoterm());
        dto.setLieuIncoterm(o.getLieuIncoterm());
        dto.setPortEmbarquement(o.getPortEmbarquement());
        dto.setValiditeOffre(o.getValiditeOffre());
        dto.setCodeDouanierSh(o.getCodeDouanierSh());
        dto.setAcompteExigePct(o.getAcompteExigePct());

        if (o.getLignes() != null) {
            dto.setLignes(o.getLignes().stream().map(l -> {
                LigneOffreFournisseurDTO lDto = new LigneOffreFournisseurDTO();
                lDto.setId(l.getId());
                lDto.setOffreFournisseurId(o.getId());
                lDto.setLigneDemandePrixId(l.getLigneDemandePrix() != null ? l.getLigneDemandePrix().getId() : null);
                if (l.getLigneDemandePrix() != null) {
                    lDto.setDesignation(l.getLigneDemandePrix().getDesignation());
                    if (l.getLigneDemandePrix().getProduit() != null) {
                        lDto.setProduitId(l.getLigneDemandePrix().getProduit().getId());
                    }
                }
                lDto.setPrixUnitaireHT(l.getPrixUnitaireHT());
                lDto.setRemisePourcentage(l.getRemisePourcentage());
                lDto.setTauxTVA(l.getTauxTVA());
                lDto.setQuantiteProposee(l.getQuantiteProposee());
                lDto.setPrixUnitaireDevise(l.getPrixUnitaireDevise());
                lDto.setCodeSh(l.getCodeSh());
                lDto.setDisponibleStock(l.getDisponibleStock());
                lDto.setEstRetenue(l.getEstRetenue());
                lDto.setMontantTotalHT(l.getMontantTotalHT());
                return lDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
