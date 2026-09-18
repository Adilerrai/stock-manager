package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ControleTvaDTO;
import com.gestion.persistent.dto.DeclarationTvaEnregistreeDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.enums.RegimeTva;
import com.gestion.persistent.enums.StatutDeclarationTva;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
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
    private final AuditService auditService;

    public TvaAvanceeService(DeclarationTvaRepository declarationTvaRepository,
                             LigneEcritureRepository ligneRepository,
                             FactureRepository factureRepository,
                             FactureAchatRepository factureAchatRepository,
                             PaiementRepository paiementRepository,
                             ReglementFournisseurRepository reglementFournisseurRepository,
                             AuditService auditService) {
        this.declarationTvaRepository = declarationTvaRepository;
        this.ligneRepository = ligneRepository;
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.paiementRepository = paiementRepository;
        this.reglementFournisseurRepository = reglementFournisseurRepository;
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
}
