package com.gestion.service;

import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.model.Produit;
import com.gestion.persistent.model.Stock;
import com.gestion.persistent.model.StockQualite;
import com.gestion.repository.ProduitRepository;
import com.gestion.repository.StockQualiteRepository;
import com.gestion.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.mapper.StockMapper;
import com.gestion.persistent.dto.StockDTO;
import com.gestion.persistent.dto.StockSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final StockQualiteRepository stockQualiteRepository;
    private final ProduitRepository produitRepository;
    private final EntrepriseProfileService entrepriseProfileService;
    private final StockMapper stockMapper;

    public StockService(StockRepository stockRepository,
                        StockQualiteRepository stockQualiteRepository,
                        ProduitRepository produitRepository,
                        EntrepriseProfileService entrepriseProfileService) {
        this(stockRepository, stockQualiteRepository, produitRepository, entrepriseProfileService, null);
    }

    @Autowired
    public StockService(StockRepository stockRepository,
                        StockQualiteRepository stockQualiteRepository,
                        ProduitRepository produitRepository,
                        EntrepriseProfileService entrepriseProfileService,
                        StockMapper stockMapper) {
        this.stockRepository = stockRepository;
        this.stockQualiteRepository = stockQualiteRepository;
        this.produitRepository = produitRepository;
        this.entrepriseProfileService = entrepriseProfileService;
        this.stockMapper = stockMapper;
    }

    // =========================================================================
    // GESTION UNIFIÉE DU STOCK (SANS DISTINCTION DE QUALITÉ)
    // =========================================================================

    public Stock initializeStock(Long produitId, BigDecimal quantiteInitiale, BigDecimal seuilAlerte) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        Stock stock = stockRepository.findByProduitId(produitId).orElse(null);
        if (stock == null) {
            stock = new Stock();
            stock.setProduit(produit);
        }
        stock.setQuantiteDisponible(quantiteInitiale != null ? quantiteInitiale : BigDecimal.ZERO);
        stock.setSeuilAlerte(seuilAlerte != null ? seuilAlerte : BigDecimal.ZERO);
        stock.setDerniereMaj(LocalDateTime.now());
        return stockRepository.save(stock);
    }

    public Stock ajouterStock(Long produitId, BigDecimal quantite) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        if (quantite == null || quantite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantité doit être > 0");
        }

        Stock stock = stockRepository.findByProduitId(produitId).orElse(null);
        if (stock == null) {
            stock = new Stock();
            stock.setProduit(produit);
            stock.setQuantiteDisponible(BigDecimal.ZERO);
        }

        stock.setQuantiteDisponible(stock.getQuantiteDisponible().add(quantite));
        stock.setDerniereMaj(LocalDateTime.now());
        return stockRepository.save(stock);
    }

    public Stock retirerStock(Long produitId, BigDecimal quantite) {
        return retirerStock(produitId, quantite, false);
    }

    public Stock retirerStock(Long produitId, BigDecimal quantite, boolean depuisReservation) {
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
            stock.setQuantiteDisponible(BigDecimal.ZERO);
            stock.setQuantiteReservee(BigDecimal.ZERO);
        }

        BigDecimal reservee = stock.getQuantiteReservee() != null ? stock.getQuantiteReservee() : BigDecimal.ZERO;
        BigDecimal disponible = stock.getQuantiteDisponible() != null ? stock.getQuantiteDisponible() : BigDecimal.ZERO;

        if (depuisReservation) {
            BigDecimal totalPhysique = disponible.add(reservee);
            if (!allowNegative && totalPhysique.compareTo(quantite) < 0) {
                String nomProd = stock.getProduit() != null && stock.getProduit().getNom() != null 
                        ? stock.getProduit().getNom() 
                        : (stock.getProduit() != null && stock.getProduit().getDesignation() != null 
                                ? stock.getProduit().getDesignation() 
                                : String.valueOf(produitId));
                throw new IllegalArgumentException(String.format(
                        "Stock physique insuffisant pour le produit %s (Total en stock: %s, Demandé: %s)",
                        nomProd, totalPhysique, quantite));
            }

            // Déduire en priorité de la réserve liée à la commande
            if (reservee.compareTo(quantite) >= 0) {
                stock.setQuantiteReservee(reservee.subtract(quantite));
            } else {
                BigDecimal reste = quantite.subtract(reservee);
                stock.setQuantiteReservee(BigDecimal.ZERO);
                stock.setQuantiteDisponible(disponible.subtract(reste));
            }
        } else {
            if (!allowNegative && disponible.compareTo(quantite) < 0) {
                String nomProd = stock.getProduit() != null && stock.getProduit().getNom() != null 
                        ? stock.getProduit().getNom() 
                        : (stock.getProduit() != null && stock.getProduit().getDesignation() != null 
                                ? stock.getProduit().getDesignation() 
                                : String.valueOf(produitId));
                throw new IllegalArgumentException(String.format(
                        "Stock insuffisant pour le produit %s (Disponible: %s, Demandé: %s)",
                        nomProd, disponible, quantite));
            }
            stock.setQuantiteDisponible(disponible.subtract(quantite));
        }

        // Synchroniser également les stock_qualites si présents
        if (stock.getStocksQualite() != null && !stock.getStocksQualite().isEmpty()) {
            for (StockQualite sq : stock.getStocksQualite()) {
                if (depuisReservation) {
                    BigDecimal sqRes = sq.getQuantiteReservee() != null ? sq.getQuantiteReservee() : BigDecimal.ZERO;
                    if (sqRes.compareTo(quantite) >= 0) {
                        sq.setQuantiteReservee(sqRes.subtract(quantite));
                    } else {
                        BigDecimal reste = quantite.subtract(sqRes);
                        sq.setQuantiteReservee(BigDecimal.ZERO);
                        BigDecimal sqDispo = sq.getQuantiteDisponible() != null ? sq.getQuantiteDisponible() : BigDecimal.ZERO;
                        sq.setQuantiteDisponible(sqDispo.subtract(reste));
                    }
                } else {
                    BigDecimal sqDispo = sq.getQuantiteDisponible() != null ? sq.getQuantiteDisponible() : BigDecimal.ZERO;
                    sq.setQuantiteDisponible(sqDispo.subtract(quantite));
                }
            }
        }

        stock.setDerniereMaj(LocalDateTime.now());
        return stockRepository.save(stock);
    }

    public boolean reserverStock(Long produitId, BigDecimal quantite) {
        boolean allowNegative = entrepriseProfileService.isVenteStockNegatifAutorisee();

        Stock stock = stockRepository.findByProduitId(produitId).orElse(null);
        if (stock == null) {
            if (!allowNegative) return false;
            Produit produit = produitRepository.findById(produitId).orElse(null);
            if (produit == null) return false;
            stock = new Stock();
            stock.setProduit(produit);
            stock.setQuantiteDisponible(BigDecimal.ZERO);
        }

        if (!allowNegative && stock.getQuantiteDisponible().compareTo(quantite) < 0) {
            return false;
        }

        stock.setQuantiteDisponible(stock.getQuantiteDisponible().subtract(quantite));
        stock.setQuantiteReservee(stock.getQuantiteReservee().add(quantite));
        stock.setDerniereMaj(LocalDateTime.now());
        stockRepository.save(stock);
        return true;
    }

    public boolean libererStock(Long produitId, BigDecimal quantite) {
        Stock stock = stockRepository.findByProduitId(produitId).orElse(null);
        if (stock == null) return false;

        BigDecimal reservee = stock.getQuantiteReservee().subtract(quantite);
        stock.setQuantiteReservee(reservee.compareTo(BigDecimal.ZERO) >= 0 ? reservee : BigDecimal.ZERO);
        stock.setQuantiteDisponible(stock.getQuantiteDisponible().add(quantite));
        stock.setDerniereMaj(LocalDateTime.now());
        stockRepository.save(stock);
        return true;
    }

    public void sortieStock(Long produitId, BigDecimal quantite, String motif) {
        retirerStock(produitId, quantite);
    }

    public void entreeStock(Long produitId, BigDecimal quantite, String motif) {
        ajouterStock(produitId, quantite);
    }

    @Transactional(readOnly = true)
    public Stock getStockByProduit(Long produitId) {
        produitRepository.findById(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", produitId));

        return stockRepository.findByProduitId(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "produitId", produitId));
    }

    @Transactional(readOnly = true)
    public List<Stock> getAllStocks() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return stockRepository.findByPointDeVenteId(tenantId);
    }

    // =========================================================================
    // MÉTHODES DE RÉTRO-COMPATIBILITÉ (DÉLÉGUENT AU STOCK UNIFIÉ)
    // =========================================================================

    public Stock initializeStockWithQualities(Long produitId,
            Map<QualiteProduit, BigDecimal> quantitesParQualite, BigDecimal seuilAlerte) {
        BigDecimal total = BigDecimal.ZERO;
        if (quantitesParQualite != null) {
            for (BigDecimal q : quantitesParQualite.values()) {
                if (q != null) total = total.add(q);
            }
        }
        return initializeStock(produitId, total, seuilAlerte);
    }

    public Stock ajouterStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite) {
        return ajouterStock(produitId, quantite);
    }

    public Stock retirerStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite) {
        return retirerStock(produitId, quantite, false);
    }

    public Stock retirerStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite, boolean depuisReservation) {
        return retirerStock(produitId, quantite, depuisReservation);
    }

    public boolean reserverStockParQualite(Long produitId, QualiteProduit qualite, BigDecimal quantite) {
        return reserverStock(produitId, quantite);
    }

    @Transactional(readOnly = true)
    public List<Stock> getAllStocksWithQualities() {
        return getAllStocks();
    }

    @Transactional(readOnly = true)
    public List<StockQualite> getStocksByQualite(QualiteProduit qualite) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return stockQualiteRepository.findByQualiteAndTenant(qualite, tenantId);
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<StockQualite> getStocksQualiteEnAlerte() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return stockQualiteRepository.findStocksEnAlerteByTenant(tenantId);
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public Page<StockDTO> searchStocks(StockSearchCriteria criteria, Pageable pageable) {
        Page<Stock> stockPage = stockRepository.findByCriteria(criteria, pageable);

        return stockPage.map(s -> {
            StockDTO dto = stockMapper != null ? stockMapper.toDto(s) : new StockDTO();
            BigDecimal dispo = s.getQuantiteDisponible() != null ? s.getQuantiteDisponible() : BigDecimal.ZERO;
            BigDecimal seuil = s.getSeuilAlerte() != null ? s.getSeuilAlerte() : BigDecimal.ZERO;
            if (dispo.compareTo(BigDecimal.ZERO) <= 0) {
                dto.setStatutStock("RUPTURE");
            } else if (seuil.compareTo(BigDecimal.ZERO) > 0 && dispo.compareTo(seuil) <= 0) {
                dto.setStatutStock("ALERTE");
            } else {
                dto.setStatutStock("OK");
            }
            return dto;
        });
    }
}
