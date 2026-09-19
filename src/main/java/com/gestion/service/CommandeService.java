package com.gestion.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

    public CommandeService(CommandeRepository commandeRepository,
                          LigneCommandeRepository ligneCommandeRepository,
                          FournisseurRepository fournisseurRepository,
                          ProduitRepository produitRepository,
                          LivraisonService livraisonService,
                          LivraisonRepository livraisonRepository,
                          LivraisonMapper livraisonMapper,
                          ProduitMapper produitMapper) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.livraisonService = livraisonService;
        this.livraisonRepository = livraisonRepository;
        this.livraisonMapper = livraisonMapper;
        this.produitMapper = produitMapper;
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
        commande.setNumeroCommande(generateNumeroCommande());
        commande.setFournisseur(fournisseur);
        commande.setStatut(StatutCommande.BROUILLON);
        commande.setDateLivraisonPrevue(commandeDTO.getDateLivraisonPrevue());
        commande.setObservations(commandeDTO.getObservations());
        commande.setPointDeVenteId(getTenantId());

        commande = commandeRepository.save(commande);

        // Créer les lignes de commande
        BigDecimal montantTotal = BigDecimal.ZERO;
        for (LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
            LigneCommande ligne = createLigneCommande(commande, ligneDTO);
            montantTotal = montantTotal.add(ligne.getMontantLigne());
        }

        commande.setMontantTotal(montantTotal);
        return commandeRepository.save(commande);
    }

    private LigneCommande createLigneCommande(Commande commande, LigneCommandeDTO ligneDTO) {
        Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", ligneDTO.getProduitId()));

        LigneCommande ligne = new LigneCommande();
        ligne.setCommande(commande);
        ligne.setProduit(produit);
        ligne.setQuantiteCommandee(ligneDTO.getQuantiteCommandee());
        ligne.setPrixUnitaire(ligneDTO.getPrixUnitaire());
        ligne.setQualiteProduit(ligneDTO.getQualiteProduit());
        ligne.setMontantLigne(BigDecimal.valueOf(ligneDTO.getQuantiteCommandee()).multiply(ligneDTO.getPrixUnitaire()));

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

    @Transactional
    public Commande updateStatutCommande(Long commandeId, StatutCommande nouveauStatut) {
        Commande commande = getCommandeById(commandeId);
        commande.setStatut(nouveauStatut);
        
        if (nouveauStatut == StatutCommande.LIVREE) {
            commande.setDateLivraisonReelle(LocalDateTime.now());
        }
        
        return commandeRepository.save(commande);
    }

    @Transactional
    public Commande annulerCommande(Long commandeId) {
        Commande commande = getCommandeById(commandeId);
        
        if (commande.getStatut() == StatutCommande.LIVREE) {
            throw new IllegalStateException("Impossible d'annuler une commande déjà livrée");
        }
        
        commande.setStatut(StatutCommande.ANNULEE);
        return commandeRepository.save(commande);
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
                    lldto.setPrixProduit(lc.getPrixUnitaire());
                    lldto.setQualiteProduit(lc.getQualiteProduit() != null ? lc.getQualiteProduit() : QualiteProduit.PREMIERE_QUALITE);

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
                if (item.getQuantiteRecue() > restant) {
                    throw new IllegalArgumentException(String.format(
                            "La quantité reçue (%d) dépasse la quantité restante (%d) pour le produit %s",
                            item.getQuantiteRecue(), (int) restant, lc.getProduit().getNom()));
                }

                LigneLivraisonDTO lldto = new LigneLivraisonDTO();
                lldto.setProduit(produitMapper.toDto(lc.getProduit()));
                lldto.setQuantiteLivree(item.getQuantiteRecue().longValue());
                lldto.setPrixProduit(item.getPrixUnitaire() != null ? item.getPrixUnitaire() : lc.getPrixUnitaire());
                lldto.setQualiteProduit(item.getQualiteProduit() != null ? item.getQualiteProduit()
                        : (lc.getQualiteProduit() != null ? lc.getQualiteProduit() : QualiteProduit.PREMIERE_QUALITE));

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
        String prefix = "CMD-";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp;
    }

    public List<Commande> searchCommandes(CommandeSearchCriteria criteria) {
        return commandeRepository.findByCriteria(criteria);
    }

    public byte[] generateCommandePdf(Long commandeId) {
        Commande commande = getCommandeById(commandeId);
        
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

