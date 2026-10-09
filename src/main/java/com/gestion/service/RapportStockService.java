package com.gestion.service;

import com.gestion.persistent.dto.AnalyseProduitStockDTO;
import com.gestion.persistent.dto.RapportAnalyseStockDTO;
import com.gestion.repository.CategorieRepository;
import com.gestion.repository.MouvementStockRepository;
import com.gestion.repository.ProduitRepository;
import com.gestion.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RapportStockService {

    private final StockRepository stockRepository;
    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final MouvementStockRepository mouvementStockRepository;

    public RapportStockService(StockRepository stockRepository,
                               ProduitRepository produitRepository,
                               CategorieRepository categorieRepository,
                               MouvementStockRepository mouvementStockRepository) {
        this.stockRepository = stockRepository;
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
        this.mouvementStockRepository = mouvementStockRepository;
    }

    /**
     * Rapport d'analyse du stock : laissé vide à la demande de l'utilisateur (aucune donnée factice).
     */
    public RapportAnalyseStockDTO genererRapportAnalyse(Long categorieIdFiltre, Long depotIdFiltre, Integer joursHistorique) {
        return new RapportAnalyseStockDTO();
    }

    /**
     * Liste des recommandations urgentes de réapprovisionnement : laissée vide (aucune donnée factice).
     */
    public List<AnalyseProduitStockDTO> getRecommandationsReapprovisionnement() {
        return Collections.emptyList();
    }
}
