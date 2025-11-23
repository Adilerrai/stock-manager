package com.ceramique.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.ceramique.persistent.enums.StatutVente;
import com.ceramique.persistent.model.*;
import com.ceramique.repository.LigneVenteRepository;
import com.ceramique.repository.ProduitRepository;
import com.ceramique.repository.VenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class VenteService {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final ProduitRepository produitRepository;
    private final ClientService clientService;
    private final StockService stockService;
    private final UserRepository userRepository;

    public VenteService(VenteRepository venteRepository,
                        LigneVenteRepository ligneVenteRepository,
                        ProduitRepository produitRepository,
                        ClientService clientService,
                        StockService stockService,
                        UserRepository userRepository) {
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.produitRepository = produitRepository;
        this.clientService = clientService;
        this.stockService = stockService;
        this.userRepository = userRepository;
    }

    public Vente creerVente(Vente vente, Long vendeurId) {

        User vendeur = userRepository.findById(vendeurId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));

        vente.setVendeur(vendeur);
        vente.setNumeroTicket(genererNumeroTicket());
        vente.setDateVente(LocalDateTime.now());
        vente.setStatut(StatutVente.EN_COURS);

        return venteRepository.save(vente);
    }

    public Vente ajouterLigneVente(Long venteId, LigneVente ligneVente) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée"));

        if (vente.getStatut() != StatutVente.EN_COURS) {
            throw new RuntimeException("Impossible de modifier une vente qui n'est pas en cours");
        }

        Produit produit = produitRepository.findById(ligneVente.getProduit().getId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        ligneVente.setVente(vente);
        ligneVente.setProduit(produit);
        ligneVente.setDesignation(produit.getDesignation());
        ligneVente.setReference(produit.getReference());
        ligneVente.setPrixUnitaireHT(produit.getPrixVente());

        // Calcul automatique des montants
        ligneVente.calculerMontants();

        vente.addLigne(ligneVente);
        vente.calculerMontants();

        return venteRepository.save(vente);
    }

    public Vente supprimerLigneVente(Long venteId, Long ligneId) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée"));

        if (vente.getStatut() != StatutVente.EN_COURS) {
            throw new RuntimeException("Impossible de modifier une vente qui n'est pas en cours");
        }

        LigneVente ligne = ligneVenteRepository.findById(ligneId)
                .orElseThrow(() -> new RuntimeException("Ligne non trouvée"));

        vente.removeLigne(ligne);
        ligneVenteRepository.delete(ligne);
        vente.calculerMontants();

        return venteRepository.save(vente);
    }

    public Vente validerVente(Long venteId) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée"));

        if (vente.getLignes().isEmpty()) {
            throw new RuntimeException("Impossible de valider une vente sans lignes");
        }

        // Déduire le stock
        for (LigneVente ligne : vente.getLignes()) {
            stockService.sortieStock(ligne.getProduit().getId(), ligne.getQuantite(),
                                    "Vente " + vente.getNumeroTicket());
        }

        vente.setStatut(StatutVente.VALIDEE);

        // Mettre à jour la date de dernière visite du client
        if (vente.getClient() != null) {
            clientService.mettreAJourDerniereVisite(vente.getClient().getId());
        }

        return venteRepository.save(vente);
    }

    public Vente annulerVente(Long venteId, String motif, Long userId) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Remettre le stock si la vente était validée
        if (vente.getStatut() == StatutVente.VALIDEE) {
            for (LigneVente ligne : vente.getLignes()) {
                stockService.entreeStock(ligne.getProduit().getId(), ligne.getQuantite(),
                                        "Annulation vente " + vente.getNumeroTicket());
            }
        }

        vente.setStatut(StatutVente.ANNULEE);
        vente.setDateAnnulation(LocalDateTime.now());
        vente.setMotifAnnulation(motif);
        vente.setAnnulePar(user);

        return venteRepository.save(vente);
    }

    public Vente getVenteById(Long id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée"));
    }

    public List<Vente> getAllVentes() {
        return venteRepository.findAll();
    }

    public List<Vente> getVentesByClient(Long clientId) {
        return venteRepository.findByClientId(clientId);
    }

    public List<Vente> getVentesByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return venteRepository.findVentesByPeriode(dateDebut, dateFin);
    }

    public List<Vente> getVentesNonSoldees() {
        return venteRepository.findVentesNonSoldees();
    }

    public BigDecimal calculerChiffreAffaires(LocalDateTime dateDebut, LocalDateTime dateFin) {
        BigDecimal ca = venteRepository.calculerChiffreAffaires( dateDebut, dateFin);
        return ca != null ? ca : BigDecimal.ZERO;
    }

    private String genererNumeroTicket() {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = venteRepository.count() + 1;
        return "TK-" + pointDeVenteId + "-" + dateStr + "-" + String.format("%06d", count);
    }

    public Vente appliquerRemiseGlobale(Long venteId, BigDecimal remise) {
        Vente vente = getVenteById(venteId);
        vente.setRemiseGlobale(remise);
        vente.calculerMontants();
        return venteRepository.save(vente);
    }
}

