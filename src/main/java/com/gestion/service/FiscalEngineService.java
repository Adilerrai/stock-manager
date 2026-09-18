package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.model.RegleFiscaleIS;
import com.gestion.repository.RegleFiscaleISRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class FiscalEngineService {

    private final RegleFiscaleISRepository regleFiscaleISRepository;
    private final ComptabiliteService comptabiliteService;

    public FiscalEngineService(RegleFiscaleISRepository regleFiscaleISRepository,
                               ComptabiliteService comptabiliteService) {
        this.regleFiscaleISRepository = regleFiscaleISRepository;
        this.comptabiliteService = comptabiliteService;
    }

    private Long getTenantId() {
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    // =========================================================================
    // GESTION DES RÈGLES FISCALES VERSIONNÉES
    // =========================================================================

    public RegleFiscaleIS getOrCreateRegleFiscale(int anneeFiscale) {
        Long tenantId = getTenantId();
        return regleFiscaleISRepository.findByAnneeFiscaleAndPointDeVenteId(anneeFiscale, tenantId)
                .orElseGet(() -> {
                    // Initialisation selon barème progressif officiel de la Loi de Finances marocaine
                    RegleFiscaleIS regle = new RegleFiscaleIS(anneeFiscale, tenantId);
                    regle.setSeuilTranche1(new BigDecimal("300000.00"));
                    regle.setTauxTranche1(new BigDecimal("0.1000")); // 10%
                    regle.setSeuilTranche2(new BigDecimal("1000000.00"));
                    regle.setTauxTranche2(new BigDecimal("0.2000")); // 20%
                    regle.setTauxTranche3(new BigDecimal("0.3100")); // 31%
                    regle.setTauxCotisationMinimale(new BigDecimal("0.0050")); // 0.50%
                    regle.setPlancherCotisationMinimale(new BigDecimal("3000.00")); // Plancher légal 3000 MAD
                    return regleFiscaleISRepository.save(regle);
                });
    }

    public RegleFiscaleIS modifierRegleFiscale(int anneeFiscale, RegleFiscaleIS dto) {
        RegleFiscaleIS regle = getOrCreateRegleFiscale(anneeFiscale);
        if (dto.getSeuilTranche1() != null) regle.setSeuilTranche1(dto.getSeuilTranche1());
        if (dto.getTauxTranche1() != null) regle.setTauxTranche1(dto.getTauxTranche1());
        if (dto.getSeuilTranche2() != null) regle.setSeuilTranche2(dto.getSeuilTranche2());
        if (dto.getTauxTranche2() != null) regle.setTauxTranche2(dto.getTauxTranche2());
        if (dto.getTauxTranche3() != null) regle.setTauxTranche3(dto.getTauxTranche3());
        if (dto.getTauxCotisationMinimale() != null) regle.setTauxCotisationMinimale(dto.getTauxCotisationMinimale());
        if (dto.getPlancherCotisationMinimale() != null) regle.setPlancherCotisationMinimale(dto.getPlancherCotisationMinimale());
        regle.setDateMiseAJour(java.time.LocalDateTime.now());
        return regleFiscaleISRepository.save(regle);
    }

    // =========================================================================
    // CALCUL RÉEL DE L'IS & COTISATION MINIMALE (DONNÉES COMPTABLES RÉELLES)
    // =========================================================================

    @Transactional(readOnly = true)
    public CalculIsDTO calculerIs(int anneeFiscale, BigDecimal reintegrations, BigDecimal deductions) {
        Long tenantId = getTenantId();
        RegleFiscaleIS regle = getOrCreateRegleFiscale(anneeFiscale);

        LocalDate dateDebut = LocalDate.of(anneeFiscale, 1, 1);
        LocalDate dateFin = LocalDate.of(anneeFiscale, 12, 31);

        // Données réelles issues du CPC officiel
        CpcOfficielDTO cpc = comptabiliteService.getCpcOfficiel(dateDebut, dateFin);

        CalculIsDTO dto = new CalculIsDTO();
        dto.setAnneeFiscale(anneeFiscale);
        dto.setTenantId(tenantId);

        // 1. Passage du résultat comptable au résultat fiscal
        BigDecimal resultatComptable = (cpc.getResultatAvantImpots() != null) ? cpc.getResultatAvantImpots() : BigDecimal.ZERO;
        BigDecimal reint = (reintegrations != null) ? reintegrations : BigDecimal.ZERO;
        BigDecimal ded = (deductions != null) ? deductions : BigDecimal.ZERO;
        BigDecimal resultatFiscal = resultatComptable.add(reint).subtract(ded);

        dto.setResultatComptableAvantImpot(resultatComptable);
        dto.setReintegrationsFiscales(reint);
        dto.setDeductionsFiscales(ded);
        dto.setResultatFiscal(resultatFiscal);

        // 2. Barème progressif marocain (Tranches réelles)
        BigDecimal baseImposable = resultatFiscal.compareTo(BigDecimal.ZERO) > 0 ? resultatFiscal : BigDecimal.ZERO;
        BigDecimal totalIsBareme = BigDecimal.ZERO;
        List<CalculIsDTO.TrancheDetailDTO> tranches = new ArrayList<>();

        // Tranche 1 : 0 à seuilTranche1
        BigDecimal baseT1 = baseImposable.min(regle.getSeuilTranche1());
        BigDecimal impotT1 = baseT1.multiply(regle.getTauxTranche1()).setScale(2, RoundingMode.HALF_UP);
        tranches.add(new CalculIsDTO.TrancheDetailDTO("0 à " + regle.getSeuilTranche1().toPlainString() + " MAD",
                baseT1, regle.getTauxTranche1().multiply(new BigDecimal("100")), impotT1));
        totalIsBareme = totalIsBareme.add(impotT1);

        // Tranche 2 : seuilTranche1 à seuilTranche2
        if (baseImposable.compareTo(regle.getSeuilTranche1()) > 0) {
            BigDecimal baseT2 = baseImposable.min(regle.getSeuilTranche2()).subtract(regle.getSeuilTranche1());
            BigDecimal impotT2 = baseT2.multiply(regle.getTauxTranche2()).setScale(2, RoundingMode.HALF_UP);
            tranches.add(new CalculIsDTO.TrancheDetailDTO(regle.getSeuilTranche1().toPlainString() + " à " + regle.getSeuilTranche2().toPlainString() + " MAD",
                    baseT2, regle.getTauxTranche2().multiply(new BigDecimal("100")), impotT2));
            totalIsBareme = totalIsBareme.add(impotT2);
        } else {
            tranches.add(new CalculIsDTO.TrancheDetailDTO(regle.getSeuilTranche1().toPlainString() + " à " + regle.getSeuilTranche2().toPlainString() + " MAD",
                    BigDecimal.ZERO, regle.getTauxTranche2().multiply(new BigDecimal("100")), BigDecimal.ZERO));
        }

        // Tranche 3 : Au-delà de seuilTranche2
        if (baseImposable.compareTo(regle.getSeuilTranche2()) > 0) {
            BigDecimal baseT3 = baseImposable.subtract(regle.getSeuilTranche2());
            BigDecimal impotT3 = baseT3.multiply(regle.getTauxTranche3()).setScale(2, RoundingMode.HALF_UP);
            tranches.add(new CalculIsDTO.TrancheDetailDTO("Au-delà de " + regle.getSeuilTranche2().toPlainString() + " MAD",
                    baseT3, regle.getTauxTranche3().multiply(new BigDecimal("100")), impotT3));
            totalIsBareme = totalIsBareme.add(impotT3);
        } else {
            tranches.add(new CalculIsDTO.TrancheDetailDTO("Au-delà de " + regle.getSeuilTranche2().toPlainString() + " MAD",
                    BigDecimal.ZERO, regle.getTauxTranche3().multiply(new BigDecimal("100")), BigDecimal.ZERO));
        }

        dto.setIsCalculeBareme(totalIsBareme);
        dto.setDetailTranches(tranches);

        // 3. Calcul de la Cotisation Minimale (CM) sur les produits réels
        BigDecimal baseCotisation = BigDecimal.ZERO;
        if (cpc.getProduitsExploitation() != null && cpc.getProduitsExploitation().getLignes() != null) {
            for (CpcOfficielDTO.LigneCpcDTO l : cpc.getProduitsExploitation().getLignes()) {
                if ("711".equals(l.getCode()) || "712".equals(l.getCode()) || "714".equals(l.getCode()) || "716".equals(l.getCode()) || "718".equals(l.getCode())) {
                    baseCotisation = baseCotisation.add(l.getMontant());
                }
            }
        }
        if (cpc.getProduitsFinanciers() != null && cpc.getProduitsFinanciers().getLignes() != null) {
            for (CpcOfficielDTO.LigneCpcDTO l : cpc.getProduitsFinanciers().getLignes()) {
                if (!"739".equals(l.getCode())) {
                    baseCotisation = baseCotisation.add(l.getMontant());
                }
            }
        }
        if (cpc.getProduitsNonCourants() != null && cpc.getProduitsNonCourants().getLignes() != null) {
            for (CpcOfficielDTO.LigneCpcDTO l : cpc.getProduitsNonCourants().getLignes()) {
                if ("756".equals(l.getCode()) || "758".equals(l.getCode())) {
                    baseCotisation = baseCotisation.add(l.getMontant());
                }
            }
        }

        BigDecimal cmCalculee = baseCotisation.multiply(regle.getTauxCotisationMinimale()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cmRetenue = cmCalculee.max(regle.getPlancherCotisationMinimale());

        dto.setBaseCotisationMinimale(baseCotisation);
        dto.setTauxCotisationMinimale(regle.getTauxCotisationMinimale().multiply(new BigDecimal("100")));
        dto.setCotisationMinimaleCalculee(cmCalculee);
        dto.setPlancherCotisationMinimale(regle.getPlancherCotisationMinimale());
        dto.setCotisationMinimaleRetenue(cmRetenue);

        // 4. Détermination de l'impôt exigible
        BigDecimal impotFinal;
        if (totalIsBareme.compareTo(cmRetenue) >= 0) {
            impotFinal = totalIsBareme;
            dto.setNatureImpotRetenu("IS_BAREME");
            dto.setCreditCotisationMinimale(BigDecimal.ZERO);
        } else {
            impotFinal = cmRetenue;
            dto.setNatureImpotRetenu("COTISATION_MINIMALE");
            dto.setCreditCotisationMinimale(cmRetenue.subtract(totalIsBareme));
        }
        dto.setImpotExigible(impotFinal);

        // 5. Rapprochement avec les acomptes versés réels (Compte 3453 dans la balance)
        List<BalanceCompteDTO> balance = comptabiliteService.getBalance(dateDebut, dateFin);
        BigDecimal acomptesVerses = balance.stream()
                .filter(b -> b.getNumeroCompte().startsWith("3453"))
                .map(b -> {
                    BigDecimal deb = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal cred = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return deb.subtract(cred);
                })
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setAcomptesVerses(acomptesVerses);
        if (impotFinal.compareTo(acomptesVerses) >= 0) {
            dto.setReliquatAPayer(impotFinal.subtract(acomptesVerses));
            dto.setExcedentVersement(BigDecimal.ZERO);
        } else {
            dto.setReliquatAPayer(BigDecimal.ZERO);
            dto.setExcedentVersement(acomptesVerses.subtract(impotFinal));
        }

        return dto;
    }

    // =========================================================================
    // ÉCHÉANCIER DES 4 ACOMPTES TRIMESTRIELS & DU RELIQUAT
    // =========================================================================

    @Transactional(readOnly = true)
    public EcheancierIsDTO genererEcheancierAcomptes(int anneeFiscale, BigDecimal impotReferenceManuel) {
        Long tenantId = getTenantId();

        BigDecimal impRef;
        if (impotReferenceManuel != null && impotReferenceManuel.compareTo(BigDecimal.ZERO) > 0) {
            impRef = impotReferenceManuel;
        } else {
            // Calcul réel depuis l'exercice précédent N-1
            CalculIsDTO isAnneePrecedente = calculerIs(anneeFiscale - 1, BigDecimal.ZERO, BigDecimal.ZERO);
            impRef = (isAnneePrecedente.getImpotExigible() != null && isAnneePrecedente.getImpotExigible().compareTo(BigDecimal.ZERO) > 0)
                    ? isAnneePrecedente.getImpotExigible()
                    : new BigDecimal("12000.00"); // Minimum si premier exercice
        }

        EcheancierIsDTO dto = new EcheancierIsDTO();
        dto.setAnneeFiscale(anneeFiscale);
        dto.setTenantId(tenantId);
        dto.setImpotReference(impRef);

        BigDecimal quart = impRef.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        dto.setTotalAcomptesDus(quart.multiply(new BigDecimal("4")));

        LocalDate[] datesLimites = {
            LocalDate.of(anneeFiscale, 3, 31),
            LocalDate.of(anneeFiscale, 6, 30),
            LocalDate.of(anneeFiscale, 9, 30),
            LocalDate.of(anneeFiscale, 12, 31)
        };

        LocalDate now = LocalDate.now();
        BigDecimal totalPaye = BigDecimal.ZERO;

        for (int i = 0; i < 4; i++) {
            int num = i + 1;
            LocalDate dateLim = datesLimites[i];
            LocalDate startQ = (i == 0) ? LocalDate.of(anneeFiscale, 1, 1) : datesLimites[i - 1].plusDays(1);

            // Recherche des paiements réels du trimestre sur le compte 3453
            List<BalanceCompteDTO> balQ = comptabiliteService.getBalance(startQ, dateLim);
            BigDecimal payeTrimestre = balQ.stream()
                    .filter(b -> b.getNumeroCompte().startsWith("3453"))
                    .map(b -> b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalPaye = totalPaye.add(payeTrimestre);

            String statut;
            if (payeTrimestre.compareTo(quart) >= 0) {
                statut = "REGLE";
            } else if (payeTrimestre.compareTo(BigDecimal.ZERO) > 0) {
                statut = "PARTIEL";
            } else if (now.isAfter(dateLim)) {
                statut = "EN_RETARD";
            } else {
                statut = "A_PAYER";
            }

            dto.getAcomptes().add(new EcheancierIsDTO.AcompteItemDTO(
                    num,
                    num + (num == 1 ? "er" : "ème") + " Acompte IS (" + anneeFiscale + ")",
                    new BigDecimal("25.00"),
                    dateLim,
                    quart,
                    payeTrimestre,
                    statut
            ));
        }

        dto.setTotalAcomptesVerses(totalPaye);
        dto.setSoldeRestantAcomptes(dto.getTotalAcomptesDus().subtract(totalPaye).max(BigDecimal.ZERO));

        // Reliquat au 31 Mars N+1
        LocalDate dateReliquat = LocalDate.of(anneeFiscale + 1, 3, 31);
        dto.setDateLimiteReliquat(dateReliquat);

        CalculIsDTO isReelAnnee = calculerIs(anneeFiscale, BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal reliquat = isReelAnnee.getImpotExigible().subtract(totalPaye);
        if (reliquat.compareTo(BigDecimal.ZERO) > 0) {
            dto.setMontantReliquat(reliquat);
            dto.setStatutReliquat(now.isAfter(dateReliquat) ? "EN_RETARD" : "A_PAYER");
        } else if (reliquat.compareTo(BigDecimal.ZERO) == 0) {
            dto.setMontantReliquat(BigDecimal.ZERO);
            dto.setStatutReliquat("REGLE");
        } else {
            dto.setMontantReliquat(reliquat.abs());
            dto.setStatutReliquat("EXCEDENT_A_IMPUTER");
        }

        return dto;
    }

    // =========================================================================
    // CALENDRIER FISCAL DGI COMPLET (TVA, ACOMPTES IS, CNSS, LIASSE)
    // =========================================================================

    @Transactional(readOnly = true)
    public CalendrierFiscalDTO getCalendrierFiscal(int annee) {
        Long tenantId = getTenantId();
        CalendrierFiscalDTO calendrier = new CalendrierFiscalDTO(annee, tenantId);
        LocalDate today = LocalDate.now();

        // 1. TVA Mensuelle (échéance le 20 du mois suivant)
        for (int m = 1; m <= 12; m++) {
            LocalDate startM = LocalDate.of(annee, m, 1);
            LocalDate endM = startM.plusMonths(1).minusDays(1);
            LocalDate deadline = (m == 12) ? LocalDate.of(annee + 1, 1, 20) : LocalDate.of(annee, m + 1, 20);

            // Calcul réel de la TVA due du mois
            DeclarationTvaDTO tva = comptabiliteService.getDeclarationTva(startM, endM);
            BigDecimal tvaDue = (tva.getTvaAPayer() != null && tva.getTvaAPayer().compareTo(BigDecimal.ZERO) > 0) ? tva.getTvaAPayer() : BigDecimal.ZERO;

            long jours = ChronoUnit.DAYS.between(today, deadline);
            String statut;
            if (jours < 0) {
                statut = "EN_RETARD";
            } else if (jours <= 7) {
                statut = "URGENT";
            } else {
                statut = "A_ECHOIR";
            }

            calendrier.getEcheances().add(new CalendrierFiscalDTO.EcheanceFiscaleDTO(
                    "TVA-" + annee + "-" + String.format("%02d", m),
                    "Déclaration TVA Mensuelle (" + startM.getMonth().name() + " " + annee + ")",
                    "TVA",
                    "MENSUEL",
                    deadline,
                    tvaDue,
                    BigDecimal.ZERO,
                    statut,
                    jours
            ));
        }

        // 2. Acomptes provisionnels IS (31/03, 30/06, 30/09, 31/12)
        EcheancierIsDTO echeancierIs = genererEcheancierAcomptes(annee, null);
        for (EcheancierIsDTO.AcompteItemDTO ac : echeancierIs.getAcomptes()) {
            long jours = ChronoUnit.DAYS.between(today, ac.getDateLimite());
            String statut = ac.getStatut();
            if ("A_PAYER".equals(statut) && jours <= 7 && jours >= 0) {
                statut = "URGENT";
            }

            calendrier.getEcheances().add(new CalendrierFiscalDTO.EcheanceFiscaleDTO(
                    "IS-ACOMPTE-" + ac.getNumero() + "-" + annee,
                    ac.getLibelle(),
                    "IS",
                    "TRIMESTRIEL",
                    ac.getDateLimite(),
                    ac.getMontantDu(),
                    ac.getMontantRegle(),
                    statut,
                    jours
            ));
        }

        // 3. Reliquat IS & Dépôt Liasse Fiscale (31 Mars N+1)
        LocalDate dateLiasse = LocalDate.of(annee + 1, 3, 31);
        long joursLiasse = ChronoUnit.DAYS.between(today, dateLiasse);
        calendrier.getEcheances().add(new CalendrierFiscalDTO.EcheanceFiscaleDTO(
                "LIASSE-IS-" + annee,
                "Dépôt Liasse Fiscale DGI & Reliquat IS (" + annee + ")",
                "LIASSE",
                "ANNUEL",
                dateLiasse,
                echeancierIs.getMontantReliquat(),
                BigDecimal.ZERO,
                joursLiasse < 0 ? "EN_RETARD" : (joursLiasse <= 15 ? "URGENT" : "A_ECHOIR"),
                joursLiasse
        ));

        // Tri chronologique des échéances
        calendrier.getEcheances().sort(Comparator.comparing(CalendrierFiscalDTO.EcheanceFiscaleDTO::getDateLimite));

        return calendrier;
    }
}
