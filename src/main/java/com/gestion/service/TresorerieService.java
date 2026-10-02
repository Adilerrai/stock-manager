package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.BalanceAgeeDTO;
import com.gestion.persistent.dto.EcheancierDTO;
import com.gestion.persistent.dto.ReleveClientDTO;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.enums.TypeEffet;
import com.gestion.persistent.enums.StatutAvoir;
import com.gestion.persistent.enums.StatutFacture;
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
import java.util.stream.Collectors;

import com.gestion.persistent.dto.ReleveFournisseurDTO;

@Service
@Transactional
public class TresorerieService {

    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final FactureRepository factureRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final PaiementRepository paiementRepository;
    private final ReglementFournisseurRepository reglementFournisseurRepository;
    private final AvoirRepository avoirRepository;
    private final BordereauRemiseRepository bordereauRemiseRepository;
    private final ChequeEffetRepository chequeEffetRepository;

    public TresorerieService(ClientRepository clientRepository,
                             FournisseurRepository fournisseurRepository,
                             FactureRepository factureRepository,
                             FactureAchatRepository factureAchatRepository,
                             PaiementRepository paiementRepository,
                             ReglementFournisseurRepository reglementFournisseurRepository,
                             AvoirRepository avoirRepository,
                             BordereauRemiseRepository bordereauRemiseRepository,
                             ChequeEffetRepository chequeEffetRepository) {
        this.clientRepository = clientRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.paiementRepository = paiementRepository;
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.avoirRepository = avoirRepository;
        this.bordereauRemiseRepository = bordereauRemiseRepository;
        this.chequeEffetRepository = chequeEffetRepository;
    }

    public ReleveFournisseurDTO genererReleveFournisseur(Long fournisseurId, LocalDate dateDebut, LocalDate dateFin) {
        Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé: " + fournisseurId));

        ReleveFournisseurDTO releve = new ReleveFournisseurDTO();
        releve.setFournisseurId(fournisseur.getId());
        releve.setFournisseurNom(fournisseur.getRaisonSociale() != null ? fournisseur.getRaisonSociale() : fournisseur.getNom());
        releve.setTelephone(fournisseur.getTelephone());
        releve.setEmail(fournisseur.getEmail());
        releve.setIce(fournisseur.getIce());
        releve.setNumeroRegistreCommerce(fournisseur.getNumeroRegistreCommerce());
        releve.setNumeroIdentificationFiscale(fournisseur.getNumeroIdentificationFiscale());
        releve.setPatente(fournisseur.getPatente());
        releve.setRibBancaire(fournisseur.getRibBancaire());
        releve.setBanqueNom(fournisseur.getBanqueNom());
        releve.setDelaiPaiementJours(fournisseur.getDelaiPaiementJours());

        List<FactureAchat> factures = factureAchatRepository.findByFournisseurId(fournisseurId);
        Long tenantId = (fournisseur.getPointDeVenteId() != null) ? fournisseur.getPointDeVenteId() : 1L;
        List<ReglementFournisseur> reglements = reglementFournisseurRepository.findByFournisseurIdAndPointDeVenteId(fournisseurId, tenantId);

        List<ReleveFournisseurDTO.LigneReleveDTO> lignes = new ArrayList<>();

        for (FactureAchat f : factures) {
            if (f.getStatut() != StatutFacture.ANNULEE) {
                LocalDate d = f.getDateFacture() != null ? f.getDateFacture().toLocalDate() : LocalDate.now();
                if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                    BigDecimal montant = f.getMontantTtc() != null ? f.getMontantTtc() : BigDecimal.ZERO;
                    lignes.add(new ReleveFournisseurDTO.LigneReleveDTO(
                            d,
                            "FACTURE_ACHAT",
                            f.getNumeroFacture(),
                            "Facture d'achat N° " + f.getNumeroFacture(),
                            BigDecimal.ZERO,
                            montant,
                            BigDecimal.ZERO
                    ));
                }
            }
        }

