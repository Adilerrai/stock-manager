package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.BalanceAgeeDTO;
import com.gestion.persistent.dto.EcheancierDTO;
import com.gestion.persistent.dto.ReleveClientDTO;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.StatutAvoir;
import com.gestion.persistent.enums.StatutRemise;
import com.gestion.persistent.enums.TypeAvoir;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class TresorerieService {

    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final FactureRepository factureRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final PaiementRepository paiementRepository;
    private final AvoirRepository avoirRepository;
    private final BordereauRemiseRepository bordereauRemiseRepository;

    public TresorerieService(ClientRepository clientRepository,
                             FournisseurRepository fournisseurRepository,
                             FactureRepository factureRepository,
                             FactureAchatRepository factureAchatRepository,
                             PaiementRepository paiementRepository,
                             AvoirRepository avoirRepository,
                             BordereauRemiseRepository bordereauRemiseRepository) {
        this.clientRepository = clientRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.paiementRepository = paiementRepository;
        this.avoirRepository = avoirRepository;
        this.bordereauRemiseRepository = bordereauRemiseRepository;
    }

    public ReleveClientDTO genererReleveClient(Long clientId, LocalDate dateDebut, LocalDate dateFin) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé: " + clientId));

        ReleveClientDTO releve = new ReleveClientDTO();
        releve.setClientId(client.getId());
        releve.setClientNom(client.getNomComplet() != null ? client.getNomComplet() : client.getNom());
        releve.setTelephone(client.getTelephone());
        releve.setEmail(client.getEmail());
        releve.setIce(client.getNumeroIdentificationFiscale());
        releve.setCreditAutorise(client.getCreditAutorise() != null ? client.getCreditAutorise() : BigDecimal.ZERO);

        List<Facture> factures = factureRepository.findByClientId(clientId);
        List<Paiement> paiements = paiementRepository.findByClientId(clientId);
        List<Avoir> avoirs = avoirRepository.findByClientIdOrderByDateAvoirDesc(clientId);

        List<ReleveClientDTO.LigneReleveDTO> lignes = new ArrayList<>();

        for (Facture f : factures) {
            if (!Boolean.TRUE.equals(f.getAnnulee())) {
                LocalDate d = f.getDateFacture() != null ? f.getDateFacture() : LocalDate.now();
                if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                    lignes.add(new ReleveClientDTO.LigneReleveDTO(
                            d,
                            "FACTURE",
                            f.getNumeroFacture(),
                            "Facture de vente N° " + f.getNumeroFacture(),
                            f.getMontantFinal(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    ));
                }
            }
        }

        for (Paiement p : paiements) {
            LocalDate d = p.getDatePaiement() != null ? p.getDatePaiement().toLocalDate() : LocalDate.now();
            if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                String lib = "Règlement " + p.getModePaiement();
                if (p.getNumeroCheque() != null) lib += " Chq N° " + p.getNumeroCheque();
                lignes.add(new ReleveClientDTO.LigneReleveDTO(
                        d,
                        "PAIEMENT",
                        p.getNumeroPaiement(),
                        lib,
                        BigDecimal.ZERO,
                        p.getMontant(),
                        BigDecimal.ZERO
                ));
            }
        }

        for (Avoir a : avoirs) {
            if (a.getStatut() != StatutAvoir.ANNULE) {
                LocalDate d = a.getDateAvoir() != null ? a.getDateAvoir() : LocalDate.now();
                if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                    lignes.add(new ReleveClientDTO.LigneReleveDTO(
                            d,
                            "AVOIR",
                            a.getNumeroAvoir(),
                            "Avoir N° " + a.getNumeroAvoir() + (a.getMotif() != null ? " - " + a.getMotif() : ""),
                            BigDecimal.ZERO,
                            a.getMontantTTC(),
                            BigDecimal.ZERO
                    ));
                }
            }
        }

        // Trier par date chronologique
        lignes.sort(Comparator.comparing(ReleveClientDTO.LigneReleveDTO::getDate));

        BigDecimal totalFac = BigDecimal.ZERO;
        BigDecimal totalPai = BigDecimal.ZERO;
        BigDecimal totalAvr = BigDecimal.ZERO;
        BigDecimal soldeProg = BigDecimal.ZERO;

        for (ReleveClientDTO.LigneReleveDTO l : lignes) {
            totalFac = totalFac.add(l.getDebit());
            totalPai = totalPai.add(l.getCredit());
            if ("AVOIR".equals(l.getTypeOperation())) {
                totalAvr = totalAvr.add(l.getCredit());
            }
            soldeProg = soldeProg.add(l.getDebit()).subtract(l.getCredit());
            l.setSoldeProgressif(soldeProg);
        }

        releve.setTotalFactures(totalFac);
        releve.setTotalPaiements(totalPai);
        releve.setTotalAvoirs(totalAvr);
        releve.setSoldeActuel(soldeProg);
        releve.setOperations(lignes);

        return releve;
    }

    public BalanceAgeeDTO calculerBalanceAgeeClients() {
        BalanceAgeeDTO balance = new BalanceAgeeDTO();
        List<Client> clients = clientRepository.findAll();
        LocalDate today = LocalDate.now();

        Map<Long, BalanceAgeeDTO.LigneBalanceAgeeDTO> mapTiers = new HashMap<>();

        for (Client c : clients) {
            List<Facture> factures = factureRepository.findByClientId(c.getId());
            for (Facture f : factures) {
                if (!Boolean.TRUE.equals(f.getAnnulee()) && f.getMontantRestant() != null && f.getMontantRestant().compareTo(BigDecimal.ZERO) > 0) {
                    BalanceAgeeDTO.LigneBalanceAgeeDTO ligne = mapTiers.computeIfAbsent(c.getId(), k -> {
                        BalanceAgeeDTO.LigneBalanceAgeeDTO l = new BalanceAgeeDTO.LigneBalanceAgeeDTO();
                        l.setTiersId(c.getId());
                        l.setTiersNom(c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
                        l.setTelephone(c.getTelephone());
                        return l;
                    });

                    BigDecimal reste = f.getMontantRestant();
                    ligne.setTotalDu(ligne.getTotalDu().add(reste));
                    balance.setTotalCreances(balance.getTotalCreances().add(reste));

                    LocalDate echeance = f.getDateEcheance() != null ? f.getDateEcheance() : f.getDateFacture();
                    if (echeance == null || !echeance.isBefore(today)) {
                        ligne.setNonEchu(ligne.getNonEchu().add(reste));
                        balance.setTotalNonEchu(balance.getTotalNonEchu().add(reste));
                    } else {
                        long jours = ChronoUnit.DAYS.between(echeance, today);
                        if (jours <= 30) {
                            ligne.setMoins30J(ligne.getMoins30J().add(reste));
                            balance.setTotalMoins30J(balance.getTotalMoins30J().add(reste));
                        } else if (jours <= 60) {
                            ligne.setDe30A60J(ligne.getDe30A60J().add(reste));
                            balance.setTotal30A60J(balance.getTotal30A60J().add(reste));
                        } else if (jours <= 90) {
                            ligne.setDe60A90J(ligne.getDe60A90J().add(reste));
                            balance.setTotal60A90J(balance.getTotal60A90J().add(reste));
                        } else {
                            ligne.setPlus90J(ligne.getPlus90J().add(reste));
                            balance.setTotalPlus90J(balance.getTotalPlus90J().add(reste));
                        }
                    }
                }
            }
        }

        balance.setTiers(new ArrayList<>(mapTiers.values()));
        return balance;
    }

    public BalanceAgeeDTO calculerBalanceAgeeFournisseurs() {
        BalanceAgeeDTO balance = new BalanceAgeeDTO();
        List<Fournisseur> fournisseurs = fournisseurRepository.findAll();
        LocalDate today = LocalDate.now();

        Map<Long, BalanceAgeeDTO.LigneBalanceAgeeDTO> mapTiers = new HashMap<>();

        for (Fournisseur frs : fournisseurs) {
            List<FactureAchat> factures = factureAchatRepository.findByFournisseurId(frs.getId());
            for (FactureAchat f : factures) {
                BigDecimal montant = f.getMontantTtc() != null ? f.getMontantTtc() : BigDecimal.ZERO;
                if (montant.compareTo(BigDecimal.ZERO) > 0) {
                    BalanceAgeeDTO.LigneBalanceAgeeDTO ligne = mapTiers.computeIfAbsent(frs.getId(), k -> {
                        BalanceAgeeDTO.LigneBalanceAgeeDTO l = new BalanceAgeeDTO.LigneBalanceAgeeDTO();
                        l.setTiersId(frs.getId());
                        l.setTiersNom(frs.getRaisonSociale());
                        l.setTelephone(frs.getTelephone());
                        return l;
                    });

                    ligne.setTotalDu(ligne.getTotalDu().add(montant));
                    balance.setTotalCreances(balance.getTotalCreances().add(montant));

                    LocalDate echeance = f.getDateFacture() != null ? f.getDateFacture().toLocalDate().plusDays(30) : today;
                    if (!echeance.isBefore(today)) {
                        ligne.setNonEchu(ligne.getNonEchu().add(montant));
                        balance.setTotalNonEchu(balance.getTotalNonEchu().add(montant));
                    } else {
                        long jours = ChronoUnit.DAYS.between(echeance, today);
                        if (jours <= 30) {
                            ligne.setMoins30J(ligne.getMoins30J().add(montant));
                            balance.setTotalMoins30J(balance.getTotalMoins30J().add(montant));
                        } else if (jours <= 60) {
                            ligne.setDe30A60J(ligne.getDe30A60J().add(montant));
                            balance.setTotal30A60J(balance.getTotal30A60J().add(montant));
                        } else if (jours <= 90) {
                            ligne.setDe60A90J(ligne.getDe60A90J().add(montant));
                            balance.setTotal60A90J(balance.getTotal60A90J().add(montant));
                        } else {
                            ligne.setPlus90J(ligne.getPlus90J().add(montant));
                            balance.setTotalPlus90J(balance.getTotalPlus90J().add(montant));
                        }
                    }
                }
            }
        }

        balance.setTiers(new ArrayList<>(mapTiers.values()));
        return balance;
    }

    public EcheancierDTO genererEcheancier(LocalDate dateDebut, LocalDate dateFin) {
        EcheancierDTO ech = new EcheancierDTO();
        List<EcheancierDTO.LigneEcheanceDTO> list = new ArrayList<>();

        // Factures clients impayées
        List<Facture> facturesImpayees = factureRepository.findFacturesImpayees();
        for (Facture f : facturesImpayees) {
            LocalDate d = f.getDateEcheance() != null ? f.getDateEcheance() : f.getDateFacture();
            if (d != null && (dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                list.add(new EcheancierDTO.LigneEcheanceDTO(
                        d,
                        "ENCAISSEMENT",
                        f.getClient() != null ? f.getClient().getNomComplet() : "Client",
                        "FACTURE_VENTE",
                        f.getNumeroFacture(),
                        f.getMontantRestant(),
                        f.getStatut() != null ? f.getStatut().name() : "EN_ATTENTE"
                ));
            }
        }

        // Chèques reçus en portefeuille non encore échus
        List<Paiement> paiements = paiementRepository.findAll();
        for (Paiement p : paiements) {
            if (p.getModePaiement() == ModePaiement.CHEQUE && p.getDateEcheance() != null) {
                LocalDate d = p.getDateEcheance().toLocalDate();
                if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                    list.add(new EcheancierDTO.LigneEcheanceDTO(
                            d,
                            "ENCAISSEMENT",
                            p.getClient() != null ? p.getClient().getNom() : (p.getNomBanque() != null ? p.getNomBanque() : "Banque"),
                            "CHEQUE_PORTEFEUILLE",
                            "Chq " + (p.getNumeroCheque() != null ? p.getNumeroCheque() : p.getNumeroPaiement()),
                            p.getMontant(),
                            "EN_PORTEFEUILLE"
                    ));
                }
            }
        }

        // Factures achats fournisseurs
        List<FactureAchat> facturesAchats = factureAchatRepository.findAll();
        for (FactureAchat fa : facturesAchats) {
            LocalDate d = fa.getDateFacture() != null ? fa.getDateFacture().toLocalDate().plusDays(30) : LocalDate.now();
            if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                list.add(new EcheancierDTO.LigneEcheanceDTO(
                        d,
                        "DECAISSEMENT",
                        fa.getFournisseur() != null ? fa.getFournisseur().getRaisonSociale() : "Fournisseur",
                        "FACTURE_ACHAT",
                        fa.getNumeroFacture(),
                        fa.getMontantTtc(),
                        "A_PAYER"
                ));
            }
        }

        list.sort(Comparator.comparing(EcheancierDTO.LigneEcheanceDTO::getDateEcheance));

        BigDecimal totalEnc = BigDecimal.ZERO;
        BigDecimal totalDec = BigDecimal.ZERO;

        for (EcheancierDTO.LigneEcheanceDTO item : list) {
            if ("ENCAISSEMENT".equals(item.getSens())) {
                totalEnc = totalEnc.add(item.getMontant());
            } else {
                totalDec = totalDec.add(item.getMontant());
            }
        }

        ech.setTotalAEncaisser(totalEnc);
        ech.setTotalAPayer(totalDec);
        ech.setSoldePrevisionnel(totalEnc.subtract(totalDec));
        ech.setEcheances(list);

        return ech;
    }

    public BordereauRemise creerBordereauRemise(BordereauRemise bordereau) {
        bordereau.setNumeroBordereau(genererNumeroBordereau());
        bordereau.setDateCreation(LocalDateTime.now());
        if (bordereau.getDateRemise() == null) {
            bordereau.setDateRemise(LocalDate.now());
        }
        if (bordereau.getStatut() == null) {
            bordereau.setStatut(StatutRemise.BROUILLON);
        }

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            bordereau.setPointDeVenteId(tenantId);
        }

        return bordereauRemiseRepository.save(bordereau);
    }

    public BordereauRemise changerStatutRemise(Long id, StatutRemise statut) {
        BordereauRemise b = bordereauRemiseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bordereau non trouvé: " + id));
        b.setStatut(statut);
        return bordereauRemiseRepository.save(b);
    }

    public List<BordereauRemise> getAllBordereaux() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return bordereauRemiseRepository.findByPointDeVenteIdOrderByDateRemiseDesc(tenantId);
        }
        return bordereauRemiseRepository.findAll();
    }

    private String genererNumeroBordereau() {
        String prefixe = "REM-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = bordereauRemiseRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}
