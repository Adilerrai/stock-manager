package com.ceramique.service;

import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import com.ceramique.persistent.enums.ModePaiement;
import com.ceramique.persistent.model.Facture;
import com.ceramique.persistent.model.Paiement;
import com.ceramique.persistent.model.Vente;
import com.ceramique.repository.PaiementRepository;
import com.ceramique.repository.FactureRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final VenteService venteService;
    private final FactureRepository factureRepository;
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final PointDeVenteRepository pointDeVenteRepository;

    public PaiementService(PaiementRepository paiementRepository,
                          @Lazy VenteService venteService,
                          FactureRepository factureRepository,
                          ClientService clientService,
                          UserRepository userRepository,
                          PointDeVenteRepository pointDeVenteRepository) {
        this.paiementRepository = paiementRepository;
        this.venteService = venteService;
        this.factureRepository = factureRepository;
        this.clientService = clientService;
        this.userRepository = userRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    public Paiement enregistrerPaiementVente(Long venteId, Paiement paiement, Long userId) {
        Vente vente = venteService.getVenteById(venteId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findById(pointDeVenteId)
                .orElseThrow(() -> new RuntimeException("Point de vente non trouvé"));

        if (paiement.getMontant().compareTo(vente.getMontantRestant()) > 0) {
            throw new RuntimeException("Le montant du paiement dépasse le montant restant");
        }

        paiement.setVente(vente);
        paiement.setClient(vente.getClient());
        paiement.setEncaissePar(user);
        paiement.setPointDeVente(pointDeVente);
        paiement.setNumeroPaiement(genererNumeroPaiement());
        paiement.setDatePaiement(LocalDateTime.now());

        paiement = paiementRepository.save(paiement);

        // Mettre à jour le montant payé de la vente
        vente.setMontantPaye(vente.getMontantPaye().add(paiement.getMontant()));
        vente.setMontantRestant(vente.getMontantFinal().subtract(vente.getMontantPaye()));

        // Si paiement à crédit, augmenter le crédit utilisé
        if (paiement.getModePaiement() == ModePaiement.CREDIT && vente.getClient() != null) {
            clientService.augmenterCreditUtilise(vente.getClient().getId(), paiement.getMontant());
        }

        return paiement;
    }

    public Paiement enregistrerPaiementFacture(Long factureId, Paiement paiement, Long userId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findById(pointDeVenteId)
                .orElseThrow(() -> new RuntimeException("Point de vente non trouvé"));

        if (paiement.getMontant().compareTo(facture.getMontantRestant()) > 0) {
            throw new RuntimeException("Le montant du paiement dépasse le montant restant");
        }

        paiement.setFacture(facture);
        paiement.setClient(facture.getClient());
        paiement.setEncaissePar(user);
        paiement.setPointDeVente(pointDeVente);
        paiement.setNumeroPaiement(genererNumeroPaiement());
        paiement.setDatePaiement(LocalDateTime.now());

        paiement = paiementRepository.save(paiement);

        // Mettre à jour le montant payé de la facture
        facture.setMontantPaye(facture.getMontantPaye().add(paiement.getMontant()));
        facture.setMontantRestant(facture.getMontantFinal().subtract(facture.getMontantPaye()));

        // Diminuer le crédit utilisé du client
        if (facture.getClient() != null) {
            clientService.diminuerCreditUtilise(facture.getClient().getId(), paiement.getMontant());
        }

        return paiement;
    }

    public Paiement annulerPaiement(Long paiementId, String motif, Long userId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        if (paiement.getAnnule()) {
            throw new RuntimeException("Ce paiement est déjà annulé");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        paiement.setAnnule(true);
        paiement.setDateAnnulation(LocalDateTime.now());
        paiement.setMotifAnnulation(motif);
        paiement.setAnnulePar(user);

        // Ajuster les montants de la vente ou facture
        if (paiement.getVente() != null) {
            Vente vente = paiement.getVente();
            vente.setMontantPaye(vente.getMontantPaye().subtract(paiement.getMontant()));
            vente.setMontantRestant(vente.getMontantFinal().subtract(vente.getMontantPaye()));
        }

        if (paiement.getFacture() != null) {
            Facture facture = paiement.getFacture();
            facture.setMontantPaye(facture.getMontantPaye().subtract(paiement.getMontant()));
            facture.setMontantRestant(facture.getMontantFinal().subtract(facture.getMontantPaye()));

            // Réaugmenter le crédit utilisé du client
            if (facture.getClient() != null) {
                clientService.augmenterCreditUtilise(facture.getClient().getId(), paiement.getMontant());
            }
        }

        return paiementRepository.save(paiement);
    }

    public List<Paiement> getPaiementsByVente(Long venteId) {
        return paiementRepository.findByVenteId(venteId);
    }

    public List<Paiement> getPaiementsByFacture(Long factureId) {
        return paiementRepository.findByFactureId(factureId);
    }

    public List<Paiement> getPaiementsByClient(Long clientId) {
        return paiementRepository.findByClientId(clientId);
    }

    public List<Paiement> getPaiementsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        return paiementRepository.findPaiementsByPeriode(pointDeVenteId, dateDebut, dateFin);
    }

    public BigDecimal getTotalPaiementsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        BigDecimal total = paiementRepository.sumMontantByPeriode(pointDeVenteId, dateDebut, dateFin);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalPaiementsByModePaiement(ModePaiement modePaiement, LocalDateTime dateDebut, LocalDateTime dateFin) {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        BigDecimal total = paiementRepository.sumMontantByModePaiement(pointDeVenteId, modePaiement, dateDebut, dateFin);
        return total != null ? total : BigDecimal.ZERO;
    }

    private String genererNumeroPaiement() {
        Long pointDeVenteId = TenantContext.getCurrentTenant();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = paiementRepository.count() + 1;
        return "PAY-" + pointDeVenteId + "-" + dateStr + "-" + String.format("%06d", count);
    }
}

