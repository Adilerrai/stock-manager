package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.ceramique.persistent.model.*;
import com.ceramique.persistent.enums.TypeMouvement;
import com.ceramique.persistent.enums.QualiteProduit;
import com.ceramique.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MouvementStockService {
    
    private final MouvementStockRepository mouvementStockRepository;
    private final StockRepository stockRepository;
    private final ProduitRepository produitRepository;
    private final DepotRepository depotRepository;
    private final StockService stockService;
    
    public MouvementStockService(MouvementStockRepository mouvementStockRepository,
                                StockRepository stockRepository,
                                ProduitRepository produitRepository,
                                DepotRepository depotRepository,
                                StockService stockService) {
        this.mouvementStockRepository = mouvementStockRepository;
        this.stockRepository = stockRepository;
        this.produitRepository = produitRepository;
        this.depotRepository = depotRepository;
        this.stockService = stockService;
    }
    
    @Transactional
    @MultitenantSearchMethod(description = "Création d'un mouvement de stock avec qualité et dépôt")
    public MouvementStock creerMouvement(Long produitId, Long depotId, TypeMouvement typeMouvement,
                                         BigDecimal quantite, QualiteProduit qualite,
                                         String referenceDocument, String motif) {
        // Charger le produit et calculer quantités
        Stock stock = stockRepository.findByProduitWithQualities(produitId).orElse(null);
        BigDecimal quantiteAvant = stock != null ? stock.getQuantiteTotaleDisponible() : BigDecimal.ZERO;
        appliquerMouvementStock(produitId, typeMouvement, quantite, qualite);
        stock = stockRepository.findByProduitWithQualities(produitId).orElse(null);
        BigDecimal quantiteApres = stock != null ? stock.getQuantiteTotaleDisponible() : BigDecimal.ZERO;

        Produit produit = produitRepository.findById(produitId).orElseThrow();

        // Résolution du depot
        Depot depot = null;
        if (depotId != null) {
            depot = depotRepository.findById(depotId).orElse(null);
        }

        if (depot == null) {
            List<Depot> all = depotRepository.findAll();
            for (Depot d : all) {
                if (d.getActif() != null && d.getActif()) { depot = d; break; }
            }
        }
        if (depot == null) {
            throw new RuntimeException("Aucun dépôt actif trouvé pour enregistrer le mouvement (produitId=" + produitId + ")");
        }

        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setTypeMouvement(typeMouvement);
        mouvement.setQuantite(quantite);
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setReferenceDocument(referenceDocument);
        mouvement.setMotif(motif);
        mouvement.setDateMouvement(LocalDateTime.now());
        mouvement.setUtilisateur(getCurrentUser());
        mouvement.setQualiteProduit(qualite);
        mouvement.setDepot(depot);
        return mouvementStockRepository.save(mouvement);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'un mouvement de stock avec qualité (sans depotId)")
    public MouvementStock creerMouvement(Long produitId, TypeMouvement typeMouvement,
                                         BigDecimal quantite, QualiteProduit qualite,
                                         String referenceDocument, String motif) {
        return creerMouvement(produitId, null, typeMouvement, quantite, qualite, referenceDocument, motif);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'un mouvement de stock (qualité par défaut)")
    public MouvementStock creerMouvement(Long produitId, Long depotId, TypeMouvement typeMouvement,
                                         BigDecimal quantite, String referenceDocument, String motif) {
        return creerMouvement(produitId, depotId, typeMouvement, quantite, QualiteProduit.PREMIERE_QUALITE, referenceDocument, motif);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'un mouvement de stock (qualité par défaut, sans depotId)")
    public MouvementStock creerMouvement(Long produitId, TypeMouvement typeMouvement,
                                         BigDecimal quantite, String referenceDocument, String motif) {
        return creerMouvement(produitId, null, typeMouvement, quantite, QualiteProduit.PREMIERE_QUALITE, referenceDocument, motif);
    }

    private void appliquerMouvementStock(Long produitId, TypeMouvement typeMouvement,
                                         BigDecimal quantite, QualiteProduit qualite) {
        switch (typeMouvement) {
            case ENTREE_LIVRAISON, AJUSTEMENT_POSITIF, TRANSFERT_ENTREE ->
                stockService.ajouterStockParQualite(produitId, qualite, quantite);
            case SORTIE_VENTE, SORTIE_COMMANDE, AJUSTEMENT_NEGATIF, TRANSFERT_SORTIE ->
                stockService.retirerStockParQualite(produitId, qualite, quantite);
            case INVENTAIRE -> {
                Stock stock = stockRepository.findByProduitWithQualities(produitId).orElse(null);
                if (stock != null) {
                    StockQualite sq = stock.getStockByQualite(qualite);
                    if (sq != null) {
                        sq.setQuantiteDisponible(quantite);
                        stockRepository.save(stock);
                    }
                }
            }
        }
    }

    private String getCurrentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
    
    @MultitenantSearchMethod(description = "Historique des mouvements par produit")
    public List<MouvementStock> getHistoriqueProduit(Long produitId) {
        return mouvementStockRepository.findByProduitIdOrderByDateMouvementDesc(produitId);
    }
}