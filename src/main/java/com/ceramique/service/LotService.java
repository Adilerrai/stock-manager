package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.ceramique.persistent.model.*;
import com.ceramique.persistent.enums.QualiteProduit;
import com.ceramique.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LotService {
    private static final Logger log = LoggerFactory.getLogger(LotService.class);

    private final LotRepository lotRepository;
    private final ProduitRepository produitRepository;
    private final DepotRepository depotRepository;

    public LotService(LotRepository lotRepository, ProduitRepository produitRepository, DepotRepository depotRepository) {
        this.lotRepository = lotRepository;
        this.produitRepository = produitRepository;
        this.depotRepository = depotRepository;
    }

    @Transactional
    public Lot creerLot(Long produitId, Long depotId, QualiteProduit qualite,
                       BigDecimal quantite, String numeroLivraison, String fournisseur, 
                       BigDecimal prixAchat) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec id : '" + produitId + "'"));

        Depot depot = null;
        // If a depotId is provided, try to load it
        if (depotId != null) {
            Optional<Depot> depotOpt = depotRepository.findById(depotId);
            if (depotOpt.isPresent()) {
                depot = depotOpt.get();
            } else {
                log.warn("Depot id {} not found, will try to fallback to an active depot for the product's pointDeVente", depotId);
            }
        }


        // If still null, fallback to any active depot global
        if (depot == null) {
            List<Depot> allDepots = depotRepository.findAll();
            for (Depot d : allDepots) {
                if (d.getActif() != null && d.getActif()) {
                    depot = d;
                    log.info("Using global fallback active depot {}", depot.getId());
                    break;
                }
            }
        }

        if (depot == null) {
            throw new RuntimeException("Aucun dépôt actif trouvé pour créer le lot (depotId fourni: " + depotId + ")");
        }

        Lot lot = new Lot();
        lot.setNumeroLot(generateNumeroLot());
        lot.setProduit(produit);
        lot.setDepot(depot);
        lot.setQualite(qualite);
        lot.setQuantiteInitiale(quantite);
        lot.setQuantiteDisponible(quantite);
        lot.setDateReception(LocalDateTime.now());
        lot.setNumeroLivraison(numeroLivraison);
        lot.setFournisseur(fournisseur);
        lot.setPrixAchatUnitaire(prixAchat);

        return lotRepository.save(lot);
    }

    public List<Lot> consommerStock(Long produitId, Long depotId, QualiteProduit qualite, BigDecimal quantiteAConsommer) {
        List<Lot> lotsDisponibles = lotRepository.findByProduitIdAndDepotIdAndQualiteAndQuantiteDisponibleGreaterThanOrderByDateReception(
            produitId, depotId, qualite, BigDecimal.ZERO);
        
        // Logique FIFO pour consommer les lots
        // ... implémentation de la consommation
        
        return lotsDisponibles;
    }
    
    private String generateNumeroLot() {
        return "LOT-" + System.currentTimeMillis();
    }
    
    public List<Lot> getLotsByProduitAndDepot(Long produitId, Long depotId) {
        return lotRepository.findByProduitIdAndDepotIdAndActifTrueOrderByDateReception(produitId, depotId);
    }
}