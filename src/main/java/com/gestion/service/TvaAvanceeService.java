package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ControleTvaDTO;
import com.gestion.persistent.dto.DeclarationTvaEnregistreeDTO;
import com.gestion.persistent.dto.LigneReleveDeductionDTO;
import com.gestion.persistent.dto.ReleveDeductionTvaDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.enums.RegimeTva;
import com.gestion.persistent.enums.StatutDeclarationTva;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.enums.StatutRapprochement;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TvaAvanceeService {

    private final DeclarationTvaRepository declarationTvaRepository;
    private final LigneEcritureRepository ligneRepository;
    private final FactureRepository factureRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final PaiementRepository paiementRepository;
    private final ReglementFournisseurRepository reglementFournisseurRepository;
    private final SocieteRepository societeRepository;
    private final LigneReleveBancaireRepository ligneReleveRepository;
    private final AuditService auditService;

    public TvaAvanceeService(DeclarationTvaRepository declarationTvaRepository,
                             LigneEcritureRepository ligneRepository,
                             FactureRepository factureRepository,
                             FactureAchatRepository factureAchatRepository,
                             PaiementRepository paiementRepository,
                             ReglementFournisseurRepository reglementFournisseurRepository,
                             SocieteRepository societeRepository,
                             LigneReleveBancaireRepository ligneReleveRepository,
                             AuditService auditService) {
        this.declarationTvaRepository = declarationTvaRepository;
        this.ligneRepository = ligneRepository;
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.paiementRepository = paiementRepository;
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.societeRepository = societeRepository;
        this.ligneReleveRepository = ligneReleveRepository;
        this.auditService = auditService;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    private String getCurrentUser() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "Système";
    }

    // =========================================================================
    // GÉNÉRATION & PERSISTANCE DE LA DÉCLARATION TVA
    // =========================================================================

    public DeclarationTvaEnregistreeDTO genererDeclaration(String periode,
                                                          RegimeTva regime,
                                                          BigDecimal prorata,
                                                          BigDecimal creditAnterieurManuel,
                                                          String notes) {
        Long tenantId = getTenantId();
        RegimeTva reg = (regime != null) ? regime : RegimeTva.ENCAISSEMENT;
        BigDecimal pro = (prorata != null && prorata.compareTo(BigDecimal.ZERO) > 0) ? prorata : new BigDecimal("100.00");

        LocalDate[] bornes = resoudreBornesPeriode(periode);
        LocalDate dDebut = bornes[0];
        LocalDate dFin = bornes[1];

        BigDecimal totalVentesHT = BigDecimal.ZERO;
        BigDecimal totalAchatsHT = BigDecimal.ZERO;
        BigDecimal tvaCollectee = BigDecimal.ZERO;
        BigDecimal tvaDeductibleCharges = BigDecimal.ZERO;
        BigDecimal tvaDeductibleImmo = BigDecimal.ZERO;

        if (reg == RegimeTva.DEBIT) {
            // RÉGIME DES DÉBITS : calculé sur la base des écritures comptables facturées (4455 / 3455 / 71xx / 61xx)
            List<LigneEcriture> lignes = ligneRepository.findAllByTenantAndPeriode(tenantId, dDebut, dFin);
            for (LigneEcriture l : lignes) {
                if (l.getCompte() != null && l.getCompte().getNumeroCompte() != null) {
                    String num = l.getCompte().getNumeroCompte().trim();
                    BigDecimal deb = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                    BigDecimal cred = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;

                    if (num.startsWith("71") || (num.startsWith("7") && !num.startsWith("79"))) {
                        totalVentesHT = totalVentesHT.add(cred.subtract(deb));
                    } else if (num.startsWith("61") || (num.startsWith("6") && !num.startsWith("69"))) {
                        totalAchatsHT = totalAchatsHT.add(deb.subtract(cred));
                    } else if (num.startsWith("4455")) {
                        tvaCollectee = tvaCollectee.add(cred.subtract(deb));
                    } else if (num.startsWith("34552")) {
                        tvaDeductibleImmo = tvaDeductibleImmo.add(deb.subtract(cred));
                    } else if (num.startsWith("3455")) {
                        tvaDeductibleCharges = tvaDeductibleCharges.add(deb.subtract(cred));
                    }
                }
            }
        } else {
            // RÉGIME DES ENCAISSEMENTS (Droit commun fiscal marocain) :
            // TVA exigible sur encaissements réels / TVA déductible sur décaissements effectifs
            LocalDateTime dtDebut = dDebut.atStartOfDay();
            LocalDateTime dtFin = dFin.atTime(LocalTime.MAX);

            // 1. Encaissements clients
            List<Paiement> paiements = paiementRepository.findPaiementsByPeriodeAndTenant(dtDebut, dtFin, tenantId);
            for (Paiement p : paiements) {
                if (!Boolean.TRUE.equals(p.getAnnule()) && p.getMontant() != null) {
                    // Calcul de la quote-part TVA (taux moyen standard 20% si facture liée ou globale : montant * 20 / 120)
                    BigDecimal montantTTC = p.getMontant();
                    BigDecimal baseHT = montantTTC.divide(new BigDecimal("1.20"), 2, RoundingMode.HALF_UP);
                    BigDecimal tva = montantTTC.subtract(baseHT);

                    totalVentesHT = totalVentesHT.add(baseHT);
                    tvaCollectee = tvaCollectee.add(tva);
                }
            }

            // 2. Décaissements fournisseurs
            List<ReglementFournisseur> reglements = reglementFournisseurRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId);
            for (ReglementFournisseur r : reglements) {
                if (r.getMontant() != null) {
                    BigDecimal montantTTC = r.getMontant();
                    BigDecimal baseHT = montantTTC.divide(new BigDecimal("1.20"), 2, RoundingMode.HALF_UP);
                    BigDecimal tva = montantTTC.subtract(baseHT);

                    totalAchatsHT = totalAchatsHT.add(baseHT);
                    tvaDeductibleCharges = tvaDeductibleCharges.add(tva);
                }
            }

            // Immobilisations : TVA sur achats immo déduite selon compte 34552 de la période
            List<LigneEcriture> lignesImmo = ligneRepository.findAllByTenantAndPeriode(tenantId, dDebut, dFin);
            for (LigneEcriture l : lignesImmo) {
                if (l.getCompte() != null && l.getCompte().getNumeroCompte() != null) {
                    String num = l.getCompte().getNumeroCompte().trim();
                    if (num.startsWith("34552")) {
                        BigDecimal deb = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                        BigDecimal cred = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;
                        tvaDeductibleImmo = tvaDeductibleImmo.add(deb.subtract(cred));
                    }
                }
            }
        }

        if (totalVentesHT.compareTo(BigDecimal.ZERO) < 0) totalVentesHT = BigDecimal.ZERO;
        if (totalAchatsHT.compareTo(BigDecimal.ZERO) < 0) totalAchatsHT = BigDecimal.ZERO;
        if (tvaCollectee.compareTo(BigDecimal.ZERO) < 0) tvaCollectee = BigDecimal.ZERO;
        if (tvaDeductibleCharges.compareTo(BigDecimal.ZERO) < 0) tvaDeductibleCharges = BigDecimal.ZERO;
        if (tvaDeductibleImmo.compareTo(BigDecimal.ZERO) < 0) tvaDeductibleImmo = BigDecimal.ZERO;

        // Application du prorata de déduction (art. 104 du CGI marocain)
        BigDecimal ratioProrata = pro.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal tvaDeductibleChargesApresProrata = tvaDeductibleCharges.multiply(ratioProrata).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tvaDeductibleImmoApresProrata = tvaDeductibleImmo.multiply(ratioProrata).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tvaDeductibleTotal = tvaDeductibleChargesApresProrata.add(tvaDeductibleImmoApresProrata);

        // Report de crédit de TVA antérieur
        BigDecimal creditAnterieur = BigDecimal.ZERO;
        if (creditAnterieurManuel != null && creditAnterieurManuel.compareTo(BigDecimal.ZERO) > 0) {
            creditAnterieur = creditAnterieurManuel;
        } else {
            creditAnterieur = recupererCreditAnterieurAutomatique(periode, tenantId);
        }

        BigDecimal totalDeductibleAvecCredit = tvaDeductibleTotal.add(creditAnterieur);

        BigDecimal tvaAPayer = BigDecimal.ZERO;
        BigDecimal creditReportable = BigDecimal.ZERO;

        if (tvaCollectee.compareTo(totalDeductibleAvecCredit) >= 0) {
            tvaAPayer = tvaCollectee.subtract(totalDeductibleAvecCredit);
            creditReportable = BigDecimal.ZERO;
        } else {
            tvaAPayer = BigDecimal.ZERO;
            creditReportable = totalDeductibleAvecCredit.subtract(tvaCollectee);
        }

        // Sauvegarder ou mettre à jour la déclaration pour cette période
        DeclarationTva decl = declarationTvaRepository.findByPeriodeAndPointDeVenteId(periode, tenantId)
                .orElseGet(() -> {
                    DeclarationTva d = new DeclarationTva();
                    d.setPeriode(periode);
                    d.setPointDeVenteId(tenantId);
                    d.setDateCreation(LocalDateTime.now());
                    return d;
                });

        if (decl.getStatut() == StatutDeclarationTva.VALIDEE || decl.getStatut() == StatutDeclarationTva.DEPOSEE) {
            throw new IllegalStateException("La déclaration TVA pour la période " + periode + " est déjà validée ou déposée.");
        }

        decl.setRegime(reg);
        decl.setProrata(pro);
        decl.setTotalVentesHT(totalVentesHT);
        decl.setTotalAchatsHT(totalAchatsHT);
        decl.setTvaCollectee(tvaCollectee);
        decl.setTvaDeductibleCharges(tvaDeductibleChargesApresProrata);
        decl.setTvaDeductibleImmo(tvaDeductibleImmoApresProrata);
        decl.setTvaDeductibleTotal(tvaDeductibleTotal);
        decl.setCreditTvaAnterieur(creditAnterieur);
        decl.setTvaAPayer(tvaAPayer);
        decl.setCreditTvaReportable(creditReportable);
        decl.setNotes(notes);

        DeclarationTva saved = declarationTvaRepository.save(decl);
        auditService.logCreation("DECLARATION_TVA", saved.getId(), "Génération déclaration TVA " + periode + " (" + reg + ")");

        return toDto(saved);
    }

    public DeclarationTvaEnregistreeDTO validerDeclaration(Long id) {
        Long tenantId = getTenantId();
        DeclarationTva decl = declarationTvaRepository.findById(id)
                .filter(d -> d.getPointDeVenteId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Déclaration TVA introuvable ID: " + id));

        decl.setStatut(StatutDeclarationTva.VALIDEE);
        decl.setDateValidation(LocalDateTime.now());
        decl.setValideePar(getCurrentUser());

        DeclarationTva saved = declarationTvaRepository.save(decl);
        auditService.logValidation("DECLARATION_TVA", saved.getId(), "Validation déclaration TVA période " + saved.getPeriode());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DeclarationTvaEnregistreeDTO> getHistoriqueDeclarations() {
        Long tenantId = getTenantId();
        return declarationTvaRepository.findByPointDeVenteIdOrderByPeriodeDesc(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeclarationTvaEnregistreeDTO getDeclaration(Long id) {
        Long tenantId = getTenantId();
        return declarationTvaRepository.findById(id)
                .filter(d -> d.getPointDeVenteId().equals(tenantId))
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Déclaration TVA introuvable ID: " + id));
    }

    // =========================================================================
    // CONTRÔLE CROISÉ & RÉCONCILIATION TVA
    // =========================================================================

    @Transactional(readOnly = true)
    public ControleTvaDTO controleReconciliation(String periode) {
        Long tenantId = getTenantId();
        LocalDate[] bornes = resoudreBornesPeriode(periode);
        LocalDate dDebut = bornes[0];
        LocalDate dFin = bornes[1];
        LocalDateTime dtDebut = dDebut.atStartOfDay();
        LocalDateTime dtFin = dFin.atTime(LocalTime.MAX);

        ControleTvaDTO controle = new ControleTvaDTO();
        controle.setPeriode(periode);

        // 1. TVA commerciale factures ventes
        BigDecimal tvaVentesCommerciales = factureRepository.findFacturesByPeriodeAndPointDeVenteId(dDebut, dFin, tenantId).stream()
                .filter(f -> f.getStatut() != StatutFacture.ANNULEE && f.getMontantTVA() != null)
                .map(Facture::getMontantTVA)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. TVA commerciale factures achats
        BigDecimal tvaAchatsCommerciales = factureAchatRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId).stream()
                .filter(a -> a.getStatut() != StatutFacture.ANNULEE && a.getMontantTva() != null)
                .map(FactureAchat::getMontantTva)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. TVA comptable (écritures aux comptes 4455 et 3455)
        BigDecimal tvaCollecteeCompta = BigDecimal.ZERO;
        BigDecimal tvaDeductibleCompta = BigDecimal.ZERO;

        List<LigneEcriture> lignes = ligneRepository.findAllByTenantAndPeriode(tenantId, dDebut, dFin);
        for (LigneEcriture l : lignes) {
            if (l.getCompte() != null && l.getCompte().getNumeroCompte() != null) {
                String num = l.getCompte().getNumeroCompte().trim();
                BigDecimal deb = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                BigDecimal cred = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;

                if (num.startsWith("4455")) {
                    tvaCollecteeCompta = tvaCollecteeCompta.add(cred.subtract(deb));
                } else if (num.startsWith("3455")) {
                    tvaDeductibleCompta = tvaDeductibleCompta.add(deb.subtract(cred));
                }
            }
        }

        controle.setTvaCollecteeCommerciale(tvaVentesCommerciales);
        controle.setTvaCollecteeComptable(tvaCollecteeCompta);
        controle.setEcartTvaCollectee(tvaVentesCommerciales.subtract(tvaCollecteeCompta).abs());

        controle.setTvaDeductibleCommerciale(tvaAchatsCommerciales);
        controle.setTvaDeductibleComptable(tvaDeductibleCompta);
        controle.setEcartTvaDeductible(tvaAchatsCommerciales.subtract(tvaDeductibleCompta).abs());

        // 4. Déclaration TVA enregistrée
        Optional<DeclarationTva> declOpt = declarationTvaRepository.findByPeriodeAndPointDeVenteId(periode, tenantId);
        if (declOpt.isPresent()) {
            controle.setTvaDeclaree(declOpt.get().getTvaAPayer());
        }

        // 5. Alertes d'incohérence
        if (controle.getEcartTvaCollectee().compareTo(new BigDecimal("1.00")) > 0) {
            controle.setCoherent(false);
            controle.getAlertes().add(String.format("Écart de %s DH détecté sur la TVA Collectée entre Factures Ventes (%s DH) et Compte 4455 (%s DH). Certaines factures ne sont pas déversées.",
                    controle.getEcartTvaCollectee(), tvaVentesCommerciales, tvaCollecteeCompta));
        }

        if (controle.getEcartTvaDeductible().compareTo(new BigDecimal("1.00")) > 0) {
            controle.setCoherent(false);
            controle.getAlertes().add(String.format("Écart de %s DH détecté sur la TVA Déductible entre Factures Achats (%s DH) et Compte 3455 (%s DH). Vérifiez les déversements d'achats.",
                    controle.getEcartTvaDeductible(), tvaAchatsCommerciales, tvaDeductibleCompta));
        }

        if (controle.isCoherent()) {
            controle.getAlertes().add("Parfaite concordance : les montants commerciaux concordent avec les écritures du grand livre comptable.");
        }

        return controle;
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private BigDecimal recupererCreditAnterieurAutomatique(String periodeCourante, Long tenantId) {
        try {
            if (periodeCourante.matches("^\\d{4}-\\d{2}$")) {
                YearMonth ym = YearMonth.parse(periodeCourante);
                YearMonth prevYm = ym.minusMonths(1);
                String prevPeriode = prevYm.toString();

                Optional<DeclarationTva> prev = declarationTvaRepository.findByPeriodeAndPointDeVenteId(prevPeriode, tenantId);
                if (prev.isPresent() && prev.get().getCreditTvaReportable() != null) {
                    return prev.get().getCreditTvaReportable();
                }
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    private LocalDate[] resoudreBornesPeriode(String periode) {
        if (periode == null || periode.trim().isEmpty()) {
            YearMonth ym = YearMonth.now();
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }

        String p = periode.trim();
        // Format mensuel "YYYY-MM"
        if (p.matches("^\\d{4}-\\d{2}$")) {
            YearMonth ym = YearMonth.parse(p);
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }
        // Format trimestriel "YYYY-T1", "YYYY-T2", etc.
        if (p.matches("^\\d{4}-T[1-4]$")) {
            int year = Integer.parseInt(p.substring(0, 4));
            int trim = Integer.parseInt(p.substring(6, 7));
            int startMonth = (trim - 1) * 3 + 1;
            int endMonth = startMonth + 2;
            LocalDate start = LocalDate.of(year, startMonth, 1);
            LocalDate end = YearMonth.of(year, endMonth).atEndOfMonth();
            return new LocalDate[]{start, end};
        }
        // Année seule "YYYY"
        if (p.matches("^\\d{4}$")) {
            int year = Integer.parseInt(p);
            return new LocalDate[]{LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)};
        }

        YearMonth ym = YearMonth.now();
        return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
    }

    private DeclarationTvaEnregistreeDTO toDto(DeclarationTva decl) {
        DeclarationTvaEnregistreeDTO dto = new DeclarationTvaEnregistreeDTO();
        dto.setId(decl.getId());
        dto.setPeriode(decl.getPeriode());
        dto.setRegime(decl.getRegime());
        dto.setTvaCollectee(decl.getTvaCollectee());
        dto.setTvaDeductibleCharges(decl.getTvaDeductibleCharges());
        dto.setTvaDeductibleImmo(decl.getTvaDeductibleImmo());
        dto.setTvaDeductibleTotal(decl.getTvaDeductibleTotal());
        dto.setCreditTvaAnterieur(decl.getCreditTvaAnterieur());
        dto.setTvaAPayer(decl.getTvaAPayer());
        dto.setCreditTvaReportable(decl.getCreditTvaReportable());
        dto.setProrata(decl.getProrata());
        dto.setTotalVentesHT(decl.getTotalVentesHT());
        dto.setTotalAchatsHT(decl.getTotalAchatsHT());
        dto.setStatut(decl.getStatut());
        dto.setDateCreation(decl.getDateCreation());
        dto.setDateValidation(decl.getDateValidation());
        dto.setValideePar(decl.getValideePar());
        dto.setNotes(decl.getNotes());
        return dto;
    }

    // =========================================================================
    // RELEVÉ DE DÉDUCTION DE TVA (ARTICLE 112 CGI MAROC)
    // AVEC CONTRÔLE PAR RAPPROCHEMENT BANCAIRE
    // =========================================================================

    @Transactional(readOnly = true)
    public ReleveDeductionTvaDTO getReleveDeduction(LocalDate dateDebut, LocalDate dateFin, Boolean seulementRapproches) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (dateDebut != null) ? dateDebut : YearMonth.now().atDay(1);
        LocalDate dFin = (dateFin != null) ? dateFin : YearMonth.now().atEndOfMonth();
        boolean filtreRapproche = Boolean.TRUE.equals(seulementRapproches);

        ReleveDeductionTvaDTO releve = new ReleveDeductionTvaDTO();
        releve.setDateDebut(dDebut);
        releve.setDateFin(dFin);
        releve.setSeulementRapproches(filtreRapproche);
        releve.setAnnee(dDebut.getYear());
        releve.setPeriode(String.format("%02d/%d", dDebut.getMonthValue(), dDebut.getYear()));
        releve.setRegime("Encaissement");

        // Société émettrice
        Societe societe = societeRepository.findById(tenantId)
                .orElseGet(() -> societeRepository.findByTenantIdAndIsParDefautTrue(tenantId)
                        .orElseGet(Societe::new));
        releve.setRaisonSociale(societe.getRaisonSociale() != null ? societe.getRaisonSociale() : "Société " + tenantId);
        releve.setIdentifiantFiscal(societe.getIdentifiantFiscal() != null ? societe.getIdentifiantFiscal() : "");
        releve.setIce(societe.getIce() != null ? societe.getIce() : "");

        LocalDateTime dtDebut = dDebut.atStartOfDay();
        LocalDateTime dtFin = dFin.atTime(LocalTime.MAX);

        // Récupérer les lignes de relevé bancaire de la période
        List<LigneReleveBancaire> bankLines = ligneReleveRepository.findByTenantAndDates(dDebut, dFin, tenantId);
        List<LigneReleveBancaire> bankLinesRapprochees = bankLines.stream()
                .filter(l -> l.getStatut() == StatutRapprochement.RAPPROCHE)
                .collect(Collectors.toList());

        List<LigneReleveDeductionDTO> toutesLesLignes = new ArrayList<>();
        Set<String> clesTraitees = new HashSet<>();

        // 1. Dépenses par règlements fournisseurs dans la période
        List<ReglementFournisseur> reglements = reglementFournisseurRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId);
        for (ReglementFournisseur r : reglements) {
            FactureAchat f = r.getFactureAchat();
            Fournisseur fr = (f != null) ? f.getFournisseur() : null;

            LigneReleveDeductionDTO ligne = new LigneReleveDeductionDTO();
            ligne.setNumeroFacture((f != null && f.getNumeroFacture() != null) ? f.getNumeroFacture() : r.getNumeroReglement());
            ligne.setDateFacture((f != null && f.getDateFacture() != null) ? f.getDateFacture().toLocalDate() : r.getDateReglement().toLocalDate());
            ligne.setNomFournisseur((fr != null && fr.getNom() != null) ? fr.getNom() : (r.getNomBanque() != null ? r.getNomBanque() : "Fournisseur"));
            ligne.setIdentifiantFiscal((fr != null && fr.getNumeroIdentificationFiscale() != null) ? fr.getNumeroIdentificationFiscale() : "");
            ligne.setIce((fr != null && fr.getIce() != null) ? fr.getIce() : "");
            ligne.setDesignation((f != null && f.getObservations() != null && !f.getObservations().isBlank()) ? f.getObservations() : "Achats et charges d'exploitation");
            ligne.setModePaiement(r.getModePaiement() != null ? r.getModePaiement().name() : "VIREMENT");
            ligne.setDatePaiement(r.getDateReglement().toLocalDate());
            ligne.setCompte("6111");
            ligne.setProrata(new BigDecimal("100.00"));

            BigDecimal mntTtc = r.getMontant() != null ? r.getMontant() : BigDecimal.ZERO;
            ligne.setMontantTtc(mntTtc);

            // Taux TVA & Calcul HT / TVA
            BigDecimal taux = new BigDecimal("20.00");
            if (f != null && f.getMontantHt() != null && f.getMontantHt().compareTo(BigDecimal.ZERO) > 0 && f.getMontantTva() != null) {
                taux = f.getMontantTva().multiply(new BigDecimal("100")).divide(f.getMontantHt(), 0, RoundingMode.HALF_UP);
            }
            ligne.setTauxTva(taux);

            BigDecimal coefDiv = BigDecimal.ONE.add(taux.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            BigDecimal baseHt = mntTtc.divide(coefDiv, 2, RoundingMode.HALF_UP);
            BigDecimal tva = mntTtc.subtract(baseHt);

            ligne.setMontantHt(baseHt);
            ligne.setMontantTva(tva);
            ligne.setMontantDeductible(tva);

            // Vérification de rapprochement bancaire
            boolean isRapproche = false;
            for (LigneReleveBancaire bl : bankLinesRapprochees) {
                if (bl.getDebit() != null && bl.getDebit().compareTo(mntTtc) == 0) {
                    isRapproche = true;
                    ligne.setDateRapprochement(bl.getDateRapprochement());
                    ligne.setReferenceBancaire(bl.getReference());
                    if (bl.getDateOperation() != null) {
                        ligne.setDatePaiement(bl.getDateOperation());
                    }
                    break;
                } else if (f != null && f.getNumeroFacture() != null && bl.getLibelle() != null && bl.getLibelle().toUpperCase().contains(f.getNumeroFacture().toUpperCase())) {
                    isRapproche = true;
                    ligne.setDateRapprochement(bl.getDateRapprochement());
                    ligne.setReferenceBancaire(bl.getReference());
                    break;
                }
            }

            ligne.setStatutRapprochement(isRapproche ? "RAPPROCHE" : "NON_RAPPROCHE");
            toutesLesLignes.add(ligne);
            if (f != null && f.getNumeroFacture() != null) {
                clesTraitees.add(f.getNumeroFacture().toUpperCase());
            }
        }

        // 2. Factures d'achat directes de la période non encore passées par règlement explicite
        List<FactureAchat> facturesPeriode = factureAchatRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId);
        for (FactureAchat f : facturesPeriode) {
            if (f.getNumeroFacture() != null && clesTraitees.contains(f.getNumeroFacture().toUpperCase())) {
                continue; // Déjà traitée via son règlement
            }

            Fournisseur fr = f.getFournisseur();
            LigneReleveDeductionDTO ligne = new LigneReleveDeductionDTO();
            ligne.setNumeroFacture(f.getNumeroFacture());
            ligne.setDateFacture(f.getDateFacture() != null ? f.getDateFacture().toLocalDate() : dDebut);
            ligne.setNomFournisseur((fr != null && fr.getNom() != null) ? fr.getNom() : "Fournisseur");
            ligne.setIdentifiantFiscal((fr != null && fr.getNumeroIdentificationFiscale() != null) ? fr.getNumeroIdentificationFiscale() : "");
            ligne.setIce((fr != null && fr.getIce() != null) ? fr.getIce() : "");
            ligne.setDesignation((f.getObservations() != null && !f.getObservations().isBlank()) ? f.getObservations() : "Achats de matières et fournitures");
            ligne.setModePaiement("VIREMENT");
            ligne.setDatePaiement(ligne.getDateFacture());
            ligne.setCompte("6111");
            ligne.setProrata(new BigDecimal("100.00"));

            BigDecimal ht = f.getMontantHt() != null ? f.getMontantHt() : BigDecimal.ZERO;
            BigDecimal tva = f.getMontantTva() != null ? f.getMontantTva() : BigDecimal.ZERO;
            BigDecimal ttc = f.getMontantTtc() != null ? f.getMontantTtc() : ht.add(tva);

            ligne.setMontantHt(ht);
            ligne.setMontantTva(tva);
            ligne.setMontantTtc(ttc);
            ligne.setMontantDeductible(tva);

            BigDecimal taux = new BigDecimal("20.00");
            if (ht.compareTo(BigDecimal.ZERO) > 0 && tva.compareTo(BigDecimal.ZERO) > 0) {
                taux = tva.multiply(new BigDecimal("100")).divide(ht, 0, RoundingMode.HALF_UP);
            }
            ligne.setTauxTva(taux);

            // Vérifier si cette facture correspond à un débit bancaire rapproché
            boolean isRapproche = false;
            for (LigneReleveBancaire bl : bankLinesRapprochees) {
                if (bl.getDebit() != null && bl.getDebit().compareTo(ttc) == 0) {
                    isRapproche = true;
                    ligne.setDateRapprochement(bl.getDateRapprochement());
                    ligne.setReferenceBancaire(bl.getReference());
                    if (bl.getDateOperation() != null) {
                        ligne.setDatePaiement(bl.getDateOperation());
                    }
                    break;
                } else if (bl.getLibelle() != null && bl.getLibelle().toUpperCase().contains(f.getNumeroFacture().toUpperCase())) {
                    isRapproche = true;
                    ligne.setDateRapprochement(bl.getDateRapprochement());
                    ligne.setReferenceBancaire(bl.getReference());
                    break;
                }
            }

            ligne.setStatutRapprochement(isRapproche ? "RAPPROCHE" : "NON_RAPPROCHE");
            toutesLesLignes.add(ligne);
            clesTraitees.add(f.getNumeroFacture().toUpperCase());
        }

        // 3. Frais bancaires et charges directes identifiées sur les relevés bancaires
        for (LigneReleveBancaire bl : bankLines) {
            if (bl.getDebit() != null && bl.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                String lib = (bl.getLibelle() != null) ? bl.getLibelle().toUpperCase() : "";
                boolean isFraisBancaire = lib.contains("FRAIS") || lib.contains("COMMISSION") || lib.contains("AGIOS")
                        || lib.contains("TENUE DE COMPTE") || lib.contains("COTISATION") || lib.contains("FORFAIT");

                if (isFraisBancaire) {
                    LigneReleveDeductionDTO ligne = new LigneReleveDeductionDTO();
                    ligne.setNumeroFacture(bl.getReference() != null ? bl.getReference() : "REL-" + bl.getId());
                    ligne.setDateFacture(bl.getDateOperation() != null ? bl.getDateOperation() : dDebut);
                    ligne.setNomFournisseur(extraireNomBanque(bl));
                    ligne.setIdentifiantFiscal("3301185"); // IF standard banque marocaine
                    ligne.setIce("00152720800008"); // ICE standard banque marocaine
                    ligne.setDesignation("Frais bancaires et commissions");
                    ligne.setModePaiement("142"); // Code officiel prélèvement bancaire direct DGI
                    ligne.setDatePaiement(bl.getDateOperation());
                    ligne.setCompte("6147");
                    ligne.setProrata(new BigDecimal("100.00"));
                    ligne.setTauxTva(new BigDecimal("10.00")); // Taux TVA légal frais bancaires marocains = 10%

                    BigDecimal ttc = bl.getDebit();
                    BigDecimal ht = ttc.divide(new BigDecimal("1.10"), 2, RoundingMode.HALF_UP);
                    BigDecimal tva = ttc.subtract(ht);

                    ligne.setMontantHt(ht);
                    ligne.setMontantTva(tva);
                    ligne.setMontantTtc(ttc);
                    ligne.setMontantDeductible(tva);

                    ligne.setStatutRapprochement("RAPPROCHE"); // Issu directement du relevé
                    ligne.setDateRapprochement(bl.getDateRapprochement());
                    ligne.setReferenceBancaire(bl.getReference());

                    toutesLesLignes.add(ligne);
                }
            }
        }

        // 4. Calcul des totaux globaux de contrôle
        BigDecimal totRapproche = BigDecimal.ZERO;
        BigDecimal totNonRapproche = BigDecimal.ZERO;
        int nbRapproche = 0;
        int nbNonRapproche = 0;

        for (LigneReleveDeductionDTO l : toutesLesLignes) {
            if ("RAPPROCHE".equalsIgnoreCase(l.getStatutRapprochement())) {
                totRapproche = totRapproche.add(l.getMontantDeductible());
                nbRapproche++;
            } else {
                totNonRapproche = totNonRapproche.add(l.getMontantDeductible());
                nbNonRapproche++;
            }
        }

        releve.setTotalTvaRapprochee(totRapproche);
        releve.setTotalTvaNonRapprochee(totNonRapproche);
        releve.setNbLignesTotal(toutesLesLignes.size());
        releve.setNbLignesRapprochees(nbRapproche);
        releve.setNbLignesNonRapprochees(nbNonRapproche);

        BigDecimal sumTvaTotal = totRapproche.add(totNonRapproche);
        if (sumTvaTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tauxCouv = totRapproche.multiply(new BigDecimal("100")).divide(sumTvaTotal, 1, RoundingMode.HALF_UP);
            releve.setTauxCouvertureRapprochement(tauxCouv);
        } else {
            releve.setTauxCouvertureRapprochement(BigDecimal.ZERO);
        }

        // 5. Filtrage selon les 2 possibilités ("j ai deux possibilité rapporocher ou pas")
        List<LigneReleveDeductionDTO> lignesFinales;
        if (filtreRapproche) {
            // Possibilité 1 : Uniquement les lignes RAPPROCHÉES
            lignesFinales = toutesLesLignes.stream()
                    .filter(l -> "RAPPROCHE".equalsIgnoreCase(l.getStatutRapprochement()))
                    .collect(Collectors.toList());
        } else {
            // Possibilité 2 : TOUTES les lignes
            lignesFinales = toutesLesLignes;
        }

        // Numérotation d'ordre 1, 2, 3...
        BigDecimal totHt = BigDecimal.ZERO;
        BigDecimal totTva = BigDecimal.ZERO;
        BigDecimal totTtc = BigDecimal.ZERO;
        BigDecimal totDeductible = BigDecimal.ZERO;

        int ordre = 1;
        for (LigneReleveDeductionDTO l : lignesFinales) {
            l.setNumOrdre(ordre++);
            totHt = totHt.add(l.getMontantHt());
            totTva = totTva.add(l.getMontantTva());
            totTtc = totTtc.add(l.getMontantTtc());
            totDeductible = totDeductible.add(l.getMontantDeductible());
        }

        releve.setTotalMontantHt(totHt);
        releve.setTotalMontantTva(totTva);
        releve.setTotalMontantTtc(totTtc);
        releve.setTotalMontantDeductible(totDeductible);
        releve.setLignes(lignesFinales);

        return releve;
    }

    private String extraireNomBanque(LigneReleveBancaire bl) {
        if (bl != null && bl.getReleveBancaire() != null && bl.getReleveBancaire().getCompteFinancier() != null) {
            CompteFinancier cf = bl.getReleveBancaire().getCompteFinancier();
            if (cf.getNomBanque() != null && !cf.getNomBanque().isBlank()) {
                return cf.getNomBanque();
            }
            if (cf.getBanque() != null && cf.getBanque().getNom() != null && !cf.getBanque().getNom().isBlank()) {
                return cf.getBanque().getNom();
            }
            if (cf.getNom() != null && !cf.getNom().isBlank()) {
                return cf.getNom();
            }
        }
        return "CREDIT AGRICOLE DU MAROC";
    }

    public byte[] exporterReleveDeductionXlsx(LocalDate dateDebut, LocalDate dateFin, Boolean seulementRapproches) {
        ReleveDeductionTvaDTO r = getReleveDeduction(dateDebut, dateFin, seulementRapproches);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Relevé Déduction Art.112");

            // === Styles ===
            // Titre principal
            org.apache.poi.ss.usermodel.CellStyle titleStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);

            // En-tête section (raison sociale etc.)
            org.apache.poi.ss.usermodel.CellStyle headerInfoStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
            boldFont.setBold(true);
            headerInfoStyle.setFont(boldFont);

            // En-tête colonnes tableau
            org.apache.poi.ss.usermodel.CellStyle colHeaderStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font colHeaderFont = wb.createFont();
            colHeaderFont.setBold(true);
            colHeaderFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            colHeaderStyle.setFont(colHeaderFont);
            colHeaderStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 31, (byte) 73, (byte) 125}, null));
            colHeaderStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            colHeaderStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.MEDIUM);
            colHeaderStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            colHeaderStyle.setWrapText(true);

            // Style montant numérique
            org.apache.poi.ss.usermodel.CellStyle numStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat fmt = wb.createDataFormat();
            numStyle.setDataFormat(fmt.getFormat("#,##0.00"));
            numStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            numStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            // Style texte ligne
            org.apache.poi.ss.usermodel.CellStyle textStyle = wb.createCellStyle();
            textStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            textStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            // Style ligne RAPPROCHEE (vert clair)
            org.apache.poi.ss.usermodel.CellStyle rapprocheStyle = wb.createCellStyle();
            rapprocheStyle.cloneStyleFrom(textStyle);
            rapprocheStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 198, (byte) 239, (byte) 206}, null));
            rapprocheStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            rapprocheStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font greenFont = wb.createFont();
            greenFont.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 0, (byte) 97, (byte) 0}, null));
            rapprocheStyle.setFont(greenFont);

            // Style ligne NON RAPPROCHEE (orange clair)
            org.apache.poi.ss.usermodel.CellStyle nonRapprocheStyle = wb.createCellStyle();
            nonRapprocheStyle.cloneStyleFrom(textStyle);
            nonRapprocheStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 255, (byte) 235, (byte) 156}, null));
            nonRapprocheStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            nonRapprocheStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font orangeFont = wb.createFont();
            orangeFont.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 156, (byte) 87, (byte) 0}, null));
            nonRapprocheStyle.setFont(orangeFont);

            // Style ligne de totaux
            org.apache.poi.ss.usermodel.CellStyle totalStyle = wb.createCellStyle();
            totalStyle.setDataFormat(fmt.getFormat("#,##0.00"));
            org.apache.poi.ss.usermodel.Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 197, (byte) 217, (byte) 241}, null));
            totalStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            totalStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.MEDIUM);
            totalStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.MEDIUM);

            // Style numStyle pour totaux
            org.apache.poi.ss.usermodel.CellStyle numRapprocheStyle = wb.createCellStyle();
            numRapprocheStyle.cloneStyleFrom(numStyle);
            numRapprocheStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 198, (byte) 239, (byte) 206}, null));
            numRapprocheStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            org.apache.poi.ss.usermodel.CellStyle numNonRapprocheStyle = wb.createCellStyle();
            numNonRapprocheStyle.cloneStyleFrom(numStyle);
            numNonRapprocheStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 255, (byte) 235, (byte) 156}, null));
            numNonRapprocheStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            int rowNum = 0;

            // === BLOC EN-TÊTE OFFICIEL DGI ===
            String[][] entete = {
                {"RAISON SOCIALE", r.getRaisonSociale() != null ? r.getRaisonSociale() : ""},
                {"IDENTIFIANT FISCAL", r.getIdentifiantFiscal() != null ? r.getIdentifiantFiscal() : ""},
                {"ICE", r.getIce() != null ? r.getIce() : ""},
                {"ANNEE", String.valueOf(r.getAnnee())},
                {"PERIODE", r.getPeriode() != null ? r.getPeriode() : ""},
                {"REGIME", r.getRegime() != null ? r.getRegime() : ""},
                {"FILTRE", Boolean.TRUE.equals(seulementRapproches) ? "RAPPROCHE_SEULEMENT" : "TOUTES_LES_OPERATIONS"},
            };
            for (String[] kv : entete) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell k = row.createCell(0);
                k.setCellValue(kv[0]);
                k.setCellStyle(headerInfoStyle);
                org.apache.poi.ss.usermodel.Cell v = row.createCell(1);
                v.setCellValue(kv[1]);
            }

            // Ligne vide
            sheet.createRow(rowNum++);

            // === TITRE PRINCIPAL ===
            org.apache.poi.ss.usermodel.Row titreRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titreCell = titreRow.createCell(0);
            titreCell.setCellValue("RELEVE DE DEDUCTION (Article 112 du Code Général des Impôts)");
            titreCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 17));

            // Ligne vide
            sheet.createRow(rowNum++);

            // === EN-TÊTES COLONNES ===
            String[] headers = {
                "N°", "N° Facture", "Date Facture", "Nom du FR", "IF du FR", "ICE",
                "Désignation", "Montant HT", "Taux", "Montant TVA", "Montant TTC",
                "Prorata", "Montant Déductible", "Mode Paiement", "Date Paiement",
                "Compte", "Statut Rapprochement", "Réf. Bancaire"
            };
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
            headerRow.setHeightInPoints(30);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(colHeaderStyle);
            }

            // === LIGNES DE DONNÉES ===
            for (LigneReleveDeductionDTO l : r.getLignes()) {
                boolean isRapproche = "RAPPROCHE".equalsIgnoreCase(l.getStatutRapprochement());
                org.apache.poi.ss.usermodel.CellStyle rowTextStyle = isRapproche ? rapprocheStyle : nonRapprocheStyle;
                org.apache.poi.ss.usermodel.CellStyle rowNumStyle = isRapproche ? numRapprocheStyle : numNonRapprocheStyle;

                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowNum++);
                int col = 0;

                org.apache.poi.ss.usermodel.Cell c0 = dataRow.createCell(col++);
                c0.setCellValue(l.getNumOrdre() != null ? l.getNumOrdre() : 0);
                c0.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c1 = dataRow.createCell(col++);
                c1.setCellValue(l.getNumeroFacture() != null ? l.getNumeroFacture() : "");
                c1.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c2 = dataRow.createCell(col++);
                c2.setCellValue(l.getDateFacture() != null ? l.getDateFacture().toString() : "");
                c2.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c3 = dataRow.createCell(col++);
                c3.setCellValue(l.getNomFournisseur() != null ? l.getNomFournisseur() : "");
                c3.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c4 = dataRow.createCell(col++);
                c4.setCellValue(l.getIdentifiantFiscal() != null ? l.getIdentifiantFiscal() : "");
                c4.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c5 = dataRow.createCell(col++);
                c5.setCellValue(l.getIce() != null ? l.getIce() : "");
                c5.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c6 = dataRow.createCell(col++);
                c6.setCellValue(l.getDesignation() != null ? l.getDesignation() : "");
                c6.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c7 = dataRow.createCell(col++);
                c7.setCellValue(l.getMontantHt() != null ? l.getMontantHt().doubleValue() : 0.0);
                c7.setCellStyle(rowNumStyle);

                org.apache.poi.ss.usermodel.Cell c8 = dataRow.createCell(col++);
                c8.setCellValue(l.getTauxTva() != null ? l.getTauxTva().intValue() + "%" : "0%");
                c8.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c9 = dataRow.createCell(col++);
                c9.setCellValue(l.getMontantTva() != null ? l.getMontantTva().doubleValue() : 0.0);
                c9.setCellStyle(rowNumStyle);

                org.apache.poi.ss.usermodel.Cell c10 = dataRow.createCell(col++);
                c10.setCellValue(l.getMontantTtc() != null ? l.getMontantTtc().doubleValue() : 0.0);
                c10.setCellStyle(rowNumStyle);

                org.apache.poi.ss.usermodel.Cell c11 = dataRow.createCell(col++);
                c11.setCellValue(l.getProrata() != null ? l.getProrata().intValue() + "%" : "100%");
                c11.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c12 = dataRow.createCell(col++);
                c12.setCellValue(l.getMontantDeductible() != null ? l.getMontantDeductible().doubleValue() : 0.0);
                c12.setCellStyle(rowNumStyle);

                org.apache.poi.ss.usermodel.Cell c13 = dataRow.createCell(col++);
                c13.setCellValue(l.getModePaiement() != null ? l.getModePaiement() : "");
                c13.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c14 = dataRow.createCell(col++);
                c14.setCellValue(l.getDatePaiement() != null ? l.getDatePaiement().toString() : "");
                c14.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c15 = dataRow.createCell(col++);
                c15.setCellValue(l.getCompte() != null ? l.getCompte() : "");
                c15.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c16 = dataRow.createCell(col++);
                c16.setCellValue(l.getStatutRapprochement() != null ? l.getStatutRapprochement() : "");
                c16.setCellStyle(rowTextStyle);

                org.apache.poi.ss.usermodel.Cell c17 = dataRow.createCell(col++);
                c17.setCellValue(l.getReferenceBancaire() != null ? l.getReferenceBancaire() : "");
                c17.setCellStyle(rowTextStyle);
            }

            // === LIGNE DE TOTAUX ===
            org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowNum++);
            // Colonnes vides 0-5
            for (int i = 0; i < 6; i++) {
                org.apache.poi.ss.usermodel.Cell tc = totalRow.createCell(i);
                tc.setCellStyle(totalStyle);
            }
            org.apache.poi.ss.usermodel.Cell totalLabel = totalRow.createCell(6);
            totalLabel.setCellValue("TOTAL");
            totalLabel.setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totHT = totalRow.createCell(7);
            totHT.setCellValue(r.getTotalMontantHt() != null ? r.getTotalMontantHt().doubleValue() : 0.0);
            totHT.setCellStyle(totalStyle);

            totalRow.createCell(8).setCellStyle(totalStyle); // Taux vide

            org.apache.poi.ss.usermodel.Cell totTVA = totalRow.createCell(9);
            totTVA.setCellValue(r.getTotalMontantTva() != null ? r.getTotalMontantTva().doubleValue() : 0.0);
            totTVA.setCellStyle(totalStyle);

            org.apache.poi.ss.usermodel.Cell totTTC = totalRow.createCell(10);
            totTTC.setCellValue(r.getTotalMontantTtc() != null ? r.getTotalMontantTtc().doubleValue() : 0.0);
            totTTC.setCellStyle(totalStyle);

            totalRow.createCell(11).setCellStyle(totalStyle); // Prorata vide

            org.apache.poi.ss.usermodel.Cell totDed = totalRow.createCell(12);
            totDed.setCellValue(r.getTotalMontantDeductible() != null ? r.getTotalMontantDeductible().doubleValue() : 0.0);
            totDed.setCellStyle(totalStyle);

            for (int i = 13; i < 18; i++) {
                totalRow.createCell(i).setCellStyle(totalStyle);
            }

            // === BLOC CONTRÔLE RAPPROCHEMENT ===
            rowNum++;
            org.apache.poi.ss.usermodel.Row controlTitleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell ctitle = controlTitleRow.createCell(0);
            ctitle.setCellValue("CONTRÔLE RAPPROCHEMENT BANCAIRE");
            ctitle.setCellStyle(headerInfoStyle);

            String[][] controles = {
                {"TVA Déductible Rapprochée (Art.112 sécurisée)", r.getTotalTvaRapprochee() != null ? r.getTotalTvaRapprochee().setScale(2, RoundingMode.HALF_UP).toString() + " DH" : "0.00 DH"},
                {"TVA en attente de pointage bancaire", r.getTotalTvaNonRapprochee() != null ? r.getTotalTvaNonRapprochee().setScale(2, RoundingMode.HALF_UP).toString() + " DH" : "0.00 DH"},
                {"Nb lignes rapprochées", String.valueOf(r.getNbLignesRapprochees()) + " / " + r.getNbLignesTotal()},
                {"Taux de couverture bancaire", r.getTauxCouvertureRapprochement() != null ? r.getTauxCouvertureRapprochement().toString() + " %" : "0 %"},
            };
            for (String[] kv : controles) {
                org.apache.poi.ss.usermodel.Row cRow = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell ck = cRow.createCell(0);
                ck.setCellValue(kv[0]);
                ck.setCellStyle(headerInfoStyle);
                org.apache.poi.ss.usermodel.Cell cv = cRow.createCell(1);
                cv.setCellValue(kv[1]);
            }

            // === LARGEURS COLONNES ===
            int[] colWidths = {8, 18, 14, 25, 15, 20, 35, 14, 8, 14, 14, 10, 16, 16, 14, 10, 20, 18};
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }

            // Figer les en-têtes colonnes
            sheet.createFreezePane(0, rowNum - r.getLignes().size() - 2);

            // Sérialiser vers bytes
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération XLSX Relevé de déduction Art.112 : " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace(";", " ").replace("\n", " ").replace("\r", "");
    }
}