        for (ReglementFournisseur r : reglements) {
            LocalDate d = r.getDateReglement() != null ? r.getDateReglement().toLocalDate() : LocalDate.now();
            if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                String lib = "Règlement " + r.getModePaiement();
                if (r.getNumeroCheque() != null) lib += " Chq N° " + r.getNumeroCheque();
                lignes.add(new ReleveFournisseurDTO.LigneReleveDTO(
                        d,
                        "REGLEMENT",
                        r.getNumeroReglement(),
                        lib,
                        r.getMontant(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
            }
        }

        // Trier par date chronologique
        lignes.sort(Comparator.comparing(ReleveFournisseurDTO.LigneReleveDTO::getDate));

        BigDecimal totalAchats = BigDecimal.ZERO;
        BigDecimal totalReglements = BigDecimal.ZERO;
        BigDecimal soldeProg = BigDecimal.ZERO;

        for (ReleveFournisseurDTO.LigneReleveDTO l : lignes) {
            totalAchats = totalAchats.add(l.getCredit());
            totalReglements = totalReglements.add(l.getDebit());
            soldeProg = soldeProg.add(l.getCredit()).subtract(l.getDebit());
            l.setSoldeProgressif(soldeProg);
        }

        releve.setTotalAchats(totalAchats);
        releve.setTotalReglements(totalReglements);
        releve.setSoldeActuel(soldeProg);
        releve.setOperations(lignes);

        return releve;
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
        Long tenantId = TenantContext.getCurrentTenant();
        List<Client> clients = tenantId != null ? clientRepository.findByPointDeVenteId(tenantId) : Collections.emptyList();
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
        Long tenantId = TenantContext.getCurrentTenant();
        List<Fournisseur> fournisseurs = tenantId != null ? fournisseurRepository.findByPointDeVenteId(tenantId) : Collections.emptyList();
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

        Long tenantId = TenantContext.getCurrentTenant();
        Long pointDeVenteId = tenantId != null ? tenantId : 1L;

        // 1. Factures clients impayées
        List<Facture> facturesImpayees = factureRepository.findFacturesImpayeesByPointDeVenteId(pointDeVenteId);
        for (Facture f : facturesImpayees) {
            LocalDate d = f.getDateEcheance() != null ? f.getDateEcheance() : f.getDateFacture();
            if (d != null && (dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                list.add(new EcheancierDTO.LigneEcheanceDTO(
                        d,
                        "ENCAISSEMENT",
                        f.getClient() != null ? (f.getClient().getNomComplet() != null ? f.getClient().getNomComplet() : f.getClient().getNom()) : "Client",
                        "FACTURE_VENTE",
                        f.getNumeroFacture(),
                        f.getMontantRestant(),
                        f.getStatut() != null ? f.getStatut().name() : "EN_ATTENTE",
                        f.getId(),
                        null
                ));
            }
        }

        // 2. Chèques & Traites en portefeuille ou remis à l'encaissement (Effets de commerce)
        List<ChequeEffet> chequesEffets = chequeEffetRepository.findByPointDeVenteIdOrderByDateEcheanceAsc(pointDeVenteId);
        for (ChequeEffet ce : chequesEffets) {
            if (ce.getStatut() == StatutEffet.EN_PORTEFEUILLE || ce.getStatut() == StatutEffet.REMIS_A_L_ENCAISSEMENT) {
                LocalDate d = ce.getDateEcheance() != null ? ce.getDateEcheance() : ce.getDateEmission();
                if (d != null && (dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                    String sens = ce.getSens() == SensEffet.ENCAISSEMENT_CLIENT ? "ENCAISSEMENT" : "DECAISSEMENT";
                    String tiers;
                    if (ce.getSens() == SensEffet.ENCAISSEMENT_CLIENT) {
                        tiers = ce.getClient() != null ? ce.getClient().getNom() : (ce.getTireur() != null ? ce.getTireur() : "Client");
                    } else {
                        tiers = ce.getFournisseur() != null ? ce.getFournisseur().getNom() : (ce.getBeneficiaire() != null ? ce.getBeneficiaire() : "Fournisseur");
                    }
                    String typeDoc = ce.getTypeEffet() == TypeEffet.TRAITE ? "TRAITE" : "CHEQUE";
                    list.add(new EcheancierDTO.LigneEcheanceDTO(
                            d,
                            sens,
                            tiers,
                            typeDoc,
                            ce.getNumeroPiece() != null ? ce.getNumeroPiece() : ("EFF-" + ce.getId()),
                            ce.getMontant(),
                            ce.getStatut() != null ? ce.getStatut().name() : "EN_PORTEFEUILLE",
                            ce.getId(),
                            ce.getBanqueEmettrice()
                    ));
                }
            }
        }

        // 3. Factures achats fournisseurs impayées
        List<FactureAchat> facturesAchats = factureAchatRepository.findByPointDeVenteId(pointDeVenteId);
        List<ReglementFournisseur> allReglements = reglementFournisseurRepository.findByPointDeVenteId(pointDeVenteId);
        Map<Long, BigDecimal> mapPaye = allReglements.stream()
                .filter(r -> r.getFactureAchat() != null && r.getFactureAchat().getId() != null && r.getMontant() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getFactureAchat().getId(),
                        Collectors.reducing(BigDecimal.ZERO, ReglementFournisseur::getMontant, BigDecimal::add)
                ));

        for (FactureAchat fa : facturesAchats) {
            if (fa.getStatut() == StatutFacture.PAYEE_TOTALEMENT || fa.getStatut() == StatutFacture.ANNULEE) {
                continue;
            }
            BigDecimal totalPaye = mapPaye.getOrDefault(fa.getId(), BigDecimal.ZERO);
            BigDecimal restant = fa.getMontantTtc() != null ? fa.getMontantTtc().subtract(totalPaye) : BigDecimal.ZERO;
            if (restant.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate d = fa.getDateEcheance() != null
                    ? fa.getDateEcheance().toLocalDate()
                    : (fa.getDateFacture() != null ? fa.getDateFacture().toLocalDate().plusDays(30) : LocalDate.now());

            if ((dateDebut == null || !d.isBefore(dateDebut)) && (dateFin == null || !d.isAfter(dateFin))) {
                list.add(new EcheancierDTO.LigneEcheanceDTO(
                        d,
                        "DECAISSEMENT",
                        fa.getFournisseur() != null ? fa.getFournisseur().getNom() : "Fournisseur",
                        "FACTURE_ACHAT",
                        fa.getNumeroFacture(),
                        restant,
                        fa.getStatut() != null ? fa.getStatut().name() : "EN_ATTENTE",
                        fa.getId(),
                        null
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
        return Collections.emptyList();
    }

    private String genererNumeroBordereau() {
        String prefixe = "REM-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = bordereauRemiseRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}
