package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.model.Facture;
import com.gestion.persistent.model.Paiement;
import com.gestion.persistent.model.Vente;
import com.gestion.repository.PaiementRepository;
import com.gestion.repository.FactureRepository;
import com.gestion.persistent.dto.PaiementSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.gestion.persistent.dto.AffectationItemDTO;
import com.gestion.persistent.dto.FactureImpayeeDTO;
import com.gestion.persistent.dto.ReglementClientRequest;
import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.enums.TypeEffet;
import com.gestion.persistent.model.*;
import com.gestion.repository.ChequeEffetRepository;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.PaiementAffectationRepository;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final VenteService venteService;
    private final FactureRepository factureRepository;
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final ComptabiliteService comptabiliteService;
    private final PaiementAffectationRepository affectationRepository;
    private final ChequeEffetRepository chequeEffetRepository;
    private final ClientRepository clientRepository;

    public PaiementService(PaiementRepository paiementRepository,
                          @Lazy VenteService venteService,
                          FactureRepository factureRepository,
                          ClientService clientService,
                          UserRepository userRepository,
                          @Lazy ComptabiliteService comptabiliteService,
                          PaiementAffectationRepository affectationRepository,
                          ChequeEffetRepository chequeEffetRepository,
                          ClientRepository clientRepository) {
        this.paiementRepository = paiementRepository;
        this.venteService = venteService;
        this.factureRepository = factureRepository;
        this.clientService = clientService;
        this.userRepository = userRepository;
        this.comptabiliteService = comptabiliteService;
        this.affectationRepository = affectationRepository;
        this.chequeEffetRepository = chequeEffetRepository;
        this.clientRepository = clientRepository;
    }

    public Page<Paiement> searchPaiements(PaiementSearchCriteria criteria, Pageable pageable) {
        return paiementRepository.findByCriteria(criteria, pageable);
    }

    public Paiement enregistrerPaiementVente(Long venteId, Paiement paiement, Long userId) {
        Vente vente = venteService.getVenteEntityById(venteId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (paiement.getMontant().compareTo(vente.getMontantRestant()) > 0) {
            throw new RuntimeException("Le montant du paiement dépasse le montant restant");
        }

        paiement.setVente(vente);
        paiement.setClient(vente.getClient());
        paiement.setEncaissePar(user);
        paiement.setNumeroPaiement(genererNumeroPaiement());
        paiement.setDatePaiement(LocalDateTime.now());

        paiement = paiementRepository.save(paiement);

        // Mettre à jour le montant payé de la vente
        vente.setMontantPaye(vente.getMontantPaye().add(paiement.getMontant()));
        vente.setMontantRestant(vente.getMontantFinal().subtract(vente.getMontantPaye()));

        // Si paiement à crédit, vérifier le plafond et augmenter le crédit utilisé
        if (paiement.getModePaiement() == ModePaiement.CREDIT && vente.getClient() != null) {
            if (!vente.getClient().peutAcheterACredit(paiement.getMontant())) {
                throw new IllegalStateException(String.format("Dépassement du crédit autorisé pour le client %s. Crédit disponible: %s DA, Montant demandé: %s DA",
                        vente.getClient().getNomComplet() != null ? vente.getClient().getNomComplet() : vente.getClient().getNom(),
                        vente.getClient().getCreditDisponible(),
                        paiement.getMontant()));
            }
            clientService.augmenterCreditUtilise(vente.getClient().getId(), paiement.getMontant());
        }

        try {
            comptabiliteService.genererEcriturePaiementClient(paiement);
        } catch (Exception e) {
            // Ne pas bloquer l'encaissement en cas d'erreur de journal
        }

        return paiement;
    }

    public Paiement enregistrerPaiementFacture(Long factureId, Paiement paiement, Long userId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (paiement.getMontant().compareTo(facture.getMontantRestant()) > 0) {
            throw new RuntimeException("Le montant du paiement dépasse le montant restant");
        }

        paiement.setFacture(facture);
        paiement.setClient(facture.getClient());
        paiement.setEncaissePar(user);
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

        try {
            comptabiliteService.genererEcriturePaiementClient(paiement);
        } catch (Exception e) {
            // Ne pas bloquer l'encaissement en cas d'erreur de journal
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
        return paiementRepository.findPaiementsByPeriode( dateDebut, dateFin);
    }

    public BigDecimal getTotalPaiementsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        BigDecimal total = paiementRepository.sumMontantByPeriode( dateDebut, dateFin);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalPaiementsByModePaiement(ModePaiement modePaiement, LocalDateTime dateDebut, LocalDateTime dateFin) {
        BigDecimal total = paiementRepository.sumMontantByModePaiement(modePaiement, dateDebut, dateFin);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<FactureImpayeeDTO> getFacturesImpayeesClient(Long clientId) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;
        List<Facture> factures = factureRepository.findFacturesImpayeesByClientIdAndPointDeVenteId(clientId, tenantId);
        return factures.stream().map(f -> new FactureImpayeeDTO(
                f.getId(),
                f.getNumeroFacture(),
                f.getDateFacture(),
                f.getDateEcheance(),
                f.getMontantFinal(),
                f.getMontantPaye(),
                f.getMontantRestant(),
                f.getStatut()
        )).collect(Collectors.toList());
    }

    public Paiement enregistrerReglementClient(ReglementClientRequest req, Long userId) {
        if (req.getClientId() == null) {
            throw new IllegalArgumentException("Le client est obligatoire");
        }
        if (req.getMontant() == null || req.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du règlement doit être supérieur à zéro");
        }

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;

        Client client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id : " + req.getClientId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + userId));

        // 1. Création de l'enregistrement Paiement principal
        Paiement paiement = new Paiement();
        paiement.setNumeroPaiement(genererNumeroPaiement());
        paiement.setDatePaiement(req.getDatePaiement() != null ? req.getDatePaiement() : LocalDateTime.now());
        paiement.setClient(client);
        paiement.setMontant(req.getMontant());
        paiement.setModePaiement(req.getModePaiement() != null ? req.getModePaiement() : ModePaiement.ESPECES);
        paiement.setNomBanque(req.getNomBanque());
        paiement.setNumeroCheque(req.getNumeroCheque());
        paiement.setDateEcheance(req.getDateEcheance() != null ? req.getDateEcheance().atStartOfDay() : null);
        paiement.setEncaissePar(user);
        paiement.setNotes(req.getNotes());
        paiement.setPointDeVenteId(tenantId);
        paiement = paiementRepository.save(paiement);

        BigDecimal montantDisponible = req.getMontant();
        BigDecimal totalAffecte = BigDecimal.ZERO;

        // 2. Gestion de l'affectation selon le type
        String type = req.getTypeAffectation() != null ? req.getTypeAffectation().toUpperCase() : "GLOBAL_FIFO";

        if ("GLOBAL_FIFO".equals(type)) {
            // Solder en priorité les factures les plus anciennes
            List<Facture> facturesImpayees = factureRepository.findFacturesImpayeesByClientIdAndPointDeVenteId(client.getId(), tenantId);
            for (Facture f : facturesImpayees) {
                if (montantDisponible.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal aPayer = montantDisponible.min(f.getMontantRestant());
                f.setMontantPaye(f.getMontantPaye().add(aPayer));
                f.setMontantRestant(f.getMontantFinal().subtract(f.getMontantPaye()));
                if (f.getMontantRestant().compareTo(BigDecimal.ZERO) <= 0) {
                    f.setStatut(StatutFacture.PAYEE_TOTALEMENT);
                } else {
                    f.setStatut(StatutFacture.PAYEE_PARTIELLEMENT);
                }
                factureRepository.save(f);

                PaiementAffectation affectation = new PaiementAffectation(paiement, f, aPayer);
                affectationRepository.save(affectation);

                montantDisponible = montantDisponible.subtract(aPayer);
                totalAffecte = totalAffecte.add(aPayer);
            }
        } else if ("MANUELLE".equals(type) && req.getAffectations() != null && !req.getAffectations().isEmpty()) {
            for (AffectationItemDTO item : req.getAffectations()) {
                if (item.getMontant() == null || item.getMontant().compareTo(BigDecimal.ZERO) <= 0) continue;
                Facture f = factureRepository.findById(item.getFactureId())
                        .orElseThrow(() -> new RuntimeException("Facture introuvable avec l'id : " + item.getFactureId()));

                BigDecimal aPayer = item.getMontant().min(f.getMontantRestant());
                f.setMontantPaye(f.getMontantPaye().add(aPayer));
                f.setMontantRestant(f.getMontantFinal().subtract(f.getMontantPaye()));
                if (f.getMontantRestant().compareTo(BigDecimal.ZERO) <= 0) {
                    f.setStatut(StatutFacture.PAYEE_TOTALEMENT);
                } else {
                    f.setStatut(StatutFacture.PAYEE_PARTIELLEMENT);
                }
                factureRepository.save(f);

                PaiementAffectation affectation = new PaiementAffectation(paiement, f, aPayer);
                affectationRepository.save(affectation);

                totalAffecte = totalAffecte.add(aPayer);
            }
        }

        // 3. Mise à jour de la dette du client (crédit utilisé)
        BigDecimal montantReductionDette = totalAffecte.compareTo(BigDecimal.ZERO) > 0 ? totalAffecte : req.getMontant();
        clientService.diminuerCreditUtilise(client.getId(), montantReductionDette);

        // 4. Si Mode de paiement = CHEQUE, créer l'effet dans le portefeuille Trésorerie
        if (req.getModePaiement() == ModePaiement.CHEQUE) {
            ChequeEffet cheque = new ChequeEffet();
            cheque.setNumeroPiece(req.getNumeroCheque() != null && !req.getNumeroCheque().trim().isEmpty()
                    ? req.getNumeroCheque().trim() : paiement.getNumeroPaiement());
            cheque.setTypeEffet(TypeEffet.CHEQUE);
            cheque.setSens(SensEffet.ENCAISSEMENT_CLIENT);
            cheque.setStatut(req.getStatutCheque() != null ? req.getStatutCheque() : StatutEffet.EN_PORTEFEUILLE);
            cheque.setMontant(req.getMontant());
            cheque.setDateEmission(req.getDatePaiement() != null ? req.getDatePaiement().toLocalDate() : LocalDate.now());
            cheque.setDateEcheance(req.getDateEcheance() != null ? req.getDateEcheance() : cheque.getDateEmission());
            cheque.setBanqueEmettrice(req.getNomBanque());
            cheque.setTireur(client.getNomComplet() != null ? client.getNomComplet() : client.getNom());
            cheque.setBeneficiaire("Entreprise");
            cheque.setReferencePaiement(paiement.getNumeroPaiement());
            cheque.setClient(client);
            cheque.setNotes(req.getNotes());
            cheque.setPointDeVenteId(tenantId);
            cheque.setDateCreation(LocalDateTime.now());
            chequeEffetRepository.save(cheque);
        }

        // 5. Génération de l'écriture comptable
        try {
            comptabiliteService.genererEcriturePaiementClient(paiement);
        } catch (Exception e) {
            // Ignorer si la compta n'est pas encore activée
        }

        return paiement;
    }

    private String genererNumeroPaiement() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = paiementRepository.count() + 1;
        return "PAY-"+ dateStr + "-" + String.format("%06d", count);
    }
}


