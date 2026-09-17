package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.SensCompte;
import com.gestion.persistent.enums.StatutExercice;
import com.gestion.persistent.enums.TypeJournal;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClotureExerciceService {

    private final ExerciceComptableRepository exerciceRepository;
    private final EcritureComptableRepository ecritureRepository;
    private final LigneEcritureRepository ligneRepository;
    private final CompteComptableRepository compteRepository;
    private final JournalComptableRepository journalRepository;
    private final ComptabiliteService comptabiliteService;

    public ClotureExerciceService(ExerciceComptableRepository exerciceRepository,
                                  EcritureComptableRepository ecritureRepository,
                                  LigneEcritureRepository ligneRepository,
                                  CompteComptableRepository compteRepository,
                                  JournalComptableRepository journalRepository,
                                  ComptabiliteService comptabiliteService) {
        this.exerciceRepository = exerciceRepository;
        this.ecritureRepository = ecritureRepository;
        this.ligneRepository = ligneRepository;
        this.compteRepository = compteRepository;
        this.journalRepository = journalRepository;
        this.comptabiliteService = comptabiliteService;
    }

    private Long getTenantId() {
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    @Transactional(readOnly = true)
    public List<ExerciceComptableDTO> getExercices() {
        Long tenantId = getTenantId();
        List<ExerciceComptable> list = exerciceRepository.findByPointDeVenteIdOrderByDateDebutDesc(tenantId);
        if (list.isEmpty()) {
            initExerciceParDefaut(tenantId);
            list = exerciceRepository.findByPointDeVenteIdOrderByDateDebutDesc(tenantId);
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ExerciceComptableDTO creerExercice(ExerciceComptableDTO dto) {
        Long tenantId = getTenantId();
        if (dto.getDateDebut() == null || dto.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin d'exercice sont obligatoires.");
        }
        if (dto.getDateDebut().isAfter(dto.getDateFin())) {
            throw new IllegalArgumentException("La date de début ne peut être postérieure à la date de fin.");
        }

        ExerciceComptable ex = new ExerciceComptable();
        ex.setCode(dto.getCode() != null ? dto.getCode().trim() : "EX-" + dto.getDateDebut().getYear());
        ex.setLibelle(dto.getLibelle() != null ? dto.getLibelle().trim() : "Exercice " + dto.getDateDebut().getYear());
        ex.setDateDebut(dto.getDateDebut());
        ex.setDateFin(dto.getDateFin());
        ex.setStatut(StatutExercice.OUVERT);
        ex.setPointDeVenteId(tenantId);

        return toDto(exerciceRepository.save(ex));
    }

    private void initExerciceParDefaut(Long tenantId) {
        int year = LocalDate.now().getYear();
        ExerciceComptable ex = new ExerciceComptable(
                "EX-" + year,
                "Exercice Comptable " + year,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31),
                tenantId
        );
        exerciceRepository.save(ex);
    }

    @Transactional(readOnly = true)
    public CloturePreviewDTO preparerCloture(Long exerciceId) {
        Long tenantId = getTenantId();
        ExerciceComptable ex = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new IllegalArgumentException("Exercice introuvable : " + exerciceId));

        CloturePreviewDTO preview = new CloturePreviewDTO();
        preview.setExerciceId(ex.getId());
        preview.setExerciceCode(ex.getCode());
        preview.setDateDebut(ex.getDateDebut());
        preview.setDateFin(ex.getDateFin());

        List<EcritureComptable> ecritures = ecritureRepository.findByPointDeVenteIdAndDateEcritureBetweenOrderByDateEcritureAsc(
                tenantId, ex.getDateDebut(), ex.getDateFin());

        long brouillons = ecritures.stream().filter(e -> !Boolean.TRUE.equals(e.getValidee())).count();
        long validees = ecritures.stream().filter(e -> Boolean.TRUE.equals(e.getValidee())).count();

        preview.setNombreEcrituresBrouillons((int) brouillons);
        preview.setNombreEcrituresValidees((int) validees);

        List<BalanceCompteDTO> balance = comptabiliteService.getBalance(ex.getDateDebut(), ex.getDateFin());
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalProduits = BigDecimal.ZERO;

        for (BalanceCompteDTO b : balance) {
            BigDecimal deb = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
            BigDecimal cred = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
            totalDebit = totalDebit.add(deb);
            totalCredit = totalCredit.add(cred);

            if (b.getNumeroCompte().startsWith("6")) {
                totalCharges = totalCharges.add(deb.subtract(cred));
            } else if (b.getNumeroCompte().startsWith("7")) {
                totalProduits = totalProduits.add(cred.subtract(deb));
            }
        }

        preview.setTotalDebitBalance(totalDebit);
        preview.setTotalCreditBalance(totalCredit);
        BigDecimal ecartBalance = totalDebit.subtract(totalCredit).abs();
        preview.setEcartBalance(ecartBalance);

        preview.setTotalChargesClasse6(totalCharges);
        preview.setTotalProduitsClasse7(totalProduits);
        BigDecimal resultat = totalProduits.subtract(totalCharges);
        preview.setResultatNetEstime(resultat);

        if (resultat.compareTo(BigDecimal.ZERO) >= 0) {
            preview.setTypeResultat("BENEFICE");
            preview.setCompteResultat("1191 - Résultat net de l'exercice (Solde créditeur)");
        } else {
            preview.setTypeResultat("PERTE");
            preview.setCompteResultat("1199 - Résultat net de l'exercice (Solde débiteur)");
        }

        boolean eligible = true;
        List<String> motifs = new ArrayList<>();

        if (ex.getStatut() == StatutExercice.CLOTURE) {
            eligible = false;
            motifs.add("Cet exercice est déjà définitivement clôturé.");
        }
        if (brouillons > 0) {
            eligible = false;
            motifs.add("Il reste " + brouillons + " écriture(s) en brouillon. Toutes les écritures doivent être validées avant clôture.");
        }
        if (ecartBalance.compareTo(new BigDecimal("0.05")) > 0) {
            eligible = false;
            motifs.add("La balance générale est déséquilibrée (Écart = " + ecartBalance + " MAD).");
        }

        preview.setEligible(eligible);
        preview.setMotifsIneligibilite(motifs);
        return preview;
    }

    public ExerciceComptableDTO executerCloture(Long exerciceId, String cloturePar) {
        Long tenantId = getTenantId();
        ExerciceComptable ex = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new IllegalArgumentException("Exercice introuvable : " + exerciceId));

        CloturePreviewDTO preview = preparerCloture(exerciceId);
        if (!preview.isEligible()) {
            throw new IllegalStateException("Clôture impossible : " + String.join(", ", preview.getMotifsIneligibilite()));
        }

        BigDecimal resultatNet = preview.getResultatNetEstime();

        // 1. SOLDE DES COMPTES DE GESTION (CLASSES 6 ET 7) DANS LE JOURNAL OD
        JournalComptable journalOD = journalRepository.findByCodeAndPointDeVenteId("OD", tenantId)
                .orElseThrow(() -> new IllegalStateException("Journal des Opérations Diverses (OD) introuvable."));

        EcritureComptable ecritureSoldeGestion = new EcritureComptable();
        ecritureSoldeGestion.setJournal(journalOD);
        ecritureSoldeGestion.setDateEcriture(ex.getDateFin());
        ecritureSoldeGestion.setLibelle("Clôture comptes de gestion - Résultat " + ex.getCode());
        ecritureSoldeGestion.setNumeroPiece("CLOT-" + ex.getCode());
        ecritureSoldeGestion.setReferencePiece(ex.getCode());
        ecritureSoldeGestion.setPointDeVenteId(tenantId);
        ecritureSoldeGestion.setValidee(true);

        List<BalanceCompteDTO> balance = comptabiliteService.getBalance(ex.getDateDebut(), ex.getDateFin());

        for (BalanceCompteDTO b : balance) {
            String num = b.getNumeroCompte();
            BigDecimal deb = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
            BigDecimal cred = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;

            if (num.startsWith("6")) {
                BigDecimal soldeDeb = deb.subtract(cred);
                if (soldeDeb.compareTo(BigDecimal.ZERO) != 0) {
                    CompteComptable c = getOrCreateCompte(num, b.getLibelleCompte(), 6, tenantId);
                    LigneEcriture ligne = new LigneEcriture();
                    ligne.setCompte(c);
                    ligne.setPointDeVenteId(tenantId);
                    ligne.setLibelleLigne("Solde charge fin exercice");
                    if (soldeDeb.compareTo(BigDecimal.ZERO) > 0) {
                        ligne.setCredit(soldeDeb);
                        ligne.setDebit(BigDecimal.ZERO);
                    } else {
                        ligne.setDebit(soldeDeb.abs());
                        ligne.setCredit(BigDecimal.ZERO);
                    }
                    ecritureSoldeGestion.addLigne(ligne);
                }
            } else if (num.startsWith("7")) {
                BigDecimal soldeCred = cred.subtract(deb);
                if (soldeCred.compareTo(BigDecimal.ZERO) != 0) {
                    CompteComptable c = getOrCreateCompte(num, b.getLibelleCompte(), 7, tenantId);
                    LigneEcriture ligne = new LigneEcriture();
                    ligne.setCompte(c);
                    ligne.setPointDeVenteId(tenantId);
                    ligne.setLibelleLigne("Solde produit fin exercice");
                    if (soldeCred.compareTo(BigDecimal.ZERO) > 0) {
                        ligne.setDebit(soldeCred);
                        ligne.setCredit(BigDecimal.ZERO);
                    } else {
                        ligne.setCredit(soldeCred.abs());
                        ligne.setDebit(BigDecimal.ZERO);
                    }
                    ecritureSoldeGestion.addLigne(ligne);
                }
            }
        }

        // Affecter le solde au compte de résultat 1191 ou 1199
        String numCompteResultat = (resultatNet.compareTo(BigDecimal.ZERO) >= 0) ? "11910000" : "11990000";
        String libCompteResultat = (resultatNet.compareTo(BigDecimal.ZERO) >= 0) ? "Résultat net de l'exercice (Solde créditeur)" : "Résultat net de l'exercice (Solde débiteur)";
        CompteComptable cResultat = getOrCreateCompte(numCompteResultat, libCompteResultat, 1, tenantId);

        LigneEcriture ligneRes = new LigneEcriture();
        ligneRes.setCompte(cResultat);
        ligneRes.setPointDeVenteId(tenantId);
        ligneRes.setLibelleLigne("Résultat net " + ex.getCode());
        if (resultatNet.compareTo(BigDecimal.ZERO) >= 0) {
            ligneRes.setCredit(resultatNet);
            ligneRes.setDebit(BigDecimal.ZERO);
        } else {
            ligneRes.setDebit(resultatNet.abs());
            ligneRes.setCredit(BigDecimal.ZERO);
        }
        ecritureSoldeGestion.addLigne(ligneRes);

        if (ecritureSoldeGestion.getLignes().size() >= 2 && ecritureSoldeGestion.isEquilibree()) {
            ecritureRepository.save(ecritureSoldeGestion);
        }

        // 2. CRÉATION DU NOUVEL EXERCICE N+1
        LocalDate dateDebutSuivant = ex.getDateFin().plusDays(1);
        LocalDate dateFinSuivant = dateDebutSuivant.plusYears(1).minusDays(1);
        int yearSuivant = dateDebutSuivant.getYear();
        String codeSuivant = "EX-" + yearSuivant;

        ExerciceComptable exSuivant = exerciceRepository.findByCodeAndPointDeVenteId(codeSuivant, tenantId)
                .orElseGet(() -> {
                    ExerciceComptable n = new ExerciceComptable(
                            codeSuivant,
                            "Exercice Comptable " + yearSuivant,
                            dateDebutSuivant,
                            dateFinSuivant,
                            tenantId
                    );
                    return exerciceRepository.save(n);
                });

        // 3. ÉCRITURE D'A-NOUVEAUX (JOURNAL 'AN') AU PREMIER JOUR DE L'EXERCICE SUIVANT
        JournalComptable journalAN = journalRepository.findByCodeAndPointDeVenteId("AN", tenantId)
                .orElseGet(() -> {
                    JournalComptable j = new JournalComptable("AN", "Journal des A-Nouveaux", TypeJournal.A_NOUVEAUX, tenantId);
                    return journalRepository.save(j);
                });

        EcritureComptable ecritureAN = new EcritureComptable();
        ecritureAN.setJournal(journalAN);
        ecritureAN.setDateEcriture(dateDebutSuivant);
        ecritureAN.setLibelle("Reprise des A-Nouveaux " + ex.getCode());
        ecritureAN.setNumeroPiece("RAN-" + yearSuivant);
        ecritureAN.setReferencePiece("REPORT-" + ex.getCode());
        ecritureAN.setPointDeVenteId(tenantId);
        ecritureAN.setValidee(true);

        // Recalculer la balance des comptes de bilan (classes 1 à 5) en tenant compte de l'écriture de solde
        List<BalanceCompteDTO> balanceBilan = comptabiliteService.getBalance(LocalDate.of(1900, 1, 1), ex.getDateFin());

        for (BalanceCompteDTO b : balanceBilan) {
            String num = b.getNumeroCompte();
            int classe = Integer.parseInt(num.substring(0, 1));
            if (classe >= 1 && classe <= 5) {
                BigDecimal deb = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                BigDecimal cred = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                BigDecimal solde = deb.subtract(cred);

                if (solde.compareTo(BigDecimal.ZERO) != 0) {
                    CompteComptable c = getOrCreateCompte(num, b.getLibelleCompte(), classe, tenantId);
                    LigneEcriture lAN = new LigneEcriture();
                    lAN.setCompte(c);
                    lAN.setPointDeVenteId(tenantId);
                    lAN.setLibelleLigne("Report à nouveau " + c.getLibelle());
                    if (solde.compareTo(BigDecimal.ZERO) > 0) {
                        lAN.setDebit(solde);
                        lAN.setCredit(BigDecimal.ZERO);
                    } else {
                        lAN.setDebit(BigDecimal.ZERO);
                        lAN.setCredit(solde.abs());
                    }
                    ecritureAN.addLigne(lAN);
                }
            }
        }

        if (ecritureAN.getLignes().size() >= 2 && ecritureAN.isEquilibree()) {
            ecritureRepository.save(ecritureAN);
        }

        // 4. VERROUILLAGE OFFICIEL DE L'EXERCICE
        ex.setStatut(StatutExercice.CLOTURE);
        ex.setResultatNet(resultatNet);
        ex.setDateCloture(LocalDateTime.now());
        ex.setCloturePar(cloturePar != null ? cloturePar : "Système");

        return toDto(exerciceRepository.save(ex));
    }

    private CompteComptable getOrCreateCompte(String numero, String libelle, int classe, Long tenantId) {
        return compteRepository.findByNumeroCompteAndPointDeVenteId(numero.trim(), tenantId)
                .orElseGet(() -> {
                    CompteComptable c = new CompteComptable();
                    c.setNumeroCompte(numero.trim());
                    c.setLibelle(libelle != null ? libelle : "Compte " + numero);
                    c.setClasse(classe);
                    c.setPointDeVenteId(tenantId);
                    c.setSensParDefaut(SensCompte.DEBIT);
                    c.setActif(true);
                    return compteRepository.save(c);
                });
    }

    private ExerciceComptableDTO toDto(ExerciceComptable e) {
        ExerciceComptableDTO dto = new ExerciceComptableDTO();
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setLibelle(e.getLibelle());
        dto.setDateDebut(e.getDateDebut());
        dto.setDateFin(e.getDateFin());
        dto.setStatut(e.getStatut());
        dto.setResultatNet(e.getResultatNet());
        dto.setDateCloture(e.getDateCloture());
        dto.setCloturePar(e.getCloturePar());
        return dto;
    }
}
