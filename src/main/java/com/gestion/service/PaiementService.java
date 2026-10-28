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

        return paiement;
    }

    public Paiement annulerPaiement(Long paiementId, String motif, Long userId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        if (Boolean.TRUE.equals(paiement.getAnnule())) {
            throw new RuntimeException("Ce paiement est déjà annulé");
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            user = userRepository.findAll().stream().findFirst().orElse(null);
        }

        paiement.setAnnule(true);
        paiement.setDateAnnulation(LocalDateTime.now());
        paiement.setMotifAnnulation(motif != null ? motif : "Annulation du règlement");
        paiement.setAnnulePar(user);

        // 1. Ajuster les montants de la vente (comptoir / POS)
        if (paiement.getVente() != null) {
            Vente vente = paiement.getVente();
            BigDecimal payeActuel = vente.getMontantPaye() != null ? vente.getMontantPaye() : BigDecimal.ZERO;
            BigDecimal montantAnnule = paiement.getMontant() != null ? paiement.getMontant() : BigDecimal.ZERO;
            BigDecimal nouveauPaye = payeActuel.subtract(montantAnnule);
            if (nouveauPaye.compareTo(BigDecimal.ZERO) < 0) {
                nouveauPaye = BigDecimal.ZERO;
            }
            vente.setMontantPaye(nouveauPaye);
            BigDecimal finalAmt = vente.getMontantFinal() != null ? vente.getMontantFinal() : BigDecimal.ZERO;
            vente.setMontantRestant(finalAmt.subtract(nouveauPaye).max(BigDecimal.ZERO));

            // Si la vente était enregistrée à crédit, réajuster le crédit utilisé
            if (paiement.getModePaiement() == ModePaiement.CREDIT && vente.getClient() != null) {
                clientService.diminuerCreditUtilise(vente.getClient().getId(), montantAnnule);
            }
        }

        // 2. Ajuster la facture directe si elle est rattachée au paiement
        if (paiement.getFacture() != null) {
            Facture facture = paiement.getFacture();
            BigDecimal payeActuel = facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO;
            BigDecimal montantAnnule = paiement.getMontant() != null ? paiement.getMontant() : BigDecimal.ZERO;
            BigDecimal nouveauPaye = payeActuel.subtract(montantAnnule);
            if (nouveauPaye.compareTo(BigDecimal.ZERO) < 0) {
                nouveauPaye = BigDecimal.ZERO;
            }
            facture.setMontantPaye(nouveauPaye);
            BigDecimal finalAmt = facture.getMontantFinal() != null ? facture.getMontantFinal() : BigDecimal.ZERO;
            facture.setMontantRestant(finalAmt.subtract(nouveauPaye).max(BigDecimal.ZERO));

            // Actualisation du statut de la facture (retour vers VALIDEE si montant restant total)
            if (nouveauPaye.compareTo(BigDecimal.ZERO) <= 0) {
                facture.setStatut(StatutFacture.VALIDEE);
            } else if (nouveauPaye.compareTo(finalAmt) >= 0) {
                facture.setStatut(StatutFacture.PAYEE_TOTALEMENT);
            } else {
                facture.setStatut(StatutFacture.PAYEE_PARTIELLEMENT);
            }
            factureRepository.save(facture);

            // Réaugmenter le crédit utilisé / solde dû du client
            Client client = facture.getClient() != null ? facture.getClient() : paiement.getClient();
            if (client != null) {
                clientService.augmenterCreditUtilise(client.getId(), montantAnnule);
            }
        }

        // 3. Ajuster les factures ventilées (règlements clients multi-factures via PaiementAffectation)
        List<PaiementAffectation> affectations = affectationRepository.findByPaiementId(paiement.getId());
        if (affectations != null && !affectations.isEmpty()) {
            BigDecimal totalAffecte = BigDecimal.ZERO;
            for (PaiementAffectation aff : affectations) {
                Facture f = aff.getFacture();
                if (f != null) {
                    BigDecimal montantAReouvrir = aff.getMontantAffecte() != null ? aff.getMontantAffecte() : BigDecimal.ZERO;
                    BigDecimal payeActuel = f.getMontantPaye() != null ? f.getMontantPaye() : BigDecimal.ZERO;
                    BigDecimal nouveauPaye = payeActuel.subtract(montantAReouvrir);
                    if (nouveauPaye.compareTo(BigDecimal.ZERO) < 0) {
                        nouveauPaye = BigDecimal.ZERO;
                    }
                    f.setMontantPaye(nouveauPaye);
                    BigDecimal finalAmt = f.getMontantFinal() != null ? f.getMontantFinal() : BigDecimal.ZERO;
                    f.setMontantRestant(finalAmt.subtract(nouveauPaye).max(BigDecimal.ZERO));

                    // Actualisation du statut de la facture
                    if (nouveauPaye.compareTo(BigDecimal.ZERO) <= 0) {
                        f.setStatut(StatutFacture.VALIDEE);
                    } else if (nouveauPaye.compareTo(finalAmt) >= 0) {
                        f.setStatut(StatutFacture.PAYEE_TOTALEMENT);
                    } else {
                        f.setStatut(StatutFacture.PAYEE_PARTIELLEMENT);
                    }
                    factureRepository.save(f);
                    totalAffecte = totalAffecte.add(montantAReouvrir);
                }
            }

            // Réaugmenter le crédit utilisé / solde dû du client
            Client client = paiement.getClient();
            if (client != null) {
                BigDecimal montantAReaugmenter = totalAffecte.compareTo(BigDecimal.ZERO) > 0 ? totalAffecte : (paiement.getMontant() != null ? paiement.getMontant() : BigDecimal.ZERO);
                clientService.augmenterCreditUtilise(client.getId(), montantAReaugmenter);
            }
        } else if (paiement.getFacture() == null && paiement.getVente() == null && paiement.getClient() != null) {
            // Règlement global ou acompte libre sans affectation
            if (paiement.getMontant() != null && paiement.getMontant().compareTo(BigDecimal.ZERO) > 0) {
                clientService.augmenterCreditUtilise(paiement.getClient().getId(), paiement.getMontant());
            }
        }

        // 4. Si un chèque ou effet a été généré pour ce paiement, l'annuler également
        if (paiement.getNumeroPaiement() != null) {
            List<ChequeEffet> cheques = chequeEffetRepository.findByReferencePaiement(paiement.getNumeroPaiement());
            if (cheques != null) {
                for (ChequeEffet c : cheques) {
                    if (c.getStatut() != StatutEffet.ANNULE && c.getStatut() != StatutEffet.IMPAYE_REJETE) {
                        c.setStatut(StatutEffet.ANNULE);
                        String currentNotes = c.getNotes() != null ? c.getNotes() : "";
                        c.setNotes(currentNotes.isEmpty() ? "Annulé suite à l'annulation du règlement " + paiement.getNumeroPaiement()
                                : currentNotes + " | Annulé suite à l'annulation du règlement " + paiement.getNumeroPaiement());
                        chequeEffetRepository.save(c);
                    }
                }
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
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return paiementRepository.findPaiementsByPeriodeAndTenant(dateDebut, dateFin, tenantId);
        }
        return paiementRepository.findPaiementsByPeriode(dateDebut, dateFin);
    }

    public BigDecimal getTotalPaiementsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        Long tenantId = TenantContext.getCurrentTenant();
        BigDecimal total = tenantId != null
                ? paiementRepository.sumMontantByPeriodeAndTenant(dateDebut, dateFin, tenantId)
                : paiementRepository.sumMontantByPeriode(dateDebut, dateFin);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalPaiementsByModePaiement(ModePaiement modePaiement, LocalDateTime dateDebut, LocalDateTime dateFin) {
        Long tenantId = TenantContext.getCurrentTenant();
        BigDecimal total = tenantId != null
                ? paiementRepository.sumMontantByModePaiementAndTenant(modePaiement, dateDebut, dateFin, tenantId)
                : paiementRepository.sumMontantByModePaiement(modePaiement, dateDebut, dateFin);
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

        // 4. Si Mode de paiement = CHEQUE, TRAITE, EFFET ou LCN, créer l'effet dans le portefeuille Trésorerie
        if (req.getModePaiement() == ModePaiement.CHEQUE || req.getModePaiement() == ModePaiement.TRAITE
                || req.getModePaiement() == ModePaiement.EFFET || req.getModePaiement() == ModePaiement.LCN) {
            ChequeEffet cheque = new ChequeEffet();
            cheque.setNumeroPiece(req.getNumeroCheque() != null && !req.getNumeroCheque().trim().isEmpty()
                    ? req.getNumeroCheque().trim() : paiement.getNumeroPaiement());
            
            TypeEffet typeEffet = TypeEffet.CHEQUE;
            if (req.getModePaiement() == ModePaiement.TRAITE) {
                typeEffet = TypeEffet.TRAITE;
            } else if (req.getModePaiement() == ModePaiement.LCN || req.getModePaiement() == ModePaiement.EFFET) {
                typeEffet = TypeEffet.LCN;
            }
            cheque.setTypeEffet(typeEffet);
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

        return paiement;
    }

    private String genererNumeroPaiement() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = paiementRepository.count() + 1;
        return "PAY-"+ dateStr + "-" + String.format("%06d", count);
    }
}


