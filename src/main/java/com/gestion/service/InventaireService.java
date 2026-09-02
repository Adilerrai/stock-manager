package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.enums.StatutInventaire;
import com.gestion.persistent.enums.TypeMouvement;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InventaireService {

    private final InventaireRepository inventaireRepository;
    private final LigneInventaireRepository ligneInventaireRepository;
    private final DepotRepository depotRepository;
    private final ProduitRepository produitRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final MouvementStockService mouvementStockService;

    public InventaireService(InventaireRepository inventaireRepository,
                             LigneInventaireRepository ligneInventaireRepository,
                             DepotRepository depotRepository,
                             ProduitRepository produitRepository,
                             StockRepository stockRepository,
                             UserRepository userRepository,
                             MouvementStockService mouvementStockService) {
        this.inventaireRepository = inventaireRepository;
        this.ligneInventaireRepository = ligneInventaireRepository;
        this.depotRepository = depotRepository;
        this.produitRepository = produitRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
        this.mouvementStockService = mouvementStockService;
    }

    public Inventaire creerInventaire(Long depotId, String notes, Long userId) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + depotId));

        Inventaire inv = new Inventaire();
        inv.setReference(genererReference());
        inv.setDateInventaire(LocalDate.now());
        inv.setDateCreation(LocalDateTime.now());
        inv.setDepot(depot);
        inv.setStatut(StatutInventaire.EN_COURS);
        inv.setNotes(notes);

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            inv.setResponsable(user);
        }

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            inv.setPointDeVenteId(tenantId);
        }

        List<Produit> produits = produitRepository.findAll();
        List<LigneInventaire> lignes = new ArrayList<>();

        for (Produit p : produits) {
            if (Boolean.TRUE.equals(p.getActif())) {
                LigneInventaire li = new LigneInventaire();
                li.setInventaire(inv);
                li.setProduit(p);
                li.setQualite(QualiteProduit.PREMIERE_QUALITE);

                Stock stock = stockRepository.findByProduitWithQualities(p.getId()).orElse(null);
                BigDecimal qteTheo = stock != null && stock.getQuantiteTotaleDisponible() != null ?
                        stock.getQuantiteTotaleDisponible() : BigDecimal.ZERO;
                li.setQuantiteTheorique(qteTheo);
                li.setQuantiteReelle(qteTheo); // Par défaut pré-rempli avec théorique

                BigDecimal prix = p.getPrixAchatHt() != null ? p.getPrixAchatHt() :
                        (p.getPrixAchat() != null ? p.getPrixAchat() : BigDecimal.ZERO);
                li.setPrixUnitaire(prix);
                li.calculerEcart();
                lignes.add(li);
            }
        }

        inv.setLignes(lignes);
        inv.calculerTotaux();
        return inventaireRepository.save(inv);
    }

    public Inventaire mettreAJourLignes(Long inventaireId, List<LigneInventaire> lignesMaj) {
        Inventaire inv = getInventaireById(inventaireId);
        if (inv.getStatut() == StatutInventaire.VALIDE) {
            throw new RuntimeException("Impossible de modifier un inventaire déjà validé");
        }

        for (LigneInventaire maj : lignesMaj) {
            if (maj.getId() != null) {
                LigneInventaire existante = ligneInventaireRepository.findById(maj.getId()).orElse(null);
                if (existante != null) {
                    existante.setQuantiteReelle(maj.getQuantiteReelle());
                    if (maj.getQualite() != null) existante.setQualite(maj.getQualite());
                    existante.calculerEcart();
                    ligneInventaireRepository.save(existante);
                }
            }
        }

        inv.calculerTotaux();
        return inventaireRepository.save(inv);
    }

    public Inventaire validerInventaire(Long inventaireId, Long userId) {
        Inventaire inv = getInventaireById(inventaireId);
        if (inv.getStatut() == StatutInventaire.VALIDE) {
            throw new RuntimeException("L'inventaire est déjà validé");
        }

        Long depotId = inv.getDepot().getId();

        if (inv.getLignes() != null) {
            for (LigneInventaire li : inv.getLignes()) {
                li.calculerEcart();
                BigDecimal ecart = li.getEcart();

                if (ecart != null && ecart.compareTo(BigDecimal.ZERO) != 0) {
                    if (ecart.compareTo(BigDecimal.ZERO) > 0) {
                        mouvementStockService.creerMouvement(
                                li.getProduit().getId(),
                                depotId,
                                TypeMouvement.INVENTAIRE,
                                ecart,
                                li.getQualite(),
                                inv.getReference(),
                                "Ajustement inventaire positif " + inv.getReference()
                        );
                    } else {
                        mouvementStockService.creerMouvement(
                                li.getProduit().getId(),
                                depotId,
                                TypeMouvement.AJUSTEMENT_NEGATIF,
                                ecart.abs(),
                                li.getQualite(),
                                inv.getReference(),
                                "Ajustement inventaire négatif " + inv.getReference()
                        );
                    }
                }
            }
        }

        inv.setStatut(StatutInventaire.VALIDE);
        inv.setDateValidation(LocalDateTime.now());
        inv.calculerTotaux();
        return inventaireRepository.save(inv);
    }

    public Inventaire getInventaireById(Long id) {
        return inventaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventaire non trouvé: " + id));
    }

    public List<Inventaire> getAllInventaires() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return inventaireRepository.findByPointDeVenteIdOrderByDateInventaireDesc(tenantId);
        }
        return inventaireRepository.findAll();
    }

    public List<Inventaire> getInventairesByDepot(Long depotId) {
        return inventaireRepository.findByDepotIdOrderByDateInventaireDesc(depotId);
    }

    public void annulerInventaire(Long id) {
        Inventaire inv = getInventaireById(id);
        if (inv.getStatut() == StatutInventaire.VALIDE) {
            throw new RuntimeException("Impossible d'annuler un inventaire déjà validé");
        }
        inv.setStatut(StatutInventaire.ANNULE);
        inventaireRepository.save(inv);
    }

    private String genererReference() {
        String prefixe = "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = inventaireRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}
