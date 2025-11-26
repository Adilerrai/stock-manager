package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.ceramique.persistent.model.Produit;
import com.ceramique.persistent.model.Stock;
import com.ceramique.persistent.model.StockQualite;
import com.ceramique.persistent.enums.QualiteProduit;
import com.ceramique.repository.ProduitRepository;
import com.ceramique.repository.StockRepository;
import com.ceramique.repository.StockQualiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockQualiteRepository stockQualiteRepository;
    private final ProduitRepository produitRepository;

    public StockService(StockRepository stockRepository, StockQualiteRepository stockQualiteRepository, ProduitRepository produitRepository) {
        this.stockRepository = stockRepository;
        this.stockQualiteRepository = stockQualiteRepository;
        this.produitRepository = produitRepository;
    }

    @Transactional
    public Stock initializeStockWithQualities(Long produitId,
            Map<QualiteProduit, BigDecimal> quantitesParQualite, BigDecimal seuilAlerte) {
        

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        // Vérifier si le stock existe déjà
        if (stockRepository.findByProduitId(produitId).isPresent()) {
            throw new IllegalArgumentException("Le stock existe déjà pour ce produit");
        }

        // Créer le stock principal
        Stock stock = new Stock();
        stock.setProduit(produit);

        // Créer les stocks par qualité
        for (Map.Entry<QualiteProduit, BigDecimal> entry : quantitesParQualite.entrySet()) {
            StockQualite sq = new StockQualite();
            sq.setStock(stock);
            sq.setProduit(produit); // ajout
            sq.setQualite(entry.getKey());
            sq.setQuantiteDisponible(entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO);
            sq.setSeuilAlerte(seuilAlerte);
            stock.ajouterStockQualite(sq);
        }
        
        return stockRepository.save(stock);
    }

    @Transactional
    public Stock ajouterStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        if (quantite == null || quantite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantité doit être > 0");
        }
        // Auto-création du stock si absent
        Stock stock = stockRepository.findByProduitId(produitId).orElse(null);
        if (stock == null) {
                         stock = new Stock();
            stock.setProduit(produit);
        }
        StockQualite stockQualite = stock.getStockByQualite(qualite);
        if (stockQualite == null) {
            stockQualite = new StockQualite();
            stockQualite.setStock(stock);
            stockQualite.setProduit(stock.getProduit()); // ajout
            stockQualite.setQualite(qualite);
            stockQualite.setQuantiteDisponible(quantite);
        stock.ajouterStockQualite(stockQualite);
        } else {
            stockQualite.setQuantiteDisponible(stockQualite.getQuantiteDisponible().add(quantite));
        }
        return stockRepository.save(stock);
    }

    @Transactional
    public Stock retirerStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite) {

        Stock stock = getStockByProduit(produitId);

        StockQualite stockQualite = stock.getStockByQualite(qualite);
        if (stockQualite == null) {
            throw new IllegalArgumentException("Stock qualité non trouvé");
        }
        
        if (stockQualite.getQuantiteDisponible().compareTo(quantite) < 0) {
            throw new IllegalArgumentException("Stock insuffisant pour la qualité " + qualite);
        }
        
        stockQualite.setQuantiteDisponible(stockQualite.getQuantiteDisponible().subtract(quantite));
        
        return stockRepository.save(stock);
    }

    @Transactional
    public boolean reserverStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite) {

        Stock stock = getStockByProduit(produitId);

        StockQualite stockQualite = stock.getStockByQualite(qualite);
        if (stockQualite == null) {
            return false;
        }
        
        boolean success = stockQualite.reserverStock(quantite);
        if (success) {
            stockRepository.save(stock);
        }
        
        return success;
    }

    public List<Stock> getAllStocksWithQualities() {
        return stockRepository.findWithQualities();
    }

    public List<StockQualite> getStocksByQualite(QualiteProduit qualite) {

        return stockQualiteRepository.findByQualite( qualite);
    }

    public List<StockQualite> getStocksQualiteEnAlerte() {

        return stockQualiteRepository.findStocksEnAlerte();
    }



    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    private Stock getStockByProduit(Long produitId) {
        produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        return stockRepository.findByProduitId(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "produitId", produitId));
    }

    @Transactional
    public void sortieStock(Long produitId, BigDecimal quantite, String motif) {
        // Sortir du stock avec qualité PREMIERE_QUALITE par défaut
        retirerStockParQualite(produitId, QualiteProduit.PREMIERE_QUALITE, quantite);
    }

    @Transactional
    public void entreeStock(Long produitId, BigDecimal quantite, String motif) {
        // Ajouter au stock avec qualité PREMIERE_QUALITE par défaut
        ajouterStockParQualite(produitId, QualiteProduit.PREMIERE_QUALITE, quantite);
    }

}
