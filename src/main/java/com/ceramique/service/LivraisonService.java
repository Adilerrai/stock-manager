package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.ceramique.persistent.dto.LivraisonDTO;
import com.ceramique.persistent.dto.LigneLivraisonDTO;
import com.ceramique.persistent.dto.LivraisonSearchCriteria;
import com.ceramique.persistent.enums.StatutLivraison;
import com.ceramique.persistent.enums.TypeMouvement;
import com.ceramique.persistent.model.*;
import com.ceramique.repository.*;
import com.ceramique.mapper.LivraisonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class LivraisonService {

    private static final Logger log = LoggerFactory.getLogger(LivraisonService.class);

    private final LivraisonRepository livraisonRepository;
    private final LivraisonMapper livraisonMapper;
    private final LigneLivraisonRepository ligneLivraisonRepository;
    private final ProduitRepository produitRepository;
    private final StockService stockService;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final DepotRepository depotRepository;
    private final MouvementStockService mouvementStockService;
    private final LotService lotService;
    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    public LivraisonService(LivraisonRepository livraisonRepository, 
                           LivraisonMapper livraisonMapper,
                           LigneLivraisonRepository ligneLivraisonRepository,
                           ProduitRepository produitRepository,
                           StockService stockService,
                           PointDeVenteRepository pointDeVenteRepository,
                           DepotRepository depotRepository,
                           MouvementStockService mouvementStockService,
                           LotService lotService,
                           CommandeRepository commandeRepository,
                           LigneCommandeRepository ligneCommandeRepository) {
        this.livraisonRepository = livraisonRepository;
        this.livraisonMapper = livraisonMapper;
        this.ligneLivraisonRepository = ligneLivraisonRepository;
        this.produitRepository = produitRepository;
        this.stockService = stockService;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.depotRepository = depotRepository;
        this.mouvementStockService = mouvementStockService;
        this.lotService = lotService;
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'une livraison en brouillon")
    public Livraison createLivraison(LivraisonDTO livraisonDTO) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        Livraison livraison = new Livraison();
        livraison.setNumeroLivraison(generateNumeroLivraison());
        livraison.setPointDeVente(pointDeVente);
        livraison.setTransporteur(livraisonDTO.getTransporteur());
        livraison.setNumeroSuivi(livraisonDTO.getNumeroSuivi());
        livraison.setObservations(livraisonDTO.getObservations());
        livraison.setStatut(StatutLivraison.EN_ATTENTE);
        livraison.setDateLivraison(livraisonDTO.getDateLivraison() != null ? 
                livraisonDTO.getDateLivraison() : LocalDateTime.now());

        livraison = livraisonRepository.save(livraison);

        BigDecimal montantTotal = BigDecimal.ZERO;
        if (livraisonDTO.getLignesLivraison() != null) {
            for (LigneLivraisonDTO ligneDTO : livraisonDTO.getLignesLivraison()) {
                LigneLivraison ligne = createLigneLivraison(livraison, ligneDTO, pointDeVente.getId());
                montantTotal = montantTotal.add(ligne.getPrixProduit().multiply(
                        BigDecimal.valueOf(ligne.getQuantiteLivree())));
            }
        }

        livraison.setMontantTotal(montantTotal);
        return livraisonRepository.save(livraison);
    }

    private LigneLivraison createLigneLivraison(Livraison livraison, LigneLivraisonDTO ligneDTO, Long pointDeVenteId) {
        Produit produit = produitRepository.findByIdAndPointDeVente_Id(ligneDTO.getProduit().getId(), pointDeVenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", ligneDTO.getProduit().getId()));

        // Get depot from DTO or use default depot
        Depot depot = null;
        if (ligneDTO.getDepot() != null && ligneDTO.getDepot().getId() != null) {
            // try find by id (no longer strictly filtered by pointDeVente) and fallback to first active depot
            depot = depotRepository.findByIdAndPointDeVente_Id(ligneDTO.getDepot().getId(), pointDeVenteId)
                    .orElse(null);
            if (depot == null) {
                log.warn("Depot with id {} not found for pointDeVente {} - falling back to first active depot", ligneDTO.getDepot().getId(), pointDeVenteId);
                List<Depot> depots = depotRepository.findByPointDeVente_IdAndActifTrue(pointDeVenteId);
                if (!depots.isEmpty()) {
                    depot = depots.get(0);
                } else {
                    log.warn("No active depots found for pointDeVente {} - depot will be left null", pointDeVenteId);
                }
            }
        } else {
            List<Depot> depots = depotRepository.findByPointDeVente_IdAndActifTrue(pointDeVenteId);
            if (depots.isEmpty()) {
                log.warn("No active depots found for pointDeVente {} - depot will be left null", pointDeVenteId);
            } else {
                depot = depots.get(0);
            }
        }

        LigneLivraison ligne = new LigneLivraison();
        ligne.setLivraison(livraison);
        ligne.setProduit(produit);
        ligne.setDepot(depot);
        ligne.setQuantiteLivree(ligneDTO.getQuantiteLivree());
        ligne.setPrixProduit(ligneDTO.getPrixProduit());
        
        // La qualité doit toujours venir du frontend - obligatoire
        if (ligneDTO.getQualiteProduit() == null) {
            throw new IllegalArgumentException("La qualité du produit est obligatoire");
        }
        ligne.setQualiteProduit(ligneDTO.getQualiteProduit());

        return ligneLivraisonRepository.save(ligne);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Validation d'une livraison avec création de lots")
    public Livraison validerLivraison(Long livraisonId) {
        Livraison livraison = this.getLivraisonById(livraisonId);
        
        if (livraison.getStatut() != StatutLivraison.EN_ATTENTE) {
            throw new IllegalStateException("Seules les livraisons en attente peuvent être validées");
        }

        livraison.setStatut(StatutLivraison.LIVREE);
        livraison.setDateLivraison(LocalDateTime.now());

        // Load lines explicitly to avoid lazy loading issues
        List<LigneLivraison> lignes = ligneLivraisonRepository.findByLivraison_Id(livraisonId);
        
        for (LigneLivraison ligne : lignes) {
            // Résolution sécurisée du depotId : utiliser le depot de la ligne si présent, sinon fallback vers le premier depot actif
            Long depotId = null;
            if (ligne.getDepot() != null && ligne.getDepot().getId() != null) {
                depotId = ligne.getDepot().getId();
            } else if (livraison.getPointDeVente() != null && livraison.getPointDeVente().getId() != null) {
                List<Depot> depots = depotRepository.findByPointDeVente_IdAndActifTrue(livraison.getPointDeVente().getId());
                if (!depots.isEmpty()) {
                    depotId = depots.get(0).getId();
                    log.warn("Using fallback depot {} for livraison {}", depotId, livraison.getId());
                } else {
                    log.warn("No depot available for pointDeVente {} while validating livraison {}", livraison.getPointDeVente() != null ? livraison.getPointDeVente().getId() : null, livraison.getId());
                }
            } else {
                log.warn("Cannot resolve pointDeVente for livraison {} to find depot fallback", livraison.getId());
            }

            Lot lot = lotService.creerLot(
                ligne.getProduit().getId(),
                depotId,
                ligne.getQualiteProduit(),
                BigDecimal.valueOf(ligne.getQuantiteLivree()),
                livraison.getNumeroLivraison(),
                livraison.getTransporteur(),
                ligne.getPrixProduit()
            );
            
            // FIX appel stockService
            stockService.ajouterStockParQualite(
                ligne.getProduit().getId(),
                ligne.getQualiteProduit(),
                BigDecimal.valueOf(ligne.getQuantiteLivree())
            );
            
            // FIX appel mouvementStockService (sans depotId)
            mouvementStockService.creerMouvement(
                ligne.getProduit().getId(),
                depotId,
                TypeMouvement.ENTREE_LIVRAISON,
                BigDecimal.valueOf(ligne.getQuantiteLivree()),
                ligne.getQualiteProduit(),
                "LIV-" + livraison.getNumeroLivraison(),
                "Validation livraison avec lot " + lot.getNumeroLot()
            );
        }

        // Mettre à jour le statut de la commande si elle existe
        if (livraison.getCommande() != null) {
            mettreAJourStatutLivraisonCommande(livraison.getCommande());
        }

        return livraisonRepository.save(livraison);
    }

    public Livraison getLivraisonById(Long livraisonId) {
        Long tenantId = TenantContext.getCurrentTenant();
        
        return livraisonRepository.findByIdAndPointDeVente_TenantId(livraisonId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison", "id", livraisonId));
    }

    @Transactional
    @MultitenantSearchMethod(description = "Annulation d'une livraison")
    public Livraison annulerLivraison(Long livraisonId) {
        Livraison livraison = getLivraisonById(livraisonId);
        
        if (livraison.getStatut() == StatutLivraison.LIVREE) {
            throw new IllegalStateException("Impossible d'annuler une livraison livrée");
        }

        livraison.setStatut(StatutLivraison.ANNULEE);
        return livraisonRepository.save(livraison);
    }

    @MultitenantSearchMethod(description = "Récupération des livraisons par statut")
    public List<Livraison> getLivraisonsByStatut(StatutLivraison statut) {
        Long tenantId = TenantContext.getCurrentTenant();
        
        return livraisonRepository.findByStatutAndPointDeVente_TenantId(statut, tenantId);
    }

    @MultitenantSearchMethod(description = "Récupération des détails d'une livraison")
    public Livraison getLivraisonWithDetails(Long livraisonId) {
        return getLivraisonById(livraisonId);
    }

    private String generateNumeroLivraison() {
        String prefix = "LIV-";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp;
    }

    @MultitenantSearchMethod(description = "Récupération de toutes les livraisons")
    public List<Livraison> getAllLivraisons() {
        Long tenantId = TenantContext.getCurrentTenant();
        
        return livraisonRepository.findByPointDeVente_TenantId(tenantId);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Mise à jour d'une livraison")
    public Livraison updateLivraison(LivraisonDTO livraisonDTO) {
        Livraison existingLivraison = getLivraisonById(livraisonDTO.getId());
        
        if (existingLivraison.getStatut() == StatutLivraison.LIVREE) {
            throw new IllegalStateException("Impossible de modifier une livraison livrée");
        }
        
        livraisonMapper.updateEntityFromDto(livraisonDTO, existingLivraison);
        return livraisonRepository.save(existingLivraison);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Suppression d'une livraison")
    public void deleteLivraison(Long id) {
        Livraison livraison = getLivraisonById(id);
        
        if (livraison.getStatut() == StatutLivraison.LIVREE) {
            throw new IllegalStateException("Impossible de supprimer une livraison livrée");
        }
        
        livraisonRepository.delete(livraison);
    }

    @MultitenantSearchMethod(description = "Recherche de livraisons par critères")
    public List<Livraison> searchLivraisons(LivraisonSearchCriteria criteria) {
        Long tenantId = TenantContext.getCurrentTenant();
        
        return livraisonRepository.findByCriteria(criteria, tenantId);
    }

    @Transactional
    public Livraison creerLivraison(Livraison livraison) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        Commande commande = commandeRepository.findByIdAndPointDeVente_Id(livraison.getCommande().getId(), pointDeVente.getId())
                .orElseThrow(RuntimeException::new);
        
        if (commande.getStatutLivraison() == StatutLivraison.LIVREE) {
            throw new IllegalStateException("Cette commande est déjà entièrement livrée");
        }

        // Store the original lines before clearing them
        List<LigneLivraison> originalLignes = new ArrayList<>(livraison.getLignesLivraison());
        livraison.getLignesLivraison().clear();

        livraison.setNumeroLivraison(generateNumeroLivraison());
        livraison.setPointDeVente(pointDeVente);
        livraison.setCommande(commande);
        livraison.setStatut(StatutLivraison.EN_ATTENTE);
        livraison.setDateLivraison(livraison.getDateLivraison() != null ?
                livraison.getDateLivraison() : LocalDateTime.now());

        // Save livraison first without lines
        livraison = livraisonRepository.save(livraison);

        BigDecimal montantTotal = BigDecimal.ZERO;
        if (!originalLignes.isEmpty()) {
            for (LigneLivraison ligne : originalLignes) {
                ligne = createLigneLivraison(livraison, ligne, pointDeVente.getId());
                montantTotal = montantTotal.add(ligne.getPrixProduit().multiply(
                        BigDecimal.valueOf(ligne.getQuantiteLivree())));
            }
        }

        livraison.setMontantTotal(montantTotal);
        livraison = livraisonRepository.save(livraison);

        mettreAJourStatutLivraisonCommande(commande);

        return livraison;
    }

    private LigneLivraison createLigneLivraison(Livraison livraison, LigneLivraison ligneEntity, Long pointDeVenteId) {
        Produit produit = produitRepository.findByIdAndPointDeVente_Id(ligneEntity.getProduit().getId(), pointDeVenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", ligneEntity.getProduit().getId()));

        Depot depot = null;
        if (ligneEntity.getDepot() != null && ligneEntity.getDepot().getId() != null) {
            depot = depotRepository.findByIdAndPointDeVente_Id(ligneEntity.getDepot().getId(), pointDeVenteId)
                    .orElse(null);
            if (depot == null) {
                log.warn("Depot with id {} not found for pointDeVente {} - falling back to first active depot", ligneEntity.getDepot().getId(), pointDeVenteId);
                List<Depot> depots = depotRepository.findByPointDeVente_IdAndActifTrue(pointDeVenteId);
                if (!depots.isEmpty()) {
                    depot = depots.get(0);
                } else {
                    log.warn("No active depots found for pointDeVente {} - depot will be left null", pointDeVenteId);
                }
            }
        } else {
            List<Depot> depots = depotRepository.findByPointDeVente_IdAndActifTrue(pointDeVenteId);
            if (depots.isEmpty()) {
                log.warn("No active depots found for pointDeVente {} - depot will be left null", pointDeVenteId);
            } else {
                depot = depots.get(0);
            }
        }

        ligneEntity.setLivraison(livraison);
        ligneEntity.setProduit(produit);
        ligneEntity.setDepot(depot);
        
        if (ligneEntity.getQualiteProduit() == null) {
            throw new IllegalArgumentException("La qualité du produit est obligatoire");
        }

        return ligneLivraisonRepository.save(ligneEntity);
    }

    private void mettreAJourStatutLivraisonCommande(Commande commande) {
        if (commande.getStatutLivraison() == StatutLivraison.LIVREE) {
            return;
        }
        
        List<LigneCommande> lignesCommande = ligneCommandeRepository.findByCommande_Id(commande.getId());
        List<Livraison> livraisons = livraisonRepository.findByCommande_IdAndStatut(commande.getId(), StatutLivraison.LIVREE);
        
        Map<Long, Double> quantitesLivrees = new HashMap<>();
        for (Livraison livraison : livraisons) {
            for (LigneLivraison ligneLivraison : livraison.getLignesLivraison()) {
                Long produitId = ligneLivraison.getProduit().getId();
                quantitesLivrees.merge(produitId, (double) ligneLivraison.getQuantiteLivree(), Double::sum);
            }
        }
        
        boolean toutLivre = true;
        boolean partiellementLivre = false;
        
        for (LigneCommande ligneCommande : lignesCommande) {
            Long produitId = ligneCommande.getProduit().getId();
            BigDecimal quantiteCommandee = BigDecimal.valueOf(ligneCommande.getQuantiteCommandee());
            double quantiteLivree = quantitesLivrees.getOrDefault(produitId, 0.0);
            
            if (quantiteLivree < quantiteCommandee.doubleValue()) {
                toutLivre = false;
            }
            if (quantiteLivree > 0) {
                partiellementLivre = true;
            }
        }
        
        if (toutLivre) {
            commande.setStatutLivraison(StatutLivraison.LIVREE);
        } else if (partiellementLivre) {
            commande.setStatutLivraison(StatutLivraison.EN_LIVRAISON);
        } else {
            commande.setStatutLivraison(StatutLivraison.EN_ATTENTE);
        }
        
        commandeRepository.save(commande);
    }

    private LivraisonDTO convertToDTO(Livraison livraison) {
        return livraisonMapper.toDto(livraison);
    }


}
