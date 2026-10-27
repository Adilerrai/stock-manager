package com.gestion.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import org.springframework.http.HttpStatus;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import com.gestion.mapper.LivraisonMapper;
import com.gestion.mapper.ProduitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.InputStream;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JREmptyDataSource;

@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final LivraisonService livraisonService;
    private final LivraisonRepository livraisonRepository;
    private final LivraisonMapper livraisonMapper;
    private final ProduitMapper produitMapper;
    private final CodificationService codificationService;
    private final EntrepriseProfileService entrepriseProfileService;
    private final ImpressionService impressionService;
    private final LieuLogistiqueRepository lieuLogistiqueRepository;
    private final ConditionPaiementRepository conditionPaiementRepository;
    private final CoutRevientService coutRevientService;

    public CommandeService(CommandeRepository commandeRepository,
                          LigneCommandeRepository ligneCommandeRepository,
                          FournisseurRepository fournisseurRepository,
                          ProduitRepository produitRepository,
                          LivraisonService livraisonService,
                          LivraisonRepository livraisonRepository,
                          LivraisonMapper livraisonMapper,
                          ProduitMapper produitMapper,
                          CodificationService codificationService) {
        this(commandeRepository, ligneCommandeRepository, fournisseurRepository, produitRepository,
             livraisonService, livraisonRepository, livraisonMapper, produitMapper, codificationService,
             null, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CommandeService(CommandeRepository commandeRepository,
                          LigneCommandeRepository ligneCommandeRepository,
                          FournisseurRepository fournisseurRepository,
                          ProduitRepository produitRepository,
                          LivraisonService livraisonService,
                          LivraisonRepository livraisonRepository,
                          LivraisonMapper livraisonMapper,
                          ProduitMapper produitMapper,
                          CodificationService codificationService,
                          EntrepriseProfileService entrepriseProfileService,
                          @org.springframework.context.annotation.Lazy ImpressionService impressionService,
                          LieuLogistiqueRepository lieuLogistiqueRepository,
                          ConditionPaiementRepository conditionPaiementRepository,
                          @org.springframework.context.annotation.Lazy CoutRevientService coutRevientService) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.livraisonService = livraisonService;
        this.livraisonRepository = livraisonRepository;
        this.livraisonMapper = livraisonMapper;
        this.produitMapper = produitMapper;
        this.codificationService = codificationService;
        this.entrepriseProfileService = entrepriseProfileService;
        this.impressionService = impressionService;
        this.lieuLogistiqueRepository = lieuLogistiqueRepository;
        this.conditionPaiementRepository = conditionPaiementRepository;
        this.coutRevientService = coutRevientService;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional
    public Commande createCommande(CommandeDTO commandeDTO) {

        Fournisseur fournisseur = fournisseurRepository.findById(
                commandeDTO.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", commandeDTO.getFournisseurId()));

        Commande commande = new Commande();
        Long tenantId = getTenantId();
        if (commandeDTO.getNumeroCommande() != null && !commandeDTO.getNumeroCommande().isBlank()) {
            String customNum = commandeDTO.getNumeroCommande().trim();
            if (entrepriseProfileService != null && !entrepriseProfileService.isModificationNumeroDocumentActive()) {
                throw new CommonException("La saisie manuelle du numéro de commande fournisseur n'est pas autorisée par la configuration de l'entreprise.", HttpStatus.BAD_REQUEST);
            }
            var existant = commandeRepository.findByPointDeVenteIdAndNumeroCommande(tenantId, customNum);
            if (existant.isPresent()) {
                throw new CommonException("Une commande fournisseur avec le numéro '" + customNum + "' existe déjà.", HttpStatus.BAD_REQUEST);
            }
            commande.setNumeroCommande(customNum);
        } else {
            commande.setNumeroCommande(generateNumeroCommande());
        }
        commande.setFournisseur(fournisseur);
        commande.setStatut(StatutCommande.BROUILLON);
        commande.setDateLivraisonPrevue(commandeDTO.getDateLivraisonPrevue());
        commande.setObservations(commandeDTO.getObservations());
        commande.setPointDeVenteId(tenantId);

        boolean isInternational = fournisseur.getTypeFournisseur() == com.gestion.persistent.enums.TypeFournisseur.INTERNATIONAL
                || commandeDTO.getTypeAchat() == com.gestion.persistent.enums.TypeAchat.INTERNATIONAL;
        commande.setTypeAchat(isInternational ? com.gestion.persistent.enums.TypeAchat.INTERNATIONAL : com.gestion.persistent.enums.TypeAchat.NATIONAL);

        // Paramètres import / international
        if (isInternational) {
            commande.setIncoterm(commandeDTO.getIncoterm() != null ? commandeDTO.getIncoterm()
                    : (fournisseur.getIncotermDefaut() != null ? fournisseur.getIncotermDefaut() : com.gestion.persistent.enums.Incoterm.FOB));
            commande.setLieuIncoterm(commandeDTO.getLieuIncoterm());
            commande.setPortEmbarquement(commandeDTO.getPortEmbarquement() != null ? commandeDTO.getPortEmbarquement() : fournisseur.getPortEmbarquementDefaut());
            if (commandeDTO.getLieuDestinationId() != null && lieuLogistiqueRepository != null) {
                lieuLogistiqueRepository.findById(commandeDTO.getLieuDestinationId()).ifPresent(commande::setLieuDestination);
            }
            commande.setModeTransport(commandeDTO.getModeTransport() != null ? commandeDTO.getModeTransport() : com.gestion.persistent.enums.ModeTransport.MARITIME);
            Long conditionId = commandeDTO.getConditionPaiementId() != null ? commandeDTO.getConditionPaiementId() : fournisseur.getConditionPaiementDefautId();
            if (conditionId != null && conditionPaiementRepository != null) {
                conditionPaiementRepository.findById(conditionId).ifPresent(commande::setConditionPaiement);
            }
            commande.setTauxChangeCommande(commandeDTO.getTauxChangeCommande());
            commande.setTauxChangeDouane(commandeDTO.getTauxChangeDouane());
            commande.setNumeroBl(commandeDTO.getNumeroBl());
            commande.setNumeroConteneur(commandeDTO.getNumeroConteneur());
            commande.setNavire(commandeDTO.getNavire());
            commande.setDateEtd(commandeDTO.getDateEtd());
            commande.setDateEta(commandeDTO.getDateEta());
            commande.setNumeroDum(commandeDTO.getNumeroDum());
            commande.setDateDum(commandeDTO.getDateDum());
        }

        String devise = (commandeDTO.getDevise() != null && !commandeDTO.getDevise().isBlank())
                ? commandeDTO.getDevise()
                : (fournisseur.getDevise() != null ? fournisseur.getDevise() : "MAD");
        commande.setDevise(devise);

        // Si le fournisseur est international, la TVA marocaine n'est pas applicable (0%)
        boolean appliquerTva = !isInternational && Boolean.TRUE.equals(commandeDTO.getAppliquerTVA());
        commande.setAppliquerTVA(appliquerTva);
        commande.setTauxTVA(appliquerTva ? (commandeDTO.getTauxTVA() != null ? commandeDTO.getTauxTVA() : new BigDecimal("20.00")) : BigDecimal.ZERO);

        commande = commandeRepository.save(commande);

        // Créer les lignes de commande
        BigDecimal montantHTTotal = BigDecimal.ZERO;
        BigDecimal montantTVATotal = BigDecimal.ZERO;
        BigDecimal montantTTCTotal = BigDecimal.ZERO;

        if (commandeDTO.getLignesCommande() != null) {
            for (LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                LigneCommande ligne = createLigneCommande(commande, ligneDTO, appliquerTva, commande.getTauxTVA());
                if (ligne.getMontantHT() != null) {
                    montantHTTotal = montantHTTotal.add(ligne.getMontantHT());
                }
                if (ligne.getMontantTVA() != null) {
                    montantTVATotal = montantTVATotal.add(ligne.getMontantTVA());
                }
                if (ligne.getMontantTTC() != null) {
                    montantTTCTotal = montantTTCTotal.add(ligne.getMontantTTC());
                } else if (ligne.getMontantLigne() != null) {
                    montantTTCTotal = montantTTCTotal.add(ligne.getMontantLigne());
                }
            }
        }

        BigDecimal finalHT = commandeDTO.getMontantHT() != null ? commandeDTO.getMontantHT() : montantHTTotal;
        BigDecimal finalTVA = appliquerTva ? (commandeDTO.getMontantTVA() != null ? commandeDTO.getMontantTVA() : montantTVATotal) : BigDecimal.ZERO;
        BigDecimal finalTTC = appliquerTva 
                ? (commandeDTO.getMontantTTC() != null ? commandeDTO.getMontantTTC() : (commandeDTO.getMontantTotal() != null ? commandeDTO.getMontantTotal() : montantTTCTotal))
                : (commandeDTO.getMontantTotal() != null ? commandeDTO.getMontantTotal() : (commandeDTO.getMontantHT() != null ? commandeDTO.getMontantHT() : finalHT));

        if (!appliquerTva && commandeDTO.getMontantTotal() != null) {
            finalHT = commandeDTO.getMontantHT() != null ? commandeDTO.getMontantHT() : commandeDTO.getMontantTotal();
            finalTTC = commandeDTO.getMontantTotal();
        }

        commande.setMontantHT(finalHT);
        commande.setMontantTVA(finalTVA);
        commande.setMontantTTC(finalTTC);
        commande.setMontantTotal(finalTTC);

        commande = commandeRepository.save(commande);

        // Déclencher le calcul du coût de revient
        if (coutRevientService != null) {
            try {
                coutRevientService.recalculerEtPersister(commande.getId());
                commande = commandeRepository.findById(commande.getId()).orElse(commande);
            } catch (Exception e) {
                // Silencieux si pas de charges initiales
            }
        }

        return commande;
    }

    @Transactional
    public Commande updateCommande(Long id, CommandeDTO commandeDTO) {
        Commande commande = getCommandeById(id);

        if (commande.getStatut() == StatutCommande.LIVREE || commande.getStatut() == StatutCommande.ANNULEE) {
            throw new CommonException("Impossible de modifier une commande déjà livrée ou annulée.", HttpStatus.BAD_REQUEST);
        }

        Fournisseur fournisseur = commande.getFournisseur();
        if (commandeDTO.getFournisseurId() != null) {
            fournisseur = fournisseurRepository.findById(commandeDTO.getFournisseurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", commandeDTO.getFournisseurId()));
            commande.setFournisseur(fournisseur);
        }

        // Modification manuelle du numéro de commande fournisseur
        if (commandeDTO.getNumeroCommande() != null && !commandeDTO.getNumeroCommande().isBlank()) {
            String nouveauNum = commandeDTO.getNumeroCommande().trim();
            if (!nouveauNum.equalsIgnoreCase(commande.getNumeroCommande())) {
                if (entrepriseProfileService != null && !entrepriseProfileService.isModificationNumeroDocumentActive()) {
                    throw new CommonException("La modification du numéro de commande fournisseur n'est pas autorisée par la configuration de l'entreprise.", HttpStatus.BAD_REQUEST);
                }
                Long tenantId = getTenantId();
                var existant = commandeRepository.findByPointDeVenteIdAndNumeroCommande(tenantId, nouveauNum);
                if (existant.isPresent() && !existant.get().getId().equals(commande.getId())) {
                    throw new CommonException("Une commande fournisseur avec le numéro '" + nouveauNum + "' existe déjà.", HttpStatus.BAD_REQUEST);
                }
                commande.setNumeroCommande(nouveauNum);
            }
        }

        if (commandeDTO.getDateLivraisonPrevue() != null) {
            commande.setDateLivraisonPrevue(commandeDTO.getDateLivraisonPrevue());
        }
        if (commandeDTO.getObservations() != null) {
            commande.setObservations(commandeDTO.getObservations());
        }

        boolean isInternational = (fournisseur != null && fournisseur.getTypeFournisseur() == com.gestion.persistent.enums.TypeFournisseur.INTERNATIONAL)
                || commandeDTO.getTypeAchat() == com.gestion.persistent.enums.TypeAchat.INTERNATIONAL;
        commande.setTypeAchat(isInternational ? com.gestion.persistent.enums.TypeAchat.INTERNATIONAL : com.gestion.persistent.enums.TypeAchat.NATIONAL);

        if (isInternational) {
            if (commandeDTO.getIncoterm() != null) commande.setIncoterm(commandeDTO.getIncoterm());
            if (commandeDTO.getLieuIncoterm() != null) commande.setLieuIncoterm(commandeDTO.getLieuIncoterm());
            if (commandeDTO.getPortEmbarquement() != null) commande.setPortEmbarquement(commandeDTO.getPortEmbarquement());
            if (commandeDTO.getLieuDestinationId() != null && lieuLogistiqueRepository != null) {
                lieuLogistiqueRepository.findById(commandeDTO.getLieuDestinationId()).ifPresent(commande::setLieuDestination);
            }
            if (commandeDTO.getModeTransport() != null) commande.setModeTransport(commandeDTO.getModeTransport());
            if (commandeDTO.getConditionPaiementId() != null && conditionPaiementRepository != null) {
                conditionPaiementRepository.findById(commandeDTO.getConditionPaiementId()).ifPresent(commande::setConditionPaiement);
            }
            if (commandeDTO.getTauxChangeCommande() != null) commande.setTauxChangeCommande(commandeDTO.getTauxChangeCommande());
            if (commandeDTO.getTauxChangeDouane() != null) commande.setTauxChangeDouane(commandeDTO.getTauxChangeDouane());
            if (commandeDTO.getNumeroBl() != null) commande.setNumeroBl(commandeDTO.getNumeroBl());
            if (commandeDTO.getNumeroConteneur() != null) commande.setNumeroConteneur(commandeDTO.getNumeroConteneur());
            if (commandeDTO.getNavire() != null) commande.setNavire(commandeDTO.getNavire());
            if (commandeDTO.getDateEtd() != null) commande.setDateEtd(commandeDTO.getDateEtd());
            if (commandeDTO.getDateEta() != null) commande.setDateEta(commandeDTO.getDateEta());
            if (commandeDTO.getNumeroDum() != null) commande.setNumeroDum(commandeDTO.getNumeroDum());
            if (commandeDTO.getDateDum() != null) commande.setDateDum(commandeDTO.getDateDum());
        }

        if (commandeDTO.getDevise() != null && !commandeDTO.getDevise().isBlank()) {
            commande.setDevise(commandeDTO.getDevise());
        } else if (fournisseur != null && fournisseur.getDevise() != null) {
            commande.setDevise(fournisseur.getDevise());
        }

        boolean appliquerTva = !isInternational && Boolean.TRUE.equals(commandeDTO.getAppliquerTVA());
        commande.setAppliquerTVA(appliquerTva);
        commande.setTauxTVA(appliquerTva ? (commandeDTO.getTauxTVA() != null ? commandeDTO.getTauxTVA() : new BigDecimal("20.00")) : BigDecimal.ZERO);

        if (commandeDTO.getLignesCommande() != null) {
            if (commande.getLignesCommande() != null && !commande.getLignesCommande().isEmpty()) {
                ligneCommandeRepository.deleteAll(commande.getLignesCommande());
                commande.getLignesCommande().clear();
            }

            BigDecimal montantHTTotal = BigDecimal.ZERO;
            BigDecimal montantTVATotal = BigDecimal.ZERO;
            BigDecimal montantTTCTotal = BigDecimal.ZERO;

            for (LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                LigneCommande ligne = createLigneCommande(commande, ligneDTO, appliquerTva, commande.getTauxTVA());
                commande.getLignesCommande().add(ligne);
                if (ligne.getMontantHT() != null) {
                    montantHTTotal = montantHTTotal.add(ligne.getMontantHT());
                }
                if (ligne.getMontantTVA() != null) {
                    montantTVATotal = montantTVATotal.add(ligne.getMontantTVA());
                }
                if (ligne.getMontantTTC() != null) {
                    montantTTCTotal = montantTTCTotal.add(ligne.getMontantTTC());
                } else if (ligne.getMontantLigne() != null) {
                    montantTTCTotal = montantTTCTotal.add(ligne.getMontantLigne());
                }
            }

            BigDecimal finalHT = commandeDTO.getMontantHT() != null ? commandeDTO.getMontantHT() : montantHTTotal;
            BigDecimal finalTVA = appliquerTva ? (commandeDTO.getMontantTVA() != null ? commandeDTO.getMontantTVA() : montantTVATotal) : BigDecimal.ZERO;
            BigDecimal finalTTC = appliquerTva 
                    ? (commandeDTO.getMontantTTC() != null ? commandeDTO.getMontantTTC() : (commandeDTO.getMontantTotal() != null ? commandeDTO.getMontantTotal() : montantTTCTotal))
                    : (commandeDTO.getMontantTotal() != null ? commandeDTO.getMontantTotal() : (commandeDTO.getMontantHT() != null ? commandeDTO.getMontantHT() : finalHT));

            if (!appliquerTva && commandeDTO.getMontantTotal() != null) {
                finalHT = commandeDTO.getMontantHT() != null ? commandeDTO.getMontantHT() : commandeDTO.getMontantTotal();
                finalTTC = commandeDTO.getMontantTotal();
            }

            commande.setMontantHT(finalHT);
            commande.setMontantTVA(finalTVA);
            commande.setMontantTTC(finalTTC);
            commande.setMontantTotal(finalTTC);
        }

        commande = commandeRepository.save(commande);

        // Recalculer le coût de revient
        if (coutRevientService != null) {
            try {
                coutRevientService.recalculerEtPersister(commande.getId());
                commande = commandeRepository.findById(commande.getId()).orElse(commande);
            } catch (Exception e) {
                // Silencieux
            }
        }

        return commande;
    }

    private LigneCommande createLigneCommande(Commande commande, LigneCommandeDTO ligneDTO, boolean appliquerTva, BigDecimal defaultTauxTva) {
        Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", ligneDTO.getProduitId()));

        LigneCommande ligne = new LigneCommande();
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantiteCommandee(ligneDTO.getQuantiteCommandee() != null ? ligneDTO.getQuantiteCommandee() : 1);
        ligne.setQualiteProduit(ligneDTO.getQualiteProduit());
        ligne.setPoidsTotalKg(ligneDTO.getPoidsTotalKg());
        ligne.setVolumeTotalM3(ligneDTO.getVolumeTotalM3());
        ligne.setCodeSh(ligneDTO.getCodeSh());
        ligne.setTauxDroitDouane(ligneDTO.getTauxDroitDouane());

        BigDecimal qte = BigDecimal.valueOf(ligne.getQuantiteCommandee());
        BigDecimal prixUnitaire = ligneDTO.getPrixUnitaire() != null ? ligneDTO.getPrixUnitaire() : BigDecimal.ZERO;
        BigDecimal puHT = ligneDTO.getPrixUnitaireHT() != null ? ligneDTO.getPrixUnitaireHT() : prixUnitaire;
        BigDecimal tauxTVA = ligneDTO.getTauxTVA() != null ? ligneDTO.getTauxTVA() : (appliquerTva ? defaultTauxTva : BigDecimal.ZERO);

        ligne.setPrixUnitaire(prixUnitaire);
        ligne.setPrixUnitaireHT(puHT);
        ligne.setTauxTVA(tauxTVA);

        BigDecimal montantHT = ligneDTO.getMontantHT() != null ? ligneDTO.getMontantHT() 
                : (ligneDTO.getMontantLigne() != null && !appliquerTva ? ligneDTO.getMontantLigne() : puHT.multiply(qte));
        BigDecimal montantTVA = ligneDTO.getMontantTVA() != null ? ligneDTO.getMontantTVA() 
                : (appliquerTva ? montantHT.multiply(tauxTVA).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        BigDecimal montantTTC = ligneDTO.getMontantTTC() != null ? ligneDTO.getMontantTTC() 
                : (ligneDTO.getMontantLigne() != null ? ligneDTO.getMontantLigne() : montantHT.add(montantTVA));
        BigDecimal montantLigne = ligneDTO.getMontantLigne() != null ? ligneDTO.getMontantLigne() : (appliquerTva ? montantTTC : montantHT);

        ligne.setMontantHT(montantHT);
        ligne.setMontantTVA(montantTVA);
        ligne.setMontantTTC(montantTTC);
        ligne.setMontantLigne(montantLigne);

        return ligneCommandeRepository.save(ligne);
    }

    public List<Commande> getAllCommandes() {
        Long tenantId = getTenantId();
        return commandeRepository.findByPointDeVenteId(tenantId);
    }

    public Commande getCommandeById(Long commandeId) {
        Long tenantId = getTenantId();
        return commandeRepository.findByIdAndPointDeVenteId(commandeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", commandeId));
    }

    public List<Commande> getCommandesByStatut(StatutCommande statut) {
        Long tenantId = getTenantId();
        return commandeRepository.findByStatutAndPointDeVenteId(statut, tenantId);
    }

    public List<Commande> getCommandesByFournisseur(Long fournisseurId) {
        Long tenantId = getTenantId();
        return commandeRepository.findByFournisseurIdAndPointDeVenteId(fournisseurId, tenantId);
    }

    public List<Commande> getCommandesBrouillonParFournisseur(Long fournisseurId) {
        Long tenantId = getTenantId();
        if (fournisseurId != null) {
            return commandeRepository.findByFournisseurIdAndStatutAndPointDeVenteId(fournisseurId, StatutCommande.BROUILLON, tenantId);
        } else {
            return commandeRepository.findByStatutAndPointDeVenteId(StatutCommande.BROUILLON, tenantId);
        }
    }

    @Transactional
    public Commande ajouterLignesACommandeBrouillon(Long commandeId, List<LigneCommandeDTO> nouvellesLignes, String complementObservations) {
        Commande commande = getCommandeById(commandeId);

        if (commande.getStatut() != StatutCommande.BROUILLON) {
            throw new CommonException("Impossible d'ajouter des lignes à la commande #" + commande.getNumeroCommande() + 
                    " car elle n'est pas au statut BROUILLON (statut actuel : " + commande.getStatut() + ").", HttpStatus.BAD_REQUEST);
        }

        boolean appliquerTva = Boolean.TRUE.equals(commande.getAppliquerTVA());
        BigDecimal defaultTauxTva = commande.getTauxTVA() != null ? commande.getTauxTVA() : new BigDecimal("20.00");

        if (nouvellesLignes != null && !nouvellesLignes.isEmpty()) {
            if (commande.getLignesCommande() == null) {
                commande.setLignesCommande(new ArrayList<>());
            }

            for (LigneCommandeDTO ligneDTO : nouvellesLignes) {
                if (ligneDTO.getProduitId() == null) {
                    continue;
                }

                BigDecimal puHT = ligneDTO.getPrixUnitaireHT() != null ? ligneDTO.getPrixUnitaireHT() 
                        : (ligneDTO.getPrixUnitaire() != null ? ligneDTO.getPrixUnitaire() : BigDecimal.ZERO);
                BigDecimal tauxTvaLigne = ligneDTO.getTauxTVA() != null ? ligneDTO.getTauxTVA() : (appliquerTva ? defaultTauxTva : BigDecimal.ZERO);

                // Vérifier si le produit existe déjà avec le même prix unitaire HT et taux TVA
                Optional<LigneCommande> existante = commande.getLignesCommande().stream()
                        .filter(l -> l.getProduit() != null && l.getProduit().getId().equals(ligneDTO.getProduitId())
                                && (l.getPrixUnitaireHT() != null ? l.getPrixUnitaireHT().compareTo(puHT) == 0 : (l.getPrixUnitaire() != null && l.getPrixUnitaire().compareTo(puHT) == 0))
                                && ((l.getTauxTVA() == null && tauxTvaLigne.compareTo(BigDecimal.ZERO) == 0) || (l.getTauxTVA() != null && l.getTauxTVA().compareTo(tauxTvaLigne) == 0)))
                        .findFirst();

                if (existante.isPresent()) {
                    LigneCommande l = existante.get();
                    int qteAjout = ligneDTO.getQuantiteCommandee() != null ? ligneDTO.getQuantiteCommandee() : 1;
                    l.setQuantiteCommandee(l.getQuantiteCommandee() + qteAjout);

                    BigDecimal qte = BigDecimal.valueOf(l.getQuantiteCommandee());
                    BigDecimal montantHT = puHT.multiply(qte);
                    BigDecimal montantTVA = appliquerTva 
                            ? montantHT.multiply(tauxTvaLigne).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal montantTTC = appliquerTva ? montantHT.add(montantTVA) : montantHT;

                    l.setMontantHT(montantHT);
                    l.setMontantTVA(montantTVA);
                    l.setMontantTTC(montantTTC);
                    l.setMontantLigne(montantTTC);
                    ligneCommandeRepository.save(l);
                } else {
                    LigneCommande nouvelleLigne = createLigneCommande(commande, ligneDTO, appliquerTva, defaultTauxTva);
                    commande.getLignesCommande().add(nouvelleLigne);
                }
            }

            // Recalculer les totaux de la commande
            BigDecimal montantHTTotal = BigDecimal.ZERO;
            BigDecimal montantTVATotal = BigDecimal.ZERO;
            BigDecimal montantTTCTotal = BigDecimal.ZERO;

            for (LigneCommande l : commande.getLignesCommande()) {
                if (l.getMontantHT() != null) {
                    montantHTTotal = montantHTTotal.add(l.getMontantHT());
                }
                if (l.getMontantTVA() != null) {
                    montantTVATotal = montantTVATotal.add(l.getMontantTVA());
                }
                if (l.getMontantTTC() != null) {
                    montantTTCTotal = montantTTCTotal.add(l.getMontantTTC());
                } else if (l.getMontantLigne() != null) {
                    montantTTCTotal = montantTTCTotal.add(l.getMontantLigne());
                }
            }

            commande.setMontantHT(montantHTTotal);
            commande.setMontantTVA(appliquerTva ? montantTVATotal : BigDecimal.ZERO);
            commande.setMontantTTC(appliquerTva ? montantTTCTotal : montantHTTotal);
            commande.setMontantTotal(commande.getMontantTTC());
        }

        // Compléter les observations
        if (complementObservations != null && !complementObservations.isBlank()) {
            String obsActuelle = commande.getObservations();
            if (obsActuelle != null && !obsActuelle.isBlank()) {
                if (!obsActuelle.contains(complementObservations)) {
                    commande.setObservations(obsActuelle + " | " + complementObservations);
                }
            } else {
                commande.setObservations("Émis suite à : " + complementObservations);
            }
        }

        commande = commandeRepository.save(commande);

        // Recalcul du coût de revient si applicable
        if (coutRevientService != null) {
            try {
                coutRevientService.recalculerEtPersister(commande.getId());
                commande = commandeRepository.findById(commande.getId()).orElse(commande);
            } catch (Exception e) {
                // Silencieux
            }
        }

        return commande;
    }

    @Transactional
    public Commande updateStatutCommande(Long commandeId, StatutCommande nouveauStatut) {
        Commande commande = getCommandeById(commandeId);

        // RÈGLE MÉTIER 1 : Approbation d'achat par seuil de montant (> 10 000 MAD par défaut)
        if (nouveauStatut == StatutCommande.VALIDEE) {
            if (entrepriseProfileService != null && entrepriseProfileService.isApprobationAchatActive()) {
                BigDecimal seuil = entrepriseProfileService.getSeuilApprobationAchat();
                if (commande.getMontantTotal() != null && commande.getMontantTotal().compareTo(seuil) > 0) {
                    org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    boolean isAdmin = auth != null && auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN") || a.getAuthority().equals("ADMIN"));
                    if (!isAdmin) {
                        throw new CommonException("Validation d'Achat Bloquée (Règle Métier active) : Le montant total (" + 
                                commande.getMontantTotal() + " MAD) dépasse le seuil d'autorisation de " + seuil + " MAD. " +
                                "Seul un Administrateur est habilité à valider cette commande.", HttpStatus.FORBIDDEN, "SEUIL_ACHAT_DEPASSE");
                    }
                }
            }
        }

        commande.setStatut(nouveauStatut);
        
        if (nouveauStatut == StatutCommande.LIVREE) {
            commande.setDateLivraisonReelle(LocalDateTime.now());
        }
        
        return commandeRepository.save(commande);
    }

    @Transactional
    public Commande annulerCommande(Long commandeId) {
        Commande commande = getCommandeById(commandeId);
        
        if (commande.getStatut() == StatutCommande.ANNULEE) {
            throw new CommonException("Cette commande est déjà annulée.", HttpStatus.BAD_REQUEST);
        }

        if (commande.getStatut() == StatutCommande.LIVREE) {
            throw new CommonException("Impossible d'annuler une commande déjà livrée.", HttpStatus.BAD_REQUEST);
        }

        // RÈGLE : Impossible d'annuler une commande liée à des livraisons actives (non annulées)
        List<Livraison> livraisons = livraisonRepository.findByCommande_Id(commandeId);
        if (livraisons != null && !livraisons.isEmpty()) {
            boolean hasActiveLivraison = livraisons.stream().anyMatch(l -> l.getStatut() != StatutLivraison.ANNULEE);
            if (hasActiveLivraison) {
                String numeros = livraisons.stream()
                        .filter(l -> l.getStatut() != StatutLivraison.ANNULEE)
                        .map(l -> l.getNumeroLivraison() != null ? l.getNumeroLivraison() : ("#" + l.getId()))
                        .collect(Collectors.joining(", "));
                throw new CommonException("Impossible d'annuler cette commande car elle possède un ou plusieurs bons de livraison actifs (" + 
                        numeros + "). Vous devez d'abord annuler ces livraisons.", HttpStatus.BAD_REQUEST);
            }
        }
        
        commande.setStatut(StatutCommande.ANNULEE);
        Commande saved = commandeRepository.save(commande);

        Long effectiveTenantId = commande.getPointDeVenteId() != null ? commande.getPointDeVenteId() : getTenantId();
        codificationService.libererNumero(
                com.gestion.persistent.enums.TypeDocumentCodification.COMMANDE_FOURNISSEUR,
                effectiveTenantId,
                commande.getNumeroCommande(),
                commande.getId(),
                "Annulation de commande"
        );

        return saved;
    }

    @Transactional
    public Commande receptionnerCommande(Long commandeId, ReceptionCommandeDTO receptionDTO) {
        Commande commande = getCommandeById(commandeId);

        if (commande.getStatut() == StatutCommande.LIVREE || commande.getStatutLivraison() == StatutLivraison.LIVREE) {
            throw new IllegalStateException("Cette commande a déjà été entièrement réceptionnée");
        }
        if (commande.getStatut() == StatutCommande.ANNULEE) {
            throw new IllegalStateException("Impossible de réceptionner une commande annulée");
        }

        List<LigneCommande> lignesCommande = commande.getLignesCommande();
        if (lignesCommande == null || lignesCommande.isEmpty()) {
            throw new IllegalStateException("La commande ne contient aucune ligne à réceptionner");
        }

        // Calcul des quantités déjà livrées pour chaque produit de la commande
        Map<Long, Double> qteDejaLivreeParProduit = new HashMap<>();
        List<Livraison> livraisonsExistantes = livraisonRepository.findByCommande_IdAndStatut(commandeId, StatutLivraison.LIVREE);
        for (Livraison liv : livraisonsExistantes) {
            for (LigneLivraison ll : liv.getLignesLivraison()) {
                Long produitId = ll.getProduit().getId();
                qteDejaLivreeParProduit.merge(produitId, (double) ll.getQuantiteLivree(), Double::sum);
            }
        }

        List<LigneLivraisonDTO> lignesLivraisonDTO = new ArrayList<>();

        if (receptionDTO == null || receptionDTO.getLignes() == null || receptionDTO.getLignes().isEmpty()) {
            // CAS 1 : RÉCEPTION TOTALE (TOUTE LA COMMANDE OU TOUT LE RELIQUAT RESTANT)
            for (LigneCommande lc : lignesCommande) {
                double dejaLivre = qteDejaLivreeParProduit.getOrDefault(lc.getProduit().getId(), 0.0);
                double restant = lc.getQuantiteCommandee() - dejaLivre;
                if (restant > 0) {
                    LigneLivraisonDTO lldto = new LigneLivraisonDTO();
                    lldto.setProduit(produitMapper.toDto(lc.getProduit()));
                    lldto.setQuantiteLivree((long) restant);
                    lldto.setQualiteProduit(lc.getQualiteProduit());

                    if (receptionDTO != null && receptionDTO.getDepotId() != null) {
                        DepotDTO dd = new DepotDTO();
                        dd.setId(receptionDTO.getDepotId());
                        lldto.setDepot(dd);
                    }
                    lignesLivraisonDTO.add(lldto);
                }
            }
        } else {
            // CAS 2 : RÉCEPTION PARTIELLE (QUANTITÉS REÇUES SPÉCIFIÉES)
            for (LigneReceptionDTO item : receptionDTO.getLignes()) {
                if (item.getQuantiteRecue() == null || item.getQuantiteRecue() <= 0) {
                    continue;
                }

                LigneCommande lc = null;
                if (item.getLigneCommandeId() != null) {
                    lc = lignesCommande.stream()
                            .filter(l -> l.getId().equals(item.getLigneCommandeId()))
                            .findFirst()
                            .orElse(null);
                }
                if (lc == null && item.getProduitId() != null) {
                    lc = lignesCommande.stream()
                            .filter(l -> l.getProduit().getId().equals(item.getProduitId()))
                            .findFirst()
                            .orElse(null);
                }
                if (lc == null) {
                    throw new ResourceNotFoundException("LigneCommande", "id/produitId",
                            item.getLigneCommandeId() != null ? item.getLigneCommandeId() : item.getProduitId());
                }

                double dejaLivre = qteDejaLivreeParProduit.getOrDefault(lc.getProduit().getId(), 0.0);
                double restant = lc.getQuantiteCommandee() - dejaLivre;
                double qteRecue = item.getQuantiteRecue();
                if (qteRecue > restant) {
                    boolean toleranceActive = entrepriseProfileService != null && entrepriseProfileService.isToleranceEcartReceptionActive();
                    double tolerancePct = toleranceActive ? entrepriseProfileService.getToleranceEcartPourcentage().doubleValue() : 0.0;
                    double maxAutorise = restant * (1.0 + (tolerancePct / 100.0));
                    if (qteRecue > maxAutorise) {
                        String nomProd = lc.getProduit().getDesignation() != null ? lc.getProduit().getDesignation() : lc.getProduit().getNom();
                        if (toleranceActive) {
                            throw new CommonException(String.format(
                                    "Écart de Réception (Règle Métier active) : La quantité reçue (%d) dépasse le seuil de tolérance de %.1f%% (max autorisé: %d, restant: %d) pour le produit '%s'.",
                                    (int) qteRecue, tolerancePct, (int) Math.floor(maxAutorise), (int) restant, nomProd),
                                    HttpStatus.BAD_REQUEST, "TOLERANCE_RECEPTION_DEPASSEE");
                        } else {
                            throw new CommonException(String.format(
                                    "La quantité reçue (%d) dépasse la quantité restante (%d) pour le produit '%s'. Activez la tolérance de sur-réception dans les paramètres si nécessaire.",
                                    (int) qteRecue, (int) restant, nomProd),
                                    HttpStatus.BAD_REQUEST, "SUR_RECEPTION_NON_AUTORISEE");
                        }
                    }
                }

                LigneLivraisonDTO lldto = new LigneLivraisonDTO();
                lldto.setProduit(produitMapper.toDto(lc.getProduit()));
                lldto.setQuantiteLivree(item.getQuantiteRecue().longValue());
                lldto.setQualiteProduit(item.getQualiteProduit() != null ? item.getQualiteProduit() : lc.getQualiteProduit());

                Long depotId = item.getDepotId() != null ? item.getDepotId()
                        : (receptionDTO.getDepotId() != null ? receptionDTO.getDepotId() : null);
                if (depotId != null) {
                    DepotDTO dd = new DepotDTO();
                    dd.setId(depotId);
                    lldto.setDepot(dd);
                }
                lignesLivraisonDTO.add(lldto);
            }
        }

        if (lignesLivraisonDTO.isEmpty()) {
            throw new IllegalStateException("Aucun article restant à réceptionner pour cette commande");
        }

        // Création de la livraison pour la commande
        LivraisonDTO livraisonDTO = new LivraisonDTO();
        livraisonDTO.setCommandeId(commande.getId());
        livraisonDTO.setDateLivraison(receptionDTO != null && receptionDTO.getDateLivraison() != null
                ? receptionDTO.getDateLivraison() : LocalDateTime.now());
        livraisonDTO.setTransporteur(receptionDTO != null ? receptionDTO.getTransporteur() : null);
        livraisonDTO.setNumeroSuivi(receptionDTO != null ? receptionDTO.getNumeroSuivi() : null);
        livraisonDTO.setObservations(receptionDTO != null && receptionDTO.getObservations() != null
                ? receptionDTO.getObservations() : "Réception pour commande " + commande.getNumeroCommande());
        livraisonDTO.setLignesLivraison(lignesLivraisonDTO);

        Livraison livraison = livraisonMapper.toEntity(livraisonDTO);
        Livraison livraisonSaved = livraisonService.creerLivraison(livraison);

        // Valider la livraison pour enregistrer automatiquement le stock, le lot et les mouvements de stock
        livraisonService.validerLivraison(livraisonSaved.getId());

        // Recharger la commande avec les statuts et lignes à jour
        return getCommandeById(commandeId);
    }

    private String generateNumeroCommande() {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        String numero;
        int attempts = 0;
        do {
            numero = codificationService.genererNumero(com.gestion.persistent.enums.TypeDocumentCodification.COMMANDE_FOURNISSEUR, effectiveTenantId);
            attempts++;
        } while (commandeRepository.findByPointDeVenteIdAndNumeroCommande(effectiveTenantId, numero).isPresent() && attempts < 1000);
        return numero;
    }

    public List<Commande> searchCommandes(CommandeSearchCriteria criteria) {
        return commandeRepository.findByCriteria(criteria);
    }

    public org.springframework.data.domain.Page<Commande> searchCommandes(CommandeSearchCriteria criteria, org.springframework.data.domain.Pageable pageable) {
        return commandeRepository.findByCriteria(criteria, pageable);
    }

    public byte[] generateCommandePdf(Long commandeId) {
        if (this.impressionService != null) {
            return this.impressionService.genererCommandeFournisseurPdf(commandeId);
        }
        Commande commande = getCommandeById(commandeId);
        if (commande.getStatut() == StatutCommande.BROUILLON || commande.getStatut() == StatutCommande.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer une commande fournisseur en statut Brouillon ou Annulée. Elle doit être validée.");
        }
        
        try {
            // Compile main report
            InputStream mainReportStream = getClass().getClassLoader().getResourceAsStream("reports/commande.jrxml");
            if (mainReportStream == null) {
                throw new RuntimeException("Template JRXML non trouvé: reports/commande.jrxml");
            }
            JasperReport mainReport = JasperCompileManager.compileReport(mainReportStream);
            
            // Compile subreport
            InputStream subReportStream = getClass().getClassLoader().getResourceAsStream("reports/lignes_commande_subreport.jrxml");
            if (subReportStream == null) {
                throw new RuntimeException("Subreport JRXML non trouvé: reports/lignes_commande_subreport.jrxml");
            }
            JasperReport subReport = JasperCompileManager.compileReport(subReportStream);
            
            // Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("numeroCommande", commande.getNumeroCommande());
            parameters.put("dateCommande", commande.getDateCommande());
            parameters.put("dateLivraisonPrevue", commande.getDateLivraisonPrevue());
            parameters.put("fournisseurNom", commande.getFournisseur().getRaisonSociale());
            parameters.put("fournisseurAdresse", commande.getFournisseur().getAdresse());
            parameters.put("fournisseurPays", commande.getFournisseur().getPays());
            parameters.put("montantTotal", commande.getMontantTotal());
            parameters.put("observations", commande.getObservations());
            parameters.put("statut", commande.getStatut().toString());
            
            // Add compiled subreport to parameters
            parameters.put("SUBREPORT_DIR", subReport);
            
            // Prepare lignes data
            List<Map<String, Object>> lignesData = commande.getLignesCommande().stream()
                    .map(ligne -> {
                        Map<String, Object> ligneMap = new HashMap<>();
                        ligneMap.put("produitReference", ligne.getProduit().getReference());
                        ligneMap.put("produitDescription", ligne.getProduit().getDescription());
                        ligneMap.put("quantiteCommandee", new BigDecimal(ligne.getQuantiteCommandee()));                        ligneMap.put("prixUnitaire", ligne.getPrixUnitaire());
                        ligneMap.put("montantLigne", ligne.getMontantLigne());
                        return ligneMap;
                    })
                    .collect(Collectors.toList());
            
            JRDataSource dataSource = new JRBeanCollectionDataSource(lignesData);
            parameters.put("lignesCommande", dataSource);
            
            // Generate PDF
            JasperPrint jasperPrint = JasperFillManager.fillReport(mainReport, parameters, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jasperPrint);
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        }
    }
}

