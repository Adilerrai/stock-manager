package com.ceramique.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.ceramique.persistent.enums.StatutFacture;
import com.ceramique.persistent.model.*;
import com.ceramique.repository.FactureRepository;
import com.ceramique.repository.LigneFactureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FactureService {

    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final ClientService clientService;
    private final UserRepository userRepository;

    public FactureService(FactureRepository factureRepository,
                         LigneFactureRepository ligneFactureRepository,
                         ClientService clientService,
                         UserRepository userRepository) {
        this.factureRepository = factureRepository;
        this.ligneFactureRepository = ligneFactureRepository;
        this.clientService = clientService;
        this.userRepository = userRepository;
    }

    public Facture creerFacture(Facture facture, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        facture.setEmisePar(user);
        facture.setNumeroFacture(genererNumeroFacture());
        facture.setDateFacture(LocalDate.now());
        facture.setDateCreation(LocalDateTime.now());
        facture.setStatut(StatutFacture.EN_ATTENTE);

        return factureRepository.save(facture);
    }

    public Facture creerFactureDepuisVente(Vente vente, Long userId) {
        Facture facture = new Facture();
        facture.setClient(vente.getClient());
        facture.setVente(vente);

        facture = creerFacture(facture, userId);

        // Copier les lignes de vente vers la facture
        for (LigneVente ligneVente : vente.getLignes()) {
            LigneFacture ligneFacture = new LigneFacture();
            ligneFacture.setFacture(facture);
            ligneFacture.setProduit(ligneVente.getProduit());
            ligneFacture.setDesignation(ligneVente.getDesignation());
            ligneFacture.setReference(ligneVente.getReference());
            ligneFacture.setQuantite(ligneVente.getQuantite());
            ligneFacture.setSurfaceM2(ligneVente.getSurfaceM2());
            ligneFacture.setPrixUnitaireHT(ligneVente.getPrixUnitaireHT());
            ligneFacture.setTauxTVA(ligneVente.getTauxTVA());
            ligneFacture.setRemisePourcentage(ligneVente.getRemisePourcentage());
            ligneFacture.calculerMontants();

            facture.addLigne(ligneFacture);
        }

        facture.setRemiseGlobale(vente.getRemiseGlobale());
        facture.calculerMontants();

        return factureRepository.save(facture);
    }

    public Facture getFactureById(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
    }

    public List<Facture> getAllFactures() {
        return factureRepository.findAll();
    }

    public List<Facture> getFacturesByClient(Long clientId) {
        return factureRepository.findByClientId((clientId));
    }

    public List<Facture> getFacturesByPeriode(LocalDate dateDebut, LocalDate dateFin) {
        return factureRepository.findFacturesByPeriode(dateDebut, dateFin);
    }

    public List<Facture> getFacturesImpayees() {
        return factureRepository.findFacturesImpayees();
    }

    public List<Facture> getFacturesEchues() {
        return factureRepository.findFacturesEchues(LocalDate.now());
    }

    public Facture annulerFacture(Long factureId, String motif, Long userId) {
        Facture facture = getFactureById(factureId);

        if (facture.getAnnulee()) {
            throw new RuntimeException("Cette facture est déjà annulée");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        facture.setAnnulee(true);
        facture.setDateAnnulation(LocalDateTime.now());
        facture.setMotifAnnulation(motif);
        facture.setAnnuleePar(user);
        facture.setStatut(StatutFacture.ANNULEE);

        return factureRepository.save(facture);
    }

    public Facture validerFacture(Long factureId) {
        Facture facture = getFactureById(factureId);

        if (facture.getLignes().isEmpty()) {
            throw new RuntimeException("Impossible de valider une facture sans lignes");
        }

        facture.setStatut(StatutFacture.VALIDEE);
        return factureRepository.save(facture);
    }

    private String genererNumeroFacture() {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        String annee = String.valueOf(LocalDate.now().getYear());
        long count = factureRepository.count() + 1;
        return "FACT-" + annee + "-" + String.format("%06d", count);
    }
}

