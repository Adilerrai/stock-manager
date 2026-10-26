package com.gestion.service;

import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.CurrentRequestContext;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.StatutDemandePrix;
import com.gestion.persistent.enums.StatutOffreFournisseur;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public DemandePrixService(DemandePrixRepository demandePrixRepository,
                              OffreFournisseurRepository offreFournisseurRepository,
                              FournisseurRepository fournisseurRepository,
                              ProduitRepository produitRepository,
                              CodificationService codificationService,
                              CommandeService commandeService) {
        this.demandePrixRepository = demandePrixRepository;
        this.offreFournisseurRepository = offreFournisseurRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.codificationService = codificationService;
        this.commandeService = commandeService;
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
     * Valide l'offre fournisseur gagnante et instancie automatiquement le Bon de Commande Fournisseur
     */
    @Transactional
    public Commande validerEtTransformerEnCommande(Long ddpId, Long offreId) {
        Long tenantId = getTenantId();
        DemandePrix ddp = demandePrixRepository.findByIdAndPointDeVenteId(ddpId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemandePrix", "id", ddpId));

        OffreFournisseur offreRetenue = ddp.getOffres().stream()
                .filter(o -> o.getId().equals(offreId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("OffreFournisseur", "id", offreId));

        // 1. Mettre à jour les statuts
        for (OffreFournisseur o : ddp.getOffres()) {
            if (o.getId().equals(offreId)) {
                o.setStatut(StatutOffreFournisseur.RETENUE);
            } else if (o.getStatut() == StatutOffreFournisseur.RECUE || o.getStatut() == StatutOffreFournisseur.EN_ATTENTE) {
                o.setStatut(StatutOffreFournisseur.REJETEE);
            }
        }
        ddp.setStatut(StatutDemandePrix.CLOTUREE);
        demandePrixRepository.save(ddp);

        // 2. Préparer CommandeDTO
        CommandeDTO cmdDTO = new CommandeDTO();
        cmdDTO.setFournisseurId(offreRetenue.getFournisseur().getId());
        cmdDTO.setAppliquerTVA(true);
        cmdDTO.setObservations("Émis suite à la DDP " + ddp.getNumeroDdp() + " (Devis Frn: " + (offreRetenue.getNumeroDevis() != null ? offreRetenue.getNumeroDevis() : "S/N") + ")");

        List<LigneCommandeDTO> lignesCommande = new ArrayList<>();
        for (LigneOffreFournisseur lof : offreRetenue.getLignes()) {
            LigneDemandePrix ldp = lof.getLigneDemandePrix();
            if (ldp.getProduit() != null) {
                LigneCommandeDTO lcmd = new LigneCommandeDTO();
                lcmd.setProduitId(ldp.getProduit().getId());
                lcmd.setDesignation(ldp.getDesignation());
                lcmd.setProduitReference(ldp.getReference());
                lcmd.setQuantiteCommandee(lof.getQuantiteProposee() != null ? lof.getQuantiteProposee().intValue() : 1);
                lcmd.setPrixUnitaire(lof.getPrixUnitaireHT());
                lcmd.setPrixUnitaireHT(lof.getPrixUnitaireHT());
                lcmd.setTauxTVA(lof.getTauxTVA());
                lignesCommande.add(lcmd);
            }
        }
        cmdDTO.setLignesCommande(lignesCommande);

        // 3. Appel de la création de commande existante
        return commandeService.createCommande(cmdDTO);
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
                lDto.setDisponibleStock(l.getDisponibleStock());
                lDto.setEstRetenue(l.getEstRetenue());
                lDto.setMontantTotalHT(l.getMontantTotalHT());
                return lDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
