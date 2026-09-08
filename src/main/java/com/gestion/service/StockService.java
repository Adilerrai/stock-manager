package com.gestion.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.model.Produit;
import com.gestion.persistent.model.Stock;
import com.gestion.persistent.model.StockQualite;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.repository.ProduitRepository;
import com.gestion.repository.StockRepository;
import com.gestion.repository.StockQualiteRepository;
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
    private final EntrepriseProfileService entrepriseProfileService;

    public StockService(StockRepository stockRepository,
                        StockQualiteRepository stockQualiteRepository,
                        ProduitRepository produitRepository,
                        EntrepriseProfileService entrepriseProfileService) {
        this.stockRepository = stockRepository;
        this.stockQualiteRepository = stockQualiteRepository;
        this.produitRepository = produitRepository;
        this.entrepriseProfileService = entrepriseProfileService;
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
        boolean allowNegative = entrepriseProfileService.isVenteStockNegatifAutorisee();

        Stock stock = stockRepository.findByProduitId(produitId).orElse(null);
        if (stock == null) {
            if (!allowNegative) {
                throw new ResourceNotFoundException("Stock", "produitId", produitId);
            }
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));
            stock = new Stock();
            stock.setProduit(produit);
        }

        StockQualite stockQualite = stock.getStockByQualite(qualite);
        if (stockQualite == null) {
            if (!allowNegative) {
                throw new IllegalArgumentException("Stock qualité non trouvé");
            }
            stockQualite = new StockQualite();
            stockQualite.setStock(stock);
            stockQualite.setProduit(stock.getProduit());
            stockQualite.setQualite(qualite);
            stockQualite.setQuantiteDisponible(BigDecimal.ZERO.subtract(quantite));
            stock.ajouterStockQualite(stockQualite);
        } else {
            if (!allowNegative && stockQualite.getQuantiteDisponible().compareTo(quantite) < 0) {
                throw new IllegalArgumentException("Stock insuffisant pour la qualité " + qualite);
            }
            stockQualite.setQuantiteDisponible(stockQualite.getQuantiteDisponible().subtract(quantite));
        }

        return stockRepository.save(stock);
    }

    @Transactional
    public boolean reserverStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite) {
        boolean allowNegative = entrepriseProfileService.isVenteStockNegatifAutorisee();

        Stock stock = stockRepository.findByProduitId(produitId).orElse(null);
        if (stock == null) {
            if (!allowNegative) {
                return false;
            }
            Produit produit = produitRepository.findById(produitId).orElse(null);
            if (produit == null) {
                return false;
            }
            stock = new Stock();
            stock.setProduit(produit);
        }

        StockQualite stockQualite = stock.getStockByQualite(qualite);
        if (stockQualite == null) {
            if (!allowNegative) {
                return false;
            }
            stockQualite = new StockQualite();
            stockQualite.setStock(stock);
            stockQualite.setProduit(stock.getProduit());
            stockQualite.setQualite(qualite);
            stockQualite.setQuantiteDisponible(BigDecimal.ZERO.subtract(quantite));
            stockQualite.setQuantiteReservee(quantite);
            stock.ajouterStockQualite(stockQualite);
            stockRepository.save(stock);
            return true;
        }

        if (allowNegative && stockQualite.getQuantiteDisponible().compareTo(quantite) < 0) {
            stockQualite.setQuantiteDisponible(stockQualite.getQuantiteDisponible().subtract(quantite));
            stockQualite.setQuantiteReservee(
                    (stockQualite.getQuantiteReservee() != null ? stockQualite.getQuantiteReservee() : BigDecimal.ZERO).add(quantite)
            );
            stockRepository.save(stock);
            return true;
        }

        boolean success = stockQualite.reserverStock(quantite);
        if (success) {
            stockRepository.save(stock);
        }

        return success;
    }

    public List<Stock> getAllStocksWithQualities() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return List.of();
        }
        return stockRepository.findWithQualitiesByPointDeVenteId(tenantId);
    }

    public List<StockQualite> getStocksByQualite(QualiteProduit qualite) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return stockQualiteRepository.findByQualiteAndTenant(qualite, tenantId);
        }
        return List.of();
    }

    public List<StockQualite> getStocksQualiteEnAlerte() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return stockQualiteRepository.findStocksEnAlerteByTenant(tenantId);
        }
        return List.of();
    }

    public List<Stock> getAllStocks() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return List.of();
        }
        return stockRepository.findWithQualitiesByPointDeVenteId(tenantId);
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

