package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.ceramique.persistent.dto.CommandeDTO;
import com.ceramique.persistent.dto.CommandeSearchCriteria;
import com.ceramique.persistent.dto.LigneCommandeDTO;
import com.ceramique.persistent.enums.StatutCommande;
import com.ceramique.persistent.model.Commande;
import com.ceramique.persistent.model.Fournisseur;
import com.ceramique.persistent.model.LigneCommande;
import com.ceramique.persistent.model.Produit;
import com.ceramique.repository.CommandeRepository;
import com.ceramique.repository.FournisseurRepository;
import com.ceramique.repository.LigneCommandeRepository;
import com.ceramique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final PointDeVenteRepository pointDeVenteRepository;

    public CommandeService(CommandeRepository commandeRepository,
                          LigneCommandeRepository ligneCommandeRepository,
                          FournisseurRepository fournisseurRepository,
                          ProduitRepository produitRepository,
                          PointDeVenteRepository pointDeVenteRepository) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'une nouvelle commande")
    public Commande createCommande(CommandeDTO commandeDTO) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        Fournisseur fournisseur = fournisseurRepository.findByIdAndPointDeVente_Id(
                commandeDTO.getFournisseurId(), pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", commandeDTO.getFournisseurId()));

        Commande commande = new Commande();
        commande.setNumeroCommande(generateNumeroCommande());
        commande.setFournisseur(fournisseur);
        commande.setPointDeVente(pointDeVente);
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande.setDateLivraisonPrevue(commandeDTO.getDateLivraisonPrevue());
        commande.setObservations(commandeDTO.getObservations());

        commande = commandeRepository.save(commande);

        // Créer les lignes de commande
        BigDecimal montantTotal = BigDecimal.ZERO;
        for (LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
            LigneCommande ligne = createLigneCommande(commande, ligneDTO, pointDeVente.getId());
            montantTotal = montantTotal.add(ligne.getMontantLigne());
        }

        commande.setMontantTotal(montantTotal);
        return commandeRepository.save(commande);
    }

    private LigneCommande createLigneCommande(Commande commande, LigneCommandeDTO ligneDTO, Long pointDeVenteId) {
        Produit produit = produitRepository.findByIdAndPointDeVente_Id(ligneDTO.getProduitId(), pointDeVenteId)
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

    @MultitenantSearchMethod(description = "Récupération de toutes les commandes")
    public List<Commande> getAllCommandes() {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return commandeRepository.findByPointDeVente_IdOrderByDateCommandeDesc(pointDeVente.getId());
    }

    @MultitenantSearchMethod(description = "Récupération d'une commande par ID")
    public Commande getCommandeById(Long commandeId) {
        Long tenantId = TenantContext.getCurrentTenant();
        
        return commandeRepository.findByIdAndPointDeVente_Id(commandeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", commandeId));
    }

    @MultitenantSearchMethod(description = "Récupération des commandes par statut")
    public List<Commande> getCommandesByStatut(StatutCommande statut) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return commandeRepository.findByPointDeVente_IdAndStatut(pointDeVente.getId(), statut);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Mise à jour du statut d'une commande")
    public Commande updateStatutCommande(Long commandeId, StatutCommande nouveauStatut) {
        Commande commande = getCommandeById(commandeId);
        commande.setStatut(nouveauStatut);
        
        if (nouveauStatut == StatutCommande.LIVREE) {
            commande.setDateLivraisonReelle(LocalDateTime.now());
        }
        
        return commandeRepository.save(commande);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Annulation d'une commande")
    public Commande annulerCommande(Long commandeId) {
        Commande commande = getCommandeById(commandeId);
        
        if (commande.getStatut() == StatutCommande.LIVREE) {
            throw new IllegalStateException("Impossible d'annuler une commande déjà livrée");
        }
        
        commande.setStatut(StatutCommande.ANNULEE);
        return commandeRepository.save(commande);
    }

    private String generateNumeroCommande() {
        String prefix = "CMD-";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp;
    }

    @MultitenantSearchMethod(description = "Recherche de commandes par critères")
    public List<Commande> searchCommandes(CommandeSearchCriteria criteria) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return commandeRepository.findByCriteria(criteria, pointDeVente.getId());
    }

    @MultitenantSearchMethod(description = "Génération PDF d'une commande")
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
