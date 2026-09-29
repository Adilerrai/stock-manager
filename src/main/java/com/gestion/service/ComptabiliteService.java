package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.SensCompte;
import com.gestion.persistent.enums.TypeJournal;
import com.gestion.persistent.model.*;
import com.gestion.repository.CompteComptableRepository;
import com.gestion.repository.EcritureComptableRepository;
import com.gestion.repository.ExerciceComptableRepository;
import com.gestion.repository.JournalComptableRepository;
import com.gestion.repository.LigneEcritureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ComptabiliteService {

    private final CompteComptableRepository compteRepository;
    private final JournalComptableRepository journalRepository;
    private final EcritureComptableRepository ecritureRepository;
    private final LigneEcritureRepository ligneRepository;
    private final ExerciceComptableRepository exerciceRepository;
    private final AuditService auditService;

    public ComptabiliteService(CompteComptableRepository compteRepository,
                               JournalComptableRepository journalRepository,
                               EcritureComptableRepository ecritureRepository,
                               LigneEcritureRepository ligneRepository,
                               ExerciceComptableRepository exerciceRepository,
                               AuditService auditService) {
        this.compteRepository = compteRepository;
        this.journalRepository = journalRepository;
        this.ecritureRepository = ecritureRepository;
        this.ligneRepository = ligneRepository;
        this.exerciceRepository = exerciceRepository;
        this.auditService = auditService;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    // =========================================================================
    // PLAN COMPTABLE
    // =========================================================================

    @Transactional
    public List<CompteComptableDTO> getPlanComptable() {
        Long tenantId = getTenantId();
        List<CompteComptable> comptes = compteRepository.findByPointDeVenteIdOrderByNumeroCompteAsc(tenantId);
        if (comptes.isEmpty()) {
            initPlanComptableParDefaut(tenantId);
            comptes = compteRepository.findByPointDeVenteIdOrderByNumeroCompteAsc(tenantId);
        }
        return comptes.stream().map(this::toCompteDto).collect(Collectors.toList());
    }

    public CompteComptableDTO creerCompte(CompteComptableDTO dto) {
        Long tenantId = getTenantId();
        if (dto.getNumeroCompte() == null || dto.getNumeroCompte().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de compte est obligatoire");
        }
        if (compteRepository.existsByNumeroCompteAndPointDeVenteId(dto.getNumeroCompte().trim(), tenantId)) {
            throw new IllegalStateException("Le compte numéro " + dto.getNumeroCompte() + " existe déjà");
        }

        CompteComptable compte = new CompteComptable();
        compte.setNumeroCompte(dto.getNumeroCompte().trim());
        compte.setLibelle(dto.getLibelle());
        compte.setClasse(dto.getClasse() != null ? dto.getClasse() : determinerClasse(dto.getNumeroCompte()));
        compte.setSensParDefaut(dto.getSensParDefaut() != null ? dto.getSensParDefaut() : SensCompte.DEBIT);
        compte.setActif(dto.getActif() != null ? dto.getActif() : true);
        compte.setPointDeVenteId(tenantId);
        compte.setDateCreation(LocalDateTime.now());

        return toCompteDto(compteRepository.save(compte));
    }

    private Integer determinerClasse(String numeroCompte) {
        if (numeroCompte != null && !numeroCompte.isEmpty()) {
            char premierChiffre = numeroCompte.charAt(0);
            if (premierChiffre >= '1' && premierChiffre <= '8') {
                return Character.getNumericValue(premierChiffre);
            }
        }
        return 1;
    }

    @Transactional
    public void initPlanComptableParDefaut(Long tenantId) {
        // Plan comptable général marocain / maghrébin standard (PCGM)
        List<Object[]> standardAccounts = List.of(
            // Classe 1 : Financement permanent
            new Object[]{"11110000", "Capital social", 1, SensCompte.CREDIT},
            new Object[]{"11910000", "Résultat net de l'exercice (Solde créditeur)", 1, SensCompte.CREDIT},
            new Object[]{"11990000", "Résultat net de l'exercice (Solde débiteur)", 1, SensCompte.DEBIT},
            new Object[]{"14810000", "Emprunts auprès des établissements de crédit", 1, SensCompte.CREDIT},

            // Classe 2 : Actif immobilisé
            new Object[]{"21110000", "Frais de constitution", 2, SensCompte.DEBIT},
            new Object[]{"23320000", "Matériel et outillage", 2, SensCompte.DEBIT},
            new Object[]{"23400000", "Matériel de transport", 2, SensCompte.DEBIT},
            new Object[]{"23510000", "Mobilier de bureau", 2, SensCompte.DEBIT},
            new Object[]{"23550000", "Matériel informatique", 2, SensCompte.DEBIT},
            new Object[]{"28330000", "Amortissements des installations techniques", 2, SensCompte.CREDIT},

            // Classe 3 : Actif circulant (hors trésorerie)
            new Object[]{"31110000", "Marchandises", 3, SensCompte.DEBIT},
            new Object[]{"31210000", "Matières premières", 3, SensCompte.DEBIT},
            new Object[]{"34210000", "Clients", 3, SensCompte.DEBIT},
            new Object[]{"34250000", "Clients - Effets à recevoir", 3, SensCompte.DEBIT},
            new Object[]{"34270000", "Clients - Factures à établir", 3, SensCompte.DEBIT},
            new Object[]{"34550000", "État - TVA récupérable", 3, SensCompte.DEBIT},
            new Object[]{"34551000", "État - TVA récupérable sur charges", 3, SensCompte.DEBIT},
            new Object[]{"34552000", "État - TVA récupérable sur immobilisations", 3, SensCompte.DEBIT},

            // Classe 4 : Passif circulant (hors trésorerie)
            new Object[]{"44110000", "Fournisseurs", 4, SensCompte.CREDIT},
            new Object[]{"44150000", "Fournisseurs - Effets à payer", 4, SensCompte.CREDIT},
            new Object[]{"44170000", "Fournisseurs - Factures non parvenues", 4, SensCompte.CREDIT},
            new Object[]{"44550000", "État - TVA facturée / collectée", 4, SensCompte.CREDIT},
            new Object[]{"44560000", "État - TVA due", 4, SensCompte.CREDIT},
            new Object[]{"44320000", "Rémunérations dues au personnel", 4, SensCompte.CREDIT},
            new Object[]{"44410000", "CNSS / Sécurité sociale", 4, SensCompte.CREDIT},

            // Classe 5 : Trésorerie
            new Object[]{"51110000", "Chèques à encaisser", 5, SensCompte.DEBIT},
            new Object[]{"51130000", "Effets à l'encaissement", 5, SensCompte.DEBIT},
            new Object[]{"51410000", "Banque", 5, SensCompte.DEBIT},
            new Object[]{"51610000", "Caisse centrale", 5, SensCompte.DEBIT},
            new Object[]{"55200000", "Crédits de trésorerie / Découverts", 5, SensCompte.CREDIT},

            // Classe 6 : Comptes de charges
            new Object[]{"61110000", "Achats de marchandises", 6, SensCompte.DEBIT},
            new Object[]{"61210000", "Achats de matières premières", 6, SensCompte.DEBIT},
            new Object[]{"61310000", "Locations et charges locatives", 6, SensCompte.DEBIT},
            new Object[]{"61330000", "Entretien et réparations", 6, SensCompte.DEBIT},
            new Object[]{"61410000", "Transports du personnel / marchandises", 6, SensCompte.DEBIT},
            new Object[]{"61450000", "Frais postaux et télécommunications", 6, SensCompte.DEBIT},
            new Object[]{"61470000", "Services bancaires", 6, SensCompte.DEBIT},
            new Object[]{"61710000", "Rémunérations du personnel", 6, SensCompte.DEBIT},
            new Object[]{"61740000", "Charges sociales", 6, SensCompte.DEBIT},
            new Object[]{"61930000", "Dotations d'exploitation aux amortissements", 6, SensCompte.DEBIT},

            // Classe 7 : Comptes de produits
            new Object[]{"71110000", "Ventes de marchandises", 7, SensCompte.CREDIT},
            new Object[]{"71210000", "Ventes de biens et services produits", 7, SensCompte.CREDIT},
            new Object[]{"71240000", "Prestations de services", 7, SensCompte.CREDIT},
            new Object[]{"71970000", "Reprises d'exploitation", 7, SensCompte.CREDIT}
        );

        for (Object[] acc : standardAccounts) {
            String num = (String) acc[0];
            if (!compteRepository.existsByNumeroCompteAndPointDeVenteId(num, tenantId)) {
                CompteComptable c = new CompteComptable(
                    num,
                    (String) acc[1],
                    (Integer) acc[2],
                    (SensCompte) acc[3],
                    tenantId
                );
                compteRepository.save(c);
            }
        }
    }

    // =========================================================================
    // JOURNAUX COMPTABLES
    // =========================================================================

    @Transactional
    public List<JournalComptableDTO> getJournaux() {
        Long tenantId = getTenantId();
        List<JournalComptable> journaux = journalRepository.findByPointDeVenteIdOrderByCodeAsc(tenantId);
        if (journaux.isEmpty()) {
            initJournauxParDefaut(tenantId);
            journaux = journalRepository.findByPointDeVenteIdOrderByCodeAsc(tenantId);
        }

        // Statistiques agrégées par journal en une seule requête SQL performante
        List<Object[]> stats = ligneRepository.getStatsGroupesParJournal(tenantId);
        Map<Long, Object[]> statsMap = new HashMap<>();
        for (Object[] row : stats) {
            if (row != null && row[0] != null) {
                statsMap.put((Long) row[0], row);
            }
        }

        return journaux.stream().map(j -> {
            JournalComptableDTO dto = toJournalDto(j);
            Object[] row = statsMap.get(j.getId());
            if (row != null) {
                dto.setNombreEcritures(row[1] != null ? ((Number) row[1]).longValue() : 0L);
                dto.setTotalDebit(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
                dto.setTotalCredit(row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO);
                if (row[4] != null) {
                    dto.setDerniereEcritureDate((LocalDate) row[4]);
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JournalComptableDTO getJournalById(Long id) {
        Long tenantId = getTenantId();
        JournalComptable journal = journalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Journal introuvable ID: " + id));
        if (!journal.getPointDeVenteId().equals(tenantId)) {
            throw new SecurityException("Accès refusé");
        }
        return toJournalDto(journal);
    }

    public JournalComptableDTO creerJournal(JournalComptableDTO dto) {
        Long tenantId = getTenantId();
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Le code journal est obligatoire (ex: VT, AC, BQ, AN)");
        }
        String code = dto.getCode().trim().toUpperCase();
        if (journalRepository.findByCodeAndPointDeVenteId(code, tenantId).isPresent()) {
            throw new IllegalStateException("Le journal avec le code " + code + " existe déjà");
        }

        JournalComptable journal = new JournalComptable(
            code,
            dto.getLibelle() != null ? dto.getLibelle() : code,
            dto.getTypeJournal() != null ? dto.getTypeJournal() : TypeJournal.OPERATIONS_DIVERSES,
            tenantId
        );
        journal.setActif(dto.getActif() != null ? dto.getActif() : true);

        return toJournalDto(journalRepository.save(journal));
    }

    public JournalComptableDTO modifierJournal(Long id, JournalComptableDTO dto) {
        Long tenantId = getTenantId();
        JournalComptable journal = journalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Journal introuvable ID: " + id));

        if (!journal.getPointDeVenteId().equals(tenantId)) {
            throw new SecurityException("Accès refusé");
        }

        if (dto.getLibelle() != null && !dto.getLibelle().trim().isEmpty()) {
            journal.setLibelle(dto.getLibelle().trim());
        }
        if (dto.getActif() != null) {
            journal.setActif(dto.getActif());
        }
        if (dto.getTypeJournal() != null) {
            journal.setTypeJournal(dto.getTypeJournal());
        }

        return toJournalDto(journalRepository.save(journal));
    }

    public JournalComptableDTO toggleActifJournal(Long id) {
        Long tenantId = getTenantId();
        JournalComptable journal = journalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Journal introuvable ID: " + id));

        if (!journal.getPointDeVenteId().equals(tenantId)) {
            throw new SecurityException("Accès refusé");
        }

        journal.setActif(!Boolean.TRUE.equals(journal.getActif()));
        return toJournalDto(journalRepository.save(journal));
    }

    public void supprimerJournal(Long id) {
        Long tenantId = getTenantId();
        JournalComptable journal = journalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Journal introuvable ID: " + id));

        if (!journal.getPointDeVenteId().equals(tenantId)) {
            throw new SecurityException("Accès refusé");
        }

        long count = ecritureRepository.countByPointDeVenteIdAndJournal(tenantId, journal);
        if (count > 0) {
            throw new IllegalStateException("Impossible de supprimer le journal '" + journal.getCode() +
                "' car il contient " + count + " écriture(s). Conformément aux règles du PCGM (intangibilité des comptes), veuillez plutôt le désactiver.");
        }

        journalRepository.delete(journal);
    }

    @Transactional
    public void initJournauxParDefaut(Long tenantId) {
        List<JournalComptable> defauts = List.of(
            new JournalComptable("VT", "Journal des Ventes", TypeJournal.VENTES, tenantId),
            new JournalComptable("AC", "Journal des Achats", TypeJournal.ACHATS, tenantId),
            new JournalComptable("BQ", "Journal de Banque", TypeJournal.BANQUE, tenantId),
            new JournalComptable("CA", "Journal de Caisse", TypeJournal.CAISSE, tenantId),
            new JournalComptable("OD", "Journal des Opérations Diverses", TypeJournal.OPERATIONS_DIVERSES, tenantId),
            new JournalComptable("AN", "Journal des À-Nouveaux (Bilan d'ouverture)", TypeJournal.A_NOUVEAUX, tenantId)
        );

        for (JournalComptable j : defauts) {
            if (journalRepository.findByCodeAndPointDeVenteId(j.getCode(), tenantId).isEmpty()) {
                journalRepository.save(j);
            }
        }
    }

    // =========================================================================
    // ÉCRITURES COMPTABLES
    // =========================================================================

    @Transactional(readOnly = true)
    public List<EcritureComptableDTO> getEcritures(Long journalId, LocalDate debut, LocalDate fin) {
        Long tenantId = getTenantId();
        List<EcritureComptable> ecritures;

        if (journalId != null) {
            JournalComptable journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable"));
            ecritures = ecritureRepository.findByPointDeVenteIdAndJournalOrderByDateEcritureDesc(tenantId, journal);
        } else if (debut != null && fin != null) {
            ecritures = ecritureRepository.findByPointDeVenteIdAndDateEcritureBetweenOrderByDateEcritureAsc(tenantId, debut, fin);
        } else {
            ecritures = ecritureRepository.findByPointDeVenteIdOrderByDateEcritureDescIdDesc(tenantId);
        }

        return ecritures.stream().map(this::toEcritureDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EcritureComptableDTO getEcritureById(Long id) {
        EcritureComptable ecriture = ecritureRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Écriture comptable introuvable: " + id));
        return toEcritureDto(ecriture);
    }

    public EcritureComptableDTO creerEcriture(EcritureComptableDTO dto) {
        Long tenantId = getTenantId();

        if (dto.getJournalId() == null && dto.getJournalCode() == null) {
            throw new IllegalArgumentException("Le journal comptable est obligatoire");
        }

        JournalComptable journal;
        if (dto.getJournalId() != null) {
            journal = journalRepository.findById(dto.getJournalId())
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable ID: " + dto.getJournalId()));
        } else {
            journal = journalRepository.findByCodeAndPointDeVenteId(dto.getJournalCode().toUpperCase(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable code: " + dto.getJournalCode()));
        }

        LocalDate dateEcr = dto.getDateEcriture() != null ? dto.getDateEcriture() : LocalDate.now();
        if (exerciceRepository.isDateInExerciceCloture(dateEcr, tenantId)) {
            throw new IllegalStateException("Impossible d'enregistrer l'écriture : l'exercice comptable pour le " + dateEcr + " est définitivement clôturé.");
        }

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journal);
        ecriture.setDateEcriture(dateEcr);
        ecriture.setLibelle(dto.getLibelle() != null ? dto.getLibelle() : "Écriture du " + dateEcr);
        ecriture.setReferencePiece(dto.getReferencePiece());
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setValidee(Boolean.TRUE.equals(dto.getValidee()));

        if (dto.getNumeroPiece() != null && !dto.getNumeroPiece().trim().isEmpty()) {
            ecriture.setNumeroPiece(dto.getNumeroPiece());
        } else {
            ecriture.setNumeroPiece(genererNumeroPiece(journal, ecriture.getDateEcriture(), tenantId));
        }

        // Lignes
        if (dto.getLignes() == null || dto.getLignes().size() < 2) {
            throw new IllegalArgumentException("Une écriture comptable doit comporter au moins deux lignes (partie double)");
        }

        for (LigneEcritureDTO lDto : dto.getLignes()) {
            CompteComptable compte = resoudreCompte(lDto, tenantId);
            LigneEcriture ligne = new LigneEcriture();
            ligne.setCompte(compte);
            ligne.setDebit(lDto.getDebit() != null ? lDto.getDebit() : BigDecimal.ZERO);
            ligne.setCredit(lDto.getCredit() != null ? lDto.getCredit() : BigDecimal.ZERO);
            ligne.setLibelleLigne(lDto.getLibelleLigne() != null ? lDto.getLibelleLigne() : ecriture.getLibelle());
            ligne.setReferenceLigne(lDto.getReferenceLigne() != null ? lDto.getReferenceLigne() : ecriture.getReferencePiece());
            ligne.setLettrage(lDto.getLettrage());
            ligne.setPointDeVenteId(tenantId);

            ecriture.addLigne(ligne);
        }

        if (!ecriture.isEquilibree()) {
            throw new IllegalStateException(String.format(
                "Écriture déséquilibrée ! Total Débit = %s, Total Crédit = %s (Écart = %s)",
                ecriture.getTotalDebit(), ecriture.getTotalCredit(),
                ecriture.getTotalDebit().subtract(ecriture.getTotalCredit())
            ));
        }

        EcritureComptable saved = ecritureRepository.save(ecriture);

        // Audit Trail
        auditService.logCreation("EcritureComptable", saved.getId(),
                "Création écriture " + saved.getNumeroPiece() + " — " + saved.getLibelle() +
                " (Débit=" + saved.getTotalDebit() + ", Crédit=" + saved.getTotalCredit() + ")");

        return toEcritureDto(saved);
    }

    public EcritureComptableDTO validerEcriture(Long id) {
        EcritureComptable ecriture = ecritureRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Écriture introuvable: " + id));

        if (!ecriture.isEquilibree()) {
            throw new IllegalStateException("Impossible de valider une écriture déséquilibrée");
        }

        ecriture.setValidee(true);
        EcritureComptable saved = ecritureRepository.save(ecriture);

        // Audit Trail
        auditService.logValidation("EcritureComptable", saved.getId(),
                "Validation définitive de l'écriture " + saved.getNumeroPiece());

        return toEcritureDto(saved);
    }

    private String genererNumeroPiece(JournalComptable journal, LocalDate date, Long tenantId) {
        int year = date != null ? date.getYear() : LocalDate.now().getYear();
        String prefix = journal.getCode() + "-" + year + "-";
        Long count = ecritureRepository.countByPrefixAndTenant(prefix, tenantId);
        return String.format("%s%05d", prefix, (count != null ? count : 0) + 1);
    }

    private CompteComptable resoudreCompte(LigneEcritureDTO dto, Long tenantId) {
        if (dto.getCompteId() != null) {
            return compteRepository.findById(dto.getCompteId())
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable ID: " + dto.getCompteId()));
        }
        if (dto.getNumeroCompte() != null && !dto.getNumeroCompte().trim().isEmpty()) {
            return resoudreOuCreerCompte(dto.getNumeroCompte().trim(), dto.getLibelleCompte(), tenantId);
        }
        throw new IllegalArgumentException("Compte comptable non renseigné sur la ligne d'écriture");
    }

    public CompteComptable resoudreOuCreerCompte(String numeroCompte, String libelleOptionnel, Long tenantId) {
        if (numeroCompte == null || numeroCompte.trim().isEmpty()) {
            throw new IllegalArgumentException("Numéro de compte non renseigné");
        }
        String num = numeroCompte.trim();
        return compteRepository.findByNumeroCompteAndPointDeVenteId(num, tenantId)
                .orElseGet(() -> {
                    int classe = Character.isDigit(num.charAt(0)) ? Character.getNumericValue(num.charAt(0)) : 1;
                    String libelle = (libelleOptionnel != null && !libelleOptionnel.trim().isEmpty())
                            ? libelleOptionnel.trim()
                            : infererLibellePcgm(num);
                    SensCompte sens = (classe == 1 || classe == 4 || classe == 7) ? SensCompte.CREDIT : SensCompte.DEBIT;
                    CompteComptable c = new CompteComptable(num, libelle, classe, sens, tenantId);
                    return compteRepository.save(c);
                });
    }

    private String infererLibellePcgm(String numero) {
        if (numero.startsWith("1111")) return "Capital social";
        if (numero.startsWith("1191")) return "Résultat net de l'exercice (Solde créditeur)";
        if (numero.startsWith("1199")) return "Résultat net de l'exercice (Solde débiteur)";
        if (numero.startsWith("23")) return "Immobilisations corporelles";
        if (numero.startsWith("28")) return "Amortissements des immobilisations";
        if (numero.startsWith("31")) return "Stocks";
        if (numero.startsWith("3421")) return "Clients";
        if (numero.startsWith("3455")) return "État - TVA récupérable";
        if (numero.startsWith("4411")) return "Fournisseurs";
        if (numero.startsWith("4432")) return "Rémunérations dues au personnel";
        if (numero.startsWith("4441")) return "C.N.S.S.";
        if (numero.startsWith("4452")) return "État - Impôts et taxes";
        if (numero.startsWith("4455")) return "État - TVA facturée";
        if (numero.startsWith("5141")) return "Banques (soldes débiteurs)";
        if (numero.startsWith("5161")) return "Caisses";
        if (numero.startsWith("6111")) return "Achats de marchandises revendues";
        if (numero.startsWith("6121")) return "Achats de matières premières";
        if (numero.startsWith("6131")) return "Locations et charges locatives";
        if (numero.startsWith("6132")) return "Redevances de crédit-bail";
        if (numero.startsWith("6134")) return "Primes d'assurances";
        if (numero.startsWith("6136")) return "Rémunérations d'intermédiaires et honoraires";
        if (numero.startsWith("6145")) return "Frais de télécommunications";
        if (numero.startsWith("6147")) return "Services bancaires";
        if (numero.startsWith("6171")) return "Rémunération du personnel";
        if (numero.startsWith("6174")) return "Charges sociales";
        if (numero.startsWith("7111")) return "Ventes de marchandises au Maroc";
        if (numero.startsWith("7121")) return "Ventes de biens et services produits";
        return "Compte " + numero;
    }

    // =========================================================================
    // SAISIE KILOMÉTRIQUE ULTRA-RAPIDE (100% CLAVIER - STYLE SAGE 100)
    // =========================================================================

    public EcritureComptableDTO creerEcritureKilometrique(SaisieKilometriqueDTO dto) {
        Long tenantId = getTenantId();
        if (dto.getJournalCode() == null && dto.getJournalId() == null) {
            throw new IllegalArgumentException("Le journal comptable est obligatoire pour la saisie kilométrique.");
        }

        JournalComptable journal;
        if (dto.getJournalId() != null) {
            journal = journalRepository.findById(dto.getJournalId())
                    .orElseThrow(() -> new IllegalArgumentException("Journal introuvable ID: " + dto.getJournalId()));
        } else {
            journal = journalRepository.findByCodeAndPointDeVenteId(dto.getJournalCode().toUpperCase(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Journal introuvable code: " + dto.getJournalCode()));
        }

        LocalDate dateEcr = dto.getDateEcriture() != null ? dto.getDateEcriture() : LocalDate.now();
        if (exerciceRepository.isDateInExerciceCloture(dateEcr, tenantId)) {
            throw new IllegalStateException("Impossible d'enregistrer l'écriture : l'exercice pour le " + dateEcr + " est définitivement clôturé.");
        }

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journal);
        ecriture.setDateEcriture(dateEcr);
        ecriture.setLibelle(dto.getLibellePiece() != null ? dto.getLibellePiece() : "Saisie " + journal.getCode() + " du " + dateEcr);
        ecriture.setReferencePiece(dto.getReferencePiece());
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setValidee(Boolean.TRUE.equals(dto.getValidee()));

        if (dto.getNumeroPiece() != null && !dto.getNumeroPiece().trim().isEmpty()) {
            ecriture.setNumeroPiece(dto.getNumeroPiece().trim());
        } else {
            ecriture.setNumeroPiece(genererNumeroPiece(journal, ecriture.getDateEcriture(), tenantId));
        }

        List<LigneSaisieKilometriqueDTO> lignes = new ArrayList<>(dto.getLignes() != null ? dto.getLignes() : Collections.emptyList());

        // Calcul des totaux
        BigDecimal totalDeb = BigDecimal.ZERO;
        BigDecimal totalCred = BigDecimal.ZERO;
        for (LigneSaisieKilometriqueDTO l : lignes) {
            totalDeb = totalDeb.add(l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO);
            totalCred = totalCred.add(l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO);
        }

        // Auto-équilibrage si demandé
        if (dto.isAutoEquilibrer() && totalDeb.compareTo(totalCred) != 0) {
            BigDecimal ecart = totalDeb.subtract(totalCred).abs();
            String compteContrepartie = dto.getCompteContrepartieAuto();
            if (compteContrepartie == null || compteContrepartie.trim().isEmpty()) {
                compteContrepartie = switch (journal.getCode().toUpperCase()) {
                    case "AC" -> "44110000";
                    case "VE" -> "34210000";
                    case "BQ" -> "51410000";
                    case "CA" -> "51610000";
                    default -> "44110000";
                };
            }

            LigneSaisieKilometriqueDTO lEq = new LigneSaisieKilometriqueDTO();
            lEq.setNumeroCompte(compteContrepartie);
            lEq.setLibelleLigne("Contrepartie " + ecriture.getLibelle());
            lEq.setReferenceLigne(ecriture.getReferencePiece());

            if (totalDeb.compareTo(totalCred) > 0) {
                lEq.setCredit(ecart);
                lEq.setDebit(BigDecimal.ZERO);
            } else {
                lEq.setDebit(ecart);
                lEq.setCredit(BigDecimal.ZERO);
            }
            lignes.add(lEq);
        }

        if (lignes.size() < 2) {
            throw new IllegalArgumentException("Une écriture comptable doit comporter au moins deux lignes (partie double).");
        }

        for (LigneSaisieKilometriqueDTO lDto : lignes) {
            CompteComptable compte = resoudreOuCreerCompte(lDto.getNumeroCompte(), lDto.getLibelleCompte(), tenantId);
            LigneEcriture ligne = new LigneEcriture();
            ligne.setCompte(compte);
            ligne.setDebit(lDto.getDebit() != null ? lDto.getDebit() : BigDecimal.ZERO);
            ligne.setCredit(lDto.getCredit() != null ? lDto.getCredit() : BigDecimal.ZERO);
            ligne.setLibelleLigne(lDto.getLibelleLigne() != null ? lDto.getLibelleLigne() : ecriture.getLibelle());
            ligne.setReferenceLigne(lDto.getReferenceLigne() != null ? lDto.getReferenceLigne() : ecriture.getReferencePiece());
            ligne.setPointDeVenteId(tenantId);
            ecriture.addLigne(ligne);
        }

        if (!ecriture.isEquilibree()) {
            throw new IllegalStateException(String.format(
                    "Écriture déséquilibrée ! Total Débit = %s, Total Crédit = %s (Écart = %s MAD)",
                    ecriture.getTotalDebit(), ecriture.getTotalCredit(),
                    ecriture.getTotalDebit().subtract(ecriture.getTotalCredit())
            ));
        }

        EcritureComptable saved = ecritureRepository.save(ecriture);
        auditService.logCreation("EcritureComptable", saved.getId(),
                "Saisie kilométrique pièce " + saved.getNumeroPiece() + " (" + saved.getLibelle() + ")");

        return toEcritureDto(saved);
    }

    public List<EcritureComptableDTO> creerEcrituresKilometriquesLot(List<SaisieKilometriqueDTO> batch) {
        if (batch == null || batch.isEmpty()) {
            return Collections.emptyList();
        }
        List<EcritureComptableDTO> results = new ArrayList<>();
        for (SaisieKilometriqueDTO item : batch) {
            results.add(creerEcritureKilometrique(item));
        }
        return results;
    }

    public AssistanceSaisieKilometriqueDTO assisterSaisieKilometrique(String numeroCompte, BigDecimal montant, String sens) {
        AssistanceSaisieKilometriqueDTO aide = new AssistanceSaisieKilometriqueDTO();
        aide.setNumeroCompteSaisi(numeroCompte);
        aide.setMontantSaisi(montant != null ? montant : BigDecimal.ZERO);
        aide.setSensSaisi(sens != null ? sens.toUpperCase() : "DEBIT");

        if (numeroCompte == null || numeroCompte.trim().isEmpty() || montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            return aide;
        }

        String num = numeroCompte.trim();
        BigDecimal m = montant;

        // Détection TVA automatique
        if (num.startsWith("6")) {
            aide.setTvaApplicable(true);
            aide.setCompteTvaSuggere("34552000");
            aide.setLibelleTvaSuggere("État - TVA récupérable sur les charges");
            aide.setSensTva("DEBIT");
            BigDecimal montantTva = m.multiply(aide.getTauxTva()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            aide.setMontantTvaCalcule(montantTva);

            aide.setCompteContrepartieSuggere("44110000");
            aide.setLibelleContrepartieSuggere("Fournisseurs");
            aide.setSensContrepartie("CREDIT");
            aide.setMontantContrepartieCalcule(m.add(montantTva));

            aide.setMontantEquilibrage(m);
            aide.setSensEquilibrage("CREDIT");

        } else if (num.startsWith("2")) {
            aide.setTvaApplicable(true);
            aide.setCompteTvaSuggere("34551000");
            aide.setLibelleTvaSuggere("État - TVA récupérable sur immobilisations");
            aide.setSensTva("DEBIT");
            BigDecimal montantTva = m.multiply(aide.getTauxTva()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            aide.setMontantTvaCalcule(montantTva);

            aide.setCompteContrepartieSuggere("44810000");
            aide.setLibelleContrepartieSuggere("Dettes sur acquisitions d'immobilisations");
            aide.setSensContrepartie("CREDIT");
            aide.setMontantContrepartieCalcule(m.add(montantTva));

            aide.setMontantEquilibrage(m);
            aide.setSensEquilibrage("CREDIT");

        } else if (num.startsWith("7")) {
            aide.setTvaApplicable(true);
            aide.setCompteTvaSuggere("44550000");
            aide.setLibelleTvaSuggere("État - TVA facturée");
            aide.setSensTva("CREDIT");
            BigDecimal montantTva = m.multiply(aide.getTauxTva()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            aide.setMontantTvaCalcule(montantTva);

            aide.setCompteContrepartieSuggere("34210000");
            aide.setLibelleContrepartieSuggere("Clients");
            aide.setSensContrepartie("DEBIT");
            aide.setMontantContrepartieCalcule(m.add(montantTva));

            aide.setMontantEquilibrage(m);
            aide.setSensEquilibrage("DEBIT");
        } else {
            aide.setMontantEquilibrage(m);
            aide.setSensEquilibrage("DEBIT".equalsIgnoreCase(sens) ? "CREDIT" : "DEBIT");
        }

        return aide;
    }

    // =========================================================================
    // PASSERELLES AUTOMATIQUES (Ventes, Achats, Règlements)
    // =========================================================================

    public EcritureComptableDTO genererEcritureVente(Facture facture) {
        if (facture == null || facture.getMontantTTC() == null || facture.getMontantTTC().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Long tenantId = facture.getPointDeVenteId() != null ? facture.getPointDeVenteId() : 1L;

        // Vérifier si écriture déjà générée
        Optional<EcritureComptable> existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(
            facture.getNumeroFacture(), tenantId);
        if (existante.isPresent()) {
            return toEcritureDto(existante.get());
        }

        JournalComptable journalVentes = journalRepository.findByCodeAndPointDeVenteId("VT", tenantId)
            .orElseGet(() -> {
                initJournauxParDefaut(tenantId);
                return journalRepository.findByCodeAndPointDeVenteId("VT", tenantId).orElse(null);
            });

        if (journalVentes == null) return null;

        CompteComptable compteClient = findOrCreateCompte("34210000", "Clients", 3, SensCompte.DEBIT, tenantId);
        CompteComptable compteVentes = findOrCreateCompte("71110000", "Ventes de marchandises", 7, SensCompte.CREDIT, tenantId);
        CompteComptable compteTva = findOrCreateCompte("44550000", "État - TVA facturée", 4, SensCompte.CREDIT, tenantId);

        BigDecimal montantHT = facture.getMontantHT() != null ? facture.getMontantHT() : facture.getMontantTTC();
        BigDecimal montantTVA = facture.getMontantTVA() != null ? facture.getMontantTVA() : BigDecimal.ZERO;
        BigDecimal montantTTC = facture.getMontantTTC();

        // Réajustement arithmétique pour garantir l'équilibre parfait
        if (montantHT.add(montantTVA).compareTo(montantTTC) != 0) {
            montantHT = montantTTC.subtract(montantTVA);
        }

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journalVentes);
        ecriture.setDateEcriture(facture.getDateFacture() != null ? facture.getDateFacture() : LocalDate.now());

        if (exerciceRepository.isDateInExerciceCloture(ecriture.getDateEcriture(), tenantId)) {
            throw new IllegalStateException("L'exercice comptable contenant le " + ecriture.getDateEcriture() + " est clôturé. Impossible d'enregistrer l'écriture.");
        }

        String clientNom = facture.getClient() != null ? facture.getClient().getNom() : "Client";
        ecriture.setLibelle("Facture Vente N° " + facture.getNumeroFacture() + " - " + clientNom);
        ecriture.setReferencePiece(facture.getNumeroFacture());
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setNumeroPiece(genererNumeroPiece(journalVentes, ecriture.getDateEcriture(), tenantId));
        ecriture.setValidee(true);

        // Débit Client (TTC)
        ecriture.addLigne(new LigneEcriture(compteClient, montantTTC, BigDecimal.ZERO, "Créance Client " + clientNom, tenantId));
        // Crédit Vente (HT)
        ecriture.addLigne(new LigneEcriture(compteVentes, BigDecimal.ZERO, montantHT, "Ventes marchandises Facture " + facture.getNumeroFacture(), tenantId));
        // Crédit TVA si > 0
        if (montantTVA.compareTo(BigDecimal.ZERO) > 0) {
            ecriture.addLigne(new LigneEcriture(compteTva, BigDecimal.ZERO, montantTVA, "TVA collectée Facture " + facture.getNumeroFacture(), tenantId));
        }

        EcritureComptable saved = ecritureRepository.save(ecriture);
        auditService.logCreation("ECRITURE", saved.getId(), "Génération auto écriture vente N° " + saved.getNumeroPiece() + " (Facture " + facture.getNumeroFacture() + ")");
        return toEcritureDto(saved);
    }

    public EcritureComptableDTO genererEcritureAchat(FactureAchat factureAchat) {
        if (factureAchat == null || factureAchat.getMontantTtc() == null || factureAchat.getMontantTtc().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Long tenantId = factureAchat.getPointDeVenteId() != null ? factureAchat.getPointDeVenteId() : 1L;

        Optional<EcritureComptable> existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(
            factureAchat.getNumeroFacture(), tenantId);
        if (existante.isPresent()) {
            return toEcritureDto(existante.get());
        }

        JournalComptable journalAchats = journalRepository.findByCodeAndPointDeVenteId("AC", tenantId)
            .orElseGet(() -> {
                initJournauxParDefaut(tenantId);
                return journalRepository.findByCodeAndPointDeVenteId("AC", tenantId).orElse(null);
            });

        if (journalAchats == null) return null;

        CompteComptable compteFournisseur = findOrCreateCompte("44110000", "Fournisseurs", 4, SensCompte.CREDIT, tenantId);
        CompteComptable compteAchats = findOrCreateCompte("61110000", "Achats de marchandises", 6, SensCompte.DEBIT, tenantId);
        CompteComptable compteTva = findOrCreateCompte("34550000", "État - TVA récupérable", 3, SensCompte.DEBIT, tenantId);

        BigDecimal montantHT = factureAchat.getMontantHt() != null ? factureAchat.getMontantHt() : factureAchat.getMontantTtc();
        BigDecimal montantTVA = factureAchat.getMontantTva() != null ? factureAchat.getMontantTva() : BigDecimal.ZERO;
        BigDecimal montantTTC = factureAchat.getMontantTtc();

        if (montantHT.add(montantTVA).compareTo(montantTTC) != 0) {
            montantHT = montantTTC.subtract(montantTVA);
        }

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journalAchats);
        LocalDate date = factureAchat.getDateFacture() != null ? factureAchat.getDateFacture().toLocalDate() : LocalDate.now();
        ecriture.setDateEcriture(date);

        if (exerciceRepository.isDateInExerciceCloture(ecriture.getDateEcriture(), tenantId)) {
            throw new IllegalStateException("L'exercice comptable contenant le " + ecriture.getDateEcriture() + " est clôturé. Impossible d'enregistrer l'écriture.");
        }

        String fNom = factureAchat.getFournisseur() != null ? factureAchat.getFournisseur().getNom() : "Fournisseur";
        ecriture.setLibelle("Facture Achat N° " + factureAchat.getNumeroFacture() + " - " + fNom);
        ecriture.setReferencePiece(factureAchat.getNumeroFacture());
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setNumeroPiece(genererNumeroPiece(journalAchats, ecriture.getDateEcriture(), tenantId));
        ecriture.setValidee(true);

        // Débit Charges Achats (HT)
        ecriture.addLigne(new LigneEcriture(compteAchats, montantHT, BigDecimal.ZERO, "Achats marchandises Facture " + factureAchat.getNumeroFacture(), tenantId));
        // Débit TVA déductible si > 0
        if (montantTVA.compareTo(BigDecimal.ZERO) > 0) {
            ecriture.addLigne(new LigneEcriture(compteTva, montantTVA, BigDecimal.ZERO, "TVA déductible Facture " + factureAchat.getNumeroFacture(), tenantId));
        }
        // Crédit Fournisseur (TTC)
        ecriture.addLigne(new LigneEcriture(compteFournisseur, BigDecimal.ZERO, montantTTC, "Dette Fournisseur " + fNom, tenantId));

        EcritureComptable saved = ecritureRepository.save(ecriture);
        auditService.logCreation("ECRITURE", saved.getId(), "Génération auto écriture achat N° " + saved.getNumeroPiece() + " (Facture " + factureAchat.getNumeroFacture() + ")");
        return toEcritureDto(saved);
    }

    public EcritureComptableDTO genererEcriturePaiementClient(Paiement paiement) {
        if (paiement == null || paiement.getMontant() == null || paiement.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Long tenantId = getTenantId();
        String ref = paiement.getNumeroPaiement();

        Optional<EcritureComptable> existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(ref, tenantId);
        if (existante.isPresent()) {
            return toEcritureDto(existante.get());
        }

        String codeJournal = (paiement.getModePaiement() == ModePaiement.ESPECES) ? "CA" : "BQ";
        JournalComptable journal = journalRepository.findByCodeAndPointDeVenteId(codeJournal, tenantId)
            .orElseGet(() -> {
                initJournauxParDefaut(tenantId);
                return journalRepository.findByCodeAndPointDeVenteId(codeJournal, tenantId).orElse(null);
            });

        if (journal == null) return null;

        String numTresorerie = (paiement.getModePaiement() == ModePaiement.ESPECES) ? "51610000" : "51410000";
        String libTresorerie = (paiement.getModePaiement() == ModePaiement.ESPECES) ? "Caisse centrale" : "Banque";

        CompteComptable compteTresorerie = findOrCreateCompte(numTresorerie, libTresorerie, 5, SensCompte.DEBIT, tenantId);
        CompteComptable compteClient = findOrCreateCompte("34210000", "Clients", 3, SensCompte.DEBIT, tenantId);

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journal);
        LocalDate date = paiement.getDatePaiement() != null ? paiement.getDatePaiement().toLocalDate() : LocalDate.now();
        ecriture.setDateEcriture(date);

        if (exerciceRepository.isDateInExerciceCloture(ecriture.getDateEcriture(), tenantId)) {
            throw new IllegalStateException("L'exercice comptable contenant le " + ecriture.getDateEcriture() + " est clôturé. Impossible d'enregistrer l'écriture.");
        }

        String clientNom = paiement.getClient() != null ? paiement.getClient().getNom() : "Client";
        ecriture.setLibelle("Encaissement " + paiement.getModePaiement() + " - " + clientNom + " (" + ref + ")");
        ecriture.setReferencePiece(ref);
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setNumeroPiece(genererNumeroPiece(journal, ecriture.getDateEcriture(), tenantId));
        ecriture.setValidee(true);

        // Débit Banque ou Caisse
        ecriture.addLigne(new LigneEcriture(compteTresorerie, paiement.getMontant(), BigDecimal.ZERO, "Encaissement " + clientNom, tenantId));
        // Crédit Compte Client
        ecriture.addLigne(new LigneEcriture(compteClient, BigDecimal.ZERO, paiement.getMontant(), "Règlement reçu - " + ref, tenantId));

        EcritureComptable saved = ecritureRepository.save(ecriture);
        auditService.logCreation("ECRITURE", saved.getId(), "Génération auto encaissement N° " + saved.getNumeroPiece() + " (" + ref + ")");
        return toEcritureDto(saved);
    }

    public EcritureComptableDTO genererEcritureReglementFournisseur(ReglementFournisseur reglement) {
        if (reglement == null || reglement.getMontant() == null || reglement.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Long tenantId = reglement.getPointDeVenteId() != null ? reglement.getPointDeVenteId() : getTenantId();
        String ref = reglement.getNumeroReglement();

        Optional<EcritureComptable> existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(ref, tenantId);
        if (existante.isPresent()) {
            return toEcritureDto(existante.get());
        }

        String codeJournal = (reglement.getModePaiement() == ModePaiement.ESPECES) ? "CA" : "BQ";
        JournalComptable journal = journalRepository.findByCodeAndPointDeVenteId(codeJournal, tenantId)
            .orElseGet(() -> {
                initJournauxParDefaut(tenantId);
                return journalRepository.findByCodeAndPointDeVenteId(codeJournal, tenantId).orElse(null);
            });

        if (journal == null) return null;

        String numTresorerie = (reglement.getModePaiement() == ModePaiement.ESPECES) ? "51610000" : "51410000";
        String libTresorerie = (reglement.getModePaiement() == ModePaiement.ESPECES) ? "Caisse centrale" : "Banque";

        CompteComptable compteFournisseur = findOrCreateCompte("44110000", "Fournisseurs", 4, SensCompte.CREDIT, tenantId);
        CompteComptable compteTresorerie = findOrCreateCompte(numTresorerie, libTresorerie, 5, SensCompte.DEBIT, tenantId);

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journal);
        LocalDate date = reglement.getDateReglement() != null ? reglement.getDateReglement().toLocalDate() : LocalDate.now();
        ecriture.setDateEcriture(date);

        if (exerciceRepository.isDateInExerciceCloture(ecriture.getDateEcriture(), tenantId)) {
            throw new IllegalStateException("L'exercice comptable contenant le " + ecriture.getDateEcriture() + " est clôturé. Impossible d'enregistrer l'écriture.");
        }

        ecriture.setLibelle("Décaissement Fournisseur " + reglement.getModePaiement() + " (" + ref + ")");
        ecriture.setReferencePiece(ref);
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setNumeroPiece(genererNumeroPiece(journal, ecriture.getDateEcriture(), tenantId));
        ecriture.setValidee(true);

        // Débit Fournisseur
        ecriture.addLigne(new LigneEcriture(compteFournisseur, reglement.getMontant(), BigDecimal.ZERO, "Règlement dette fournisseur", tenantId));
        // Crédit Banque ou Caisse
        ecriture.addLigne(new LigneEcriture(compteTresorerie, BigDecimal.ZERO, reglement.getMontant(), "Décaissement trésorerie " + ref, tenantId));

        EcritureComptable saved = ecritureRepository.save(ecriture);
        auditService.logCreation("ECRITURE", saved.getId(), "Génération auto décaissement N° " + saved.getNumeroPiece() + " (" + ref + ")");
        return toEcritureDto(saved);
    }

    private CompteComptable findOrCreateCompte(String numero, String libelle, int classe, SensCompte sens, Long tenantId) {
        return compteRepository.findByNumeroCompteAndPointDeVenteId(numero, tenantId)
            .orElseGet(() -> {
                CompteComptable c = new CompteComptable(numero, libelle, classe, sens, tenantId);
                return compteRepository.save(c);
            });
    }

    // =========================================================================
    // GRAND LIVRE
    // =========================================================================

    @Transactional(readOnly = true)
    public List<GrandLivreDTO> getGrandLivre(String numeroCompte, LocalDate debut, LocalDate fin) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (debut != null) ? debut : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate dFin = (fin != null) ? fin : LocalDate.now();

        List<CompteComptable> comptes;
        if (numeroCompte != null && !numeroCompte.trim().isEmpty()) {
            comptes = compteRepository.findByNumeroCompteAndPointDeVenteId(numeroCompte.trim(), tenantId)
                .map(List::of)
                .orElse(Collections.emptyList());
        } else {
            comptes = compteRepository.findByPointDeVenteIdOrderByNumeroCompteAsc(tenantId);
            if (comptes.isEmpty()) {
                initPlanComptableParDefaut(tenantId);
                comptes = compteRepository.findByPointDeVenteIdOrderByNumeroCompteAsc(tenantId);
            }
        }

        List<GrandLivreDTO> resultat = new ArrayList<>();

        for (CompteComptable compte : comptes) {
            List<LigneEcriture> lignesPeriode = ligneRepository.findLignesPourGrandLivre(tenantId, compte, dDebut, dFin);

            // Calcul du solde initial (toutes écritures antérieures à dDebut)
            List<LigneEcriture> lignesAnterieures = ligneRepository.findLignesPourGrandLivre(
                tenantId, compte, LocalDate.of(1970, 1, 1), dDebut.minusDays(1));

            BigDecimal soldeInitialDebit = BigDecimal.ZERO;
            BigDecimal soldeInitialCredit = BigDecimal.ZERO;

            for (LigneEcriture l : lignesAnterieures) {
                if (l.getDebit() != null) soldeInitialDebit = soldeInitialDebit.add(l.getDebit());
                if (l.getCredit() != null) soldeInitialCredit = soldeInitialCredit.add(l.getCredit());
            }

            BigDecimal soldeNetInitial = soldeInitialDebit.subtract(soldeInitialCredit);

            // Si aucune ligne antérieure et aucun mouvement sur la période, on peut ignorer pour alléger l'affichage
            if (lignesPeriode.isEmpty() && soldeNetInitial.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            GrandLivreDTO glDto = new GrandLivreDTO();
            glDto.setNumeroCompte(compte.getNumeroCompte());
            glDto.setLibelleCompte(compte.getLibelle());
            glDto.setClasse(compte.getClasse());

            if (soldeNetInitial.compareTo(BigDecimal.ZERO) >= 0) {
                glDto.setSoldeInitialDebit(soldeNetInitial);
                glDto.setSoldeInitialCredit(BigDecimal.ZERO);
            } else {
                glDto.setSoldeInitialDebit(BigDecimal.ZERO);
                glDto.setSoldeInitialCredit(soldeNetInitial.abs());
            }

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal soldeCourant = soldeNetInitial;

            List<GrandLivreDTO.LigneGrandLivreItemDTO> mouvements = new ArrayList<>();

            for (LigneEcriture l : lignesPeriode) {
                BigDecimal deb = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                BigDecimal cred = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;

                totalDebit = totalDebit.add(deb);
                totalCredit = totalCredit.add(cred);
                soldeCourant = soldeCourant.add(deb).subtract(cred);

                GrandLivreDTO.LigneGrandLivreItemDTO item = new GrandLivreDTO.LigneGrandLivreItemDTO();
                item.setId(l.getId());
                item.setLettrage(l.getLettrage());
                if (l.getEcriture() != null) {
                    item.setDate(l.getEcriture().getDateEcriture());
                    item.setNumeroPiece(l.getEcriture().getNumeroPiece());
                    if (l.getEcriture().getJournal() != null) {
                        item.setJournalCode(l.getEcriture().getJournal().getCode());
                    }
                }
                item.setLibelle(l.getLibelleLigne() != null ? l.getLibelleLigne() : (l.getEcriture() != null ? l.getEcriture().getLibelle() : ""));
                item.setDebit(deb);
                item.setCredit(cred);
                item.setSoldeProgressif(soldeCourant);

                mouvements.add(item);
            }

            glDto.setMouvements(mouvements);
            glDto.setTotalDebit(totalDebit);
            glDto.setTotalCredit(totalCredit);

            BigDecimal soldeFinal = soldeCourant;
            if (soldeFinal.compareTo(BigDecimal.ZERO) >= 0) {
                glDto.setSoldeFinalDebit(soldeFinal);
                glDto.setSoldeFinalCredit(BigDecimal.ZERO);
            } else {
                glDto.setSoldeFinalDebit(BigDecimal.ZERO);
                glDto.setSoldeFinalCredit(soldeFinal.abs());
            }

            resultat.add(glDto);
        }

        return resultat;
    }

    // =========================================================================
    // BALANCE GÉNÉRALE
    // =========================================================================

    @Transactional(readOnly = true)
    public List<BalanceCompteDTO> getBalance(LocalDate debut, LocalDate fin) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (debut != null) ? debut : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate dFin = (fin != null) ? fin : LocalDate.now();

        List<LigneEcriture> allLignes = ligneRepository.findAllByTenantAndPeriode(tenantId, dDebut, dFin);

        Map<CompteComptable, BigDecimal[]> cumulParCompte = new LinkedHashMap<>();

        for (LigneEcriture l : allLignes) {
            CompteComptable compte = l.getCompte();
            if (compte != null) {
                BigDecimal[] totaux = cumulParCompte.computeIfAbsent(compte, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                if (l.getDebit() != null) {
                    totaux[0] = totaux[0].add(l.getDebit());
                }
                if (l.getCredit() != null) {
                    totaux[1] = totaux[1].add(l.getCredit());
                }
            }
        }

        List<BalanceCompteDTO> balance = new ArrayList<>();
        cumulParCompte.entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getKey().getNumeroCompte()))
            .forEach(e -> {
                CompteComptable c = e.getKey();
                BigDecimal[] tot = e.getValue();
                balance.add(new BalanceCompteDTO(
                    c.getNumeroCompte(),
                    c.getLibelle(),
                    c.getClasse(),
                    tot[0],
                    tot[1]
                ));
            });

        return balance;
    }

    // =========================================================================
    // DÉCLARATION DE TVA
    // =========================================================================

    @Transactional(readOnly = true)
    public DeclarationTvaDTO getDeclarationTva(LocalDate debut, LocalDate fin) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (debut != null) ? debut : LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);
        LocalDate dFin = (fin != null) ? fin : LocalDate.now();

        List<LigneEcriture> lignes = ligneRepository.findAllByTenantAndPeriode(tenantId, dDebut, dFin);

        BigDecimal tvaCollectee = BigDecimal.ZERO;
        BigDecimal tvaDeductibleCharges = BigDecimal.ZERO;
        BigDecimal tvaDeductibleImmo = BigDecimal.ZERO;
        BigDecimal tvaDeductibleTotal = BigDecimal.ZERO;
        BigDecimal totalVentesHT = BigDecimal.ZERO;
        BigDecimal totalAchatsHT = BigDecimal.ZERO;

        for (LigneEcriture l : lignes) {
            if (l.getCompte() != null && l.getCompte().getNumeroCompte() != null) {
                String num = l.getCompte().getNumeroCompte().trim();
                BigDecimal deb = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                BigDecimal cred = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;

                // Ventes HT (Classe 7 : Produits d'exploitation 71xx) -> Solde Créditeur
                if (num.startsWith("71") || (num.startsWith("7") && !num.startsWith("79"))) {
                    totalVentesHT = totalVentesHT.add(cred.subtract(deb));
                }
                // Achats & Charges HT (Classe 6 : Charges d'exploitation 61xx) -> Solde Débiteur
                else if (num.startsWith("61") || (num.startsWith("6") && !num.startsWith("69"))) {
                    totalAchatsHT = totalAchatsHT.add(deb.subtract(cred));
                }
                // TVA facturée / collectée : compte 4455 (Crédit - Débit)
                else if (num.startsWith("4455")) {
                    tvaCollectee = tvaCollectee.add(cred.subtract(deb));
                }
                // TVA déductible sur immobilisations : compte 34552 (Débit - Crédit)
                else if (num.startsWith("34552")) {
                    BigDecimal mnt = deb.subtract(cred);
                    tvaDeductibleImmo = tvaDeductibleImmo.add(mnt);
                    tvaDeductibleTotal = tvaDeductibleTotal.add(mnt);
                }
                // TVA déductible sur charges : compte 34551 ou compte général 3455 (Débit - Crédit)
                else if (num.startsWith("3455")) {
                    BigDecimal mnt = deb.subtract(cred);
                    tvaDeductibleCharges = tvaDeductibleCharges.add(mnt);
                    tvaDeductibleTotal = tvaDeductibleTotal.add(mnt);
                }
            }
        }

        if (totalVentesHT.compareTo(BigDecimal.ZERO) < 0) totalVentesHT = BigDecimal.ZERO;
        if (totalAchatsHT.compareTo(BigDecimal.ZERO) < 0) totalAchatsHT = BigDecimal.ZERO;
        if (tvaCollectee.compareTo(BigDecimal.ZERO) < 0) tvaCollectee = BigDecimal.ZERO;
        if (tvaDeductibleTotal.compareTo(BigDecimal.ZERO) < 0) tvaDeductibleTotal = BigDecimal.ZERO;
        if (tvaDeductibleCharges.compareTo(BigDecimal.ZERO) < 0) tvaDeductibleCharges = BigDecimal.ZERO;
        if (tvaDeductibleImmo.compareTo(BigDecimal.ZERO) < 0) tvaDeductibleImmo = BigDecimal.ZERO;

        DeclarationTvaDTO dto = new DeclarationTvaDTO();
        dto.setDateDebut(dDebut);
        dto.setDateFin(dFin);
        dto.setTotalVentesHT(totalVentesHT);
        dto.setTotalAchatsHT(totalAchatsHT);
        dto.setTvaCollectee(tvaCollectee);
        dto.setTvaDeductibleCharges(tvaDeductibleCharges);
        dto.setTvaDeductibleImmo(tvaDeductibleImmo);
        dto.setTvaDeductible(tvaDeductibleTotal);

        BigDecimal diff = tvaCollectee.subtract(tvaDeductibleTotal);
        if (diff.compareTo(BigDecimal.ZERO) >= 0) {
            dto.setTvaAPayer(diff);
            dto.setCreditTva(BigDecimal.ZERO);
        } else {
            dto.setTvaAPayer(BigDecimal.ZERO);
            dto.setCreditTva(diff.abs());
        }

        // Ventilation par taux réglementaire PCGM (20%, 14%, 10%, 7%)
        List<VentilationTvaDTO> ventilation = new ArrayList<>();
        ventilation.add(new VentilationTvaDTO(20, totalVentesHT, tvaCollectee, "Taux normal (Marchandises, prestations générales, honoraires)"));
        ventilation.add(new VentilationTvaDTO(14, BigDecimal.ZERO, BigDecimal.ZERO, "Taux intermédiaire (Transport, énergie, eau)"));
        ventilation.add(new VentilationTvaDTO(10, BigDecimal.ZERO, BigDecimal.ZERO, "Taux réduit (Hôtellerie, restauration)"));
        ventilation.add(new VentilationTvaDTO(7, BigDecimal.ZERO, BigDecimal.ZERO, "Taux super-réduit (Produits de 1ère nécessité, fournitures scolaires)"));

        dto.setVentilationParTaux(ventilation);
        return dto;
    }

    // =========================================================================
    // MAPPERS DTO
    // =========================================================================

    private CompteComptableDTO toCompteDto(CompteComptable c) {
        CompteComptableDTO dto = new CompteComptableDTO();
        dto.setId(c.getId());
        dto.setNumeroCompte(c.getNumeroCompte());
        dto.setLibelle(c.getLibelle());
        dto.setClasse(c.getClasse());
        dto.setSensParDefaut(c.getSensParDefaut());
        dto.setActif(c.getActif());
        return dto;
    }

    private JournalComptableDTO toJournalDto(JournalComptable j) {
        JournalComptableDTO dto = new JournalComptableDTO();
        dto.setId(j.getId());
        dto.setCode(j.getCode());
        dto.setLibelle(j.getLibelle());
        dto.setTypeJournal(j.getTypeJournal());
        dto.setActif(j.getActif());
        return dto;
    }

    private EcritureComptableDTO toEcritureDto(EcritureComptable e) {
        EcritureComptableDTO dto = new EcritureComptableDTO();
        dto.setId(e.getId());
        dto.setNumeroPiece(e.getNumeroPiece());
        dto.setDateEcriture(e.getDateEcriture());
        dto.setLibelle(e.getLibelle());
        dto.setReferencePiece(e.getReferencePiece());
        dto.setValidee(e.getValidee());
        dto.setTotalDebit(e.getTotalDebit());
        dto.setTotalCredit(e.getTotalCredit());

        if (e.getJournal() != null) {
            dto.setJournalId(e.getJournal().getId());
            dto.setJournalCode(e.getJournal().getCode());
            dto.setJournalLibelle(e.getJournal().getLibelle());
        }

        if (e.getLignes() != null) {
            dto.setLignes(e.getLignes().stream().map(this::toLigneDto).collect(Collectors.toList()));
        }

        return dto;
    }

    private LigneEcritureDTO toLigneDto(LigneEcriture l) {
        LigneEcritureDTO dto = new LigneEcritureDTO();
        dto.setId(l.getId());
        if (l.getCompte() != null) {
            dto.setCompteId(l.getCompte().getId());
            dto.setNumeroCompte(l.getCompte().getNumeroCompte());
            dto.setLibelleCompte(l.getCompte().getLibelle());
        }
        dto.setDebit(l.getDebit());
        dto.setCredit(l.getCredit());
        dto.setLibelleLigne(l.getLibelleLigne());
        dto.setReferenceLigne(l.getReferenceLigne());
        dto.setLettrage(l.getLettrage());
        return dto;
    }

    public byte[] exporterEcrituresCsv(Long journalId, LocalDate dateDebut, LocalDate dateFin) {
        List<EcritureComptableDTO> ecritures = getEcritures(journalId, dateDebut, dateFin);
        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 pour ouverture directe dans Excel
        sb.append("\uFEFF");
        sb.append("Date;Journal;NumeroPiece;ReferencePiece;CompteNum;CompteLibelle;LibelleEcriture;Debit;Credit\n");

        for (EcritureComptableDTO e : ecritures) {
            String dateStr = e.getDateEcriture() != null ? e.getDateEcriture().toString() : "";
            String journalCode = e.getJournalCode() != null ? e.getJournalCode() : "";
            String numPiece = e.getNumeroPiece() != null ? e.getNumeroPiece() : "";
            String refPiece = e.getReferencePiece() != null ? e.getReferencePiece() : "";
            String libelle = e.getLibelle() != null ? e.getLibelle().replace(";", ",") : "";

            if (e.getLignes() != null) {
                for (LigneEcritureDTO l : e.getLignes()) {
                    sb.append(dateStr).append(";")
                      .append(journalCode).append(";")
                      .append(numPiece).append(";")
                      .append(refPiece).append(";")
                      .append(l.getNumeroCompte() != null ? l.getNumeroCompte() : "").append(";")
                      .append(l.getLibelleCompte() != null ? l.getLibelleCompte().replace(";", ",") : "").append(";")
                      .append(l.getLibelleLigne() != null ? l.getLibelleLigne().replace(";", ",") : libelle).append(";")
                      .append(l.getDebit() != null ? l.getDebit().toPlainString() : "0.00").append(";")
                      .append(l.getCredit() != null ? l.getCredit().toPlainString() : "0.00").append("\n");
                }
            }
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // =========================================================================
    // LETTRAGE DES ÉCRITURES COMPTABLES
    // =========================================================================

    @Transactional(readOnly = true)
    public List<LigneLettrageDTO> getLignesNonLettrees(String prefixCompte, LocalDate debut, LocalDate fin) {
        Long tenantId = getTenantId();
        String prefix = (prefixCompte != null && !prefixCompte.trim().isEmpty()) ? prefixCompte.trim() : "3421";
        LocalDate dDebut = debut != null ? debut : LocalDate.of(2000, 1, 1);
        LocalDate dFin = fin != null ? fin : LocalDate.now().plusYears(1);

        List<LigneEcriture> lignes = ligneRepository.findLignesNonLettrees(tenantId, prefix, dDebut, dFin);
        return lignes.stream().map(this::toLigneLettrageDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LigneLettrageDTO> getLignesLettrees(String prefixCompte) {
        Long tenantId = getTenantId();
        String prefix = (prefixCompte != null && !prefixCompte.trim().isEmpty()) ? prefixCompte.trim() : "3421";
        List<LigneEcriture> lignes = ligneRepository.findLignesLettrees(tenantId, prefix);
        return lignes.stream().map(this::toLigneLettrageDto).collect(Collectors.toList());
    }

    public String validerLettrage(List<Long> ligneIds) {
        Long tenantId = getTenantId();
        if (ligneIds == null || ligneIds.size() < 2) {
            throw new IllegalArgumentException("Le lettrage requiert au moins 2 lignes d'écritures (une facture et un règlement/avoir).");
        }

        List<LigneEcriture> lignes = ligneRepository.findAllById(ligneIds);
        if (lignes.size() != ligneIds.size()) {
            Set<Long> trouves = lignes.stream().map(LigneEcriture::getId).collect(Collectors.toSet());
            List<Long> introuvables = ligneIds.stream().filter(id -> !trouves.contains(id)).collect(Collectors.toList());
            throw new IllegalArgumentException("Certaines lignes d'écritures sont introuvables en base de données : " + introuvables);
        }

        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;

        for (LigneEcriture l : lignes) {
            if (!tenantId.equals(l.getPointDeVenteId())) {
                throw new IllegalStateException("Ligne non autorisée pour ce tenant : " + l.getId());
            }
            if (l.getLettrage() != null && !l.getLettrage().trim().isEmpty()) {
                throw new IllegalStateException("La ligne #" + l.getId() + " est déjà lettrée avec le code : " + l.getLettrage());
            }
            sumDebit = sumDebit.add(l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO);
            sumCredit = sumCredit.add(l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO);
        }

        if (sumDebit.subtract(sumCredit).abs().compareTo(new BigDecimal("0.05")) > 0) {
            throw new IllegalStateException(String.format(
                "Déséquilibre de lettrage ! Total Débit = %s, Total Crédit = %s (Écart = %s MAD). Les montants doivent s'équilibrer exactement.",
                sumDebit, sumCredit, sumDebit.subtract(sumCredit)
            ));
        }

        String nouveauCode = genererCodeLettrageSuivant(tenantId);
        for (LigneEcriture l : lignes) {
            l.setLettrage(nouveauCode);
        }
        ligneRepository.saveAll(lignes);

        // Audit Trail
        auditService.logAction(ActionAudit.LETTRAGE, "LigneEcriture", ligneIds.get(0),
                "Lettrage " + nouveauCode + " appliqué sur " + ligneIds.size() + " lignes : " + ligneIds);

        return nouveauCode;
    }

    public void annulerLettrage(String codeLettrage) {
        Long tenantId = getTenantId();
        if (codeLettrage == null || codeLettrage.trim().isEmpty()) {
            throw new IllegalArgumentException("Code de lettrage manquant");
        }
        List<LigneEcriture> lignes = ligneRepository.findByLettrageAndPointDeVenteId(codeLettrage.trim().toUpperCase(), tenantId);
        List<Long> ligneIds = lignes.stream().map(LigneEcriture::getId).collect(Collectors.toList());
        for (LigneEcriture l : lignes) {
            l.setLettrage(null);
        }
        ligneRepository.saveAll(lignes);

        // Audit Trail
        auditService.logAction(ActionAudit.ANNULATION_LETTRAGE, "LigneEcriture",
                ligneIds.isEmpty() ? 0L : ligneIds.get(0),
                "Annulation du lettrage " + codeLettrage + " sur " + ligneIds.size() + " lignes : " + ligneIds);
    }

    public void delettrerLignes(List<Long> ligneIds) {
        Long tenantId = getTenantId();
        if (ligneIds == null || ligneIds.isEmpty()) {
            return;
        }
        List<LigneEcriture> lignes = ligneRepository.findAllById(ligneIds).stream()
                .filter(l -> l.getPointDeVenteId().equals(tenantId))
                .collect(Collectors.toList());

        for (LigneEcriture l : lignes) {
            l.setLettrage(null);
        }
        ligneRepository.saveAll(lignes);

        auditService.logAction(ActionAudit.ANNULATION_LETTRAGE, "LigneEcriture",
                ligneIds.get(0), "Délettrage manuel sur " + lignes.size() + " lignes : " + ligneIds);
    }

    public Map<String, Object> autoLettrage(String prefixCompte) {
        Long tenantId = getTenantId();
        String prefix = (prefixCompte != null && !prefixCompte.trim().isEmpty()) ? prefixCompte.trim() : "3421";
        List<LigneEcriture> nonLettrees = ligneRepository.findLignesNonLettrees(tenantId, prefix, LocalDate.of(2000, 1, 1), LocalDate.now().plusYears(1));

        List<LigneEcriture> debits = nonLettrees.stream().filter(l -> l.getDebit().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
        List<LigneEcriture> credits = nonLettrees.stream().filter(l -> l.getCredit().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());

        int countLettrees = 0;
        int countCodes = 0;
        Set<Long> debitsUtilises = new HashSet<>();
        Set<Long> creditsUtilises = new HashSet<>();

        // PASSE 1 : Matching strict par Référence de Pièce exacte + Montant égal
        for (LigneEcriture deb : debits) {
            if (debitsUtilises.contains(deb.getId())) continue;
            for (LigneEcriture cred : credits) {
                if (creditsUtilises.contains(cred.getId())) continue;

                if (deb.getDebit().compareTo(cred.getCredit()) == 0) {
                    boolean samePiece = deb.getReferenceLigne() != null && cred.getReferenceLigne() != null &&
                            deb.getReferenceLigne().trim().equalsIgnoreCase(cred.getReferenceLigne().trim());

                    if (samePiece) {
                        String code = genererCodeLettrageSuivant(tenantId);
                        deb.setLettrage(code);
                        cred.setLettrage(code);
                        ligneRepository.save(deb);
                        ligneRepository.save(cred);
                        debitsUtilises.add(deb.getId());
                        creditsUtilises.add(cred.getId());
                        countLettrees += 2;
                        countCodes++;
                        break;
                    }
                }
            }
        }

        // PASSE 2 : Matching par Même Compte Comptable + Montant égal
        for (LigneEcriture deb : debits) {
            if (debitsUtilises.contains(deb.getId())) continue;
            for (LigneEcriture cred : credits) {
                if (creditsUtilises.contains(cred.getId())) continue;

                if (deb.getDebit().compareTo(cred.getCredit()) == 0) {
                    boolean sameCompte = deb.getCompte() != null && cred.getCompte() != null &&
                            deb.getCompte().getId().equals(cred.getCompte().getId());

                    if (sameCompte) {
                        String code = genererCodeLettrageSuivant(tenantId);
                        deb.setLettrage(code);
                        cred.setLettrage(code);
                        ligneRepository.save(deb);
                        ligneRepository.save(cred);
                        debitsUtilises.add(deb.getId());
                        creditsUtilises.add(cred.getId());
                        countLettrees += 2;
                        countCodes++;
                        break;
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("lignesLettrees", countLettrees);
        result.put("codesAttribues", countCodes);
        return result;
    }

    private String genererCodeLettrageSuivant(Long tenantId) {
        String dernier = ligneRepository.findDernierLettrage(tenantId);
        if (dernier == null || dernier.trim().isEmpty() || !dernier.matches("^[A-Z]+$")) {
            return "AA";
        }
        dernier = dernier.trim();
        char[] chars = dernier.toCharArray();
        int i = chars.length - 1;
        while (i >= 0) {
            if (chars[i] < 'Z') {
                chars[i]++;
                return new String(chars);
            } else {
                chars[i] = 'A';
                i--;
            }
        }
        return "A" + new String(chars);
    }

    private LigneLettrageDTO toLigneLettrageDto(LigneEcriture l) {
        LigneLettrageDTO dto = new LigneLettrageDTO();
        dto.setId(l.getId());
        if (l.getEcriture() != null) {
            dto.setEcritureId(l.getEcriture().getId());
            dto.setNumeroPiece(l.getEcriture().getNumeroPiece());
            dto.setDateEcriture(l.getEcriture().getDateEcriture());
            if (l.getEcriture().getJournal() != null) {
                dto.setJournalCode(l.getEcriture().getJournal().getCode());
            }
        }
        if (l.getCompte() != null) {
            dto.setNumeroCompte(l.getCompte().getNumeroCompte());
            dto.setLibelleCompte(l.getCompte().getLibelle());
        }
        dto.setLibelleLigne(l.getLibelleLigne());
        dto.setDebit(l.getDebit());
        dto.setCredit(l.getCredit());
        dto.setLettrage(l.getLettrage());
        return dto;
    }

    // =========================================================================
    // BILAN OFFICIEL PCGM (ACTIF / PASSIF)
    // =========================================================================

    @Transactional(readOnly = true)
    public BilanOfficielDTO getBilanOfficiel(LocalDate dateArrete) {
        Long tenantId = getTenantId();
        LocalDate date = (dateArrete != null) ? dateArrete : LocalDate.now();

        List<BalanceCompteDTO> balance = getBalance(LocalDate.of(1900, 1, 1), date);

        BilanOfficielDTO dto = new BilanOfficielDTO();
        dto.setDateArrete(date);
        dto.setTenantId(tenantId);

        java.util.function.Function<String, BigDecimal> soldeDebiteur = prefix -> balance.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal d = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal c = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return d.subtract(c);
                })
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        java.util.function.Function<String, BigDecimal> soldeCrediteur = prefix -> balance.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal d = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal c = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return c.subtract(d);
                })
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // I. ACTIF IMMOBILISÉ
        BigDecimal brutNonVal = soldeDebiteur.apply("21");
        BigDecimal amortNonVal = soldeCrediteur.apply("281");
        dto.getActifImmobilise().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("21", "Immobilisation en non-valeurs", brutNonVal, amortNonVal, brutNonVal.subtract(amortNonVal)));

        BigDecimal brutIncorp = soldeDebiteur.apply("22");
        BigDecimal amortIncorp = soldeCrediteur.apply("282");
        dto.getActifImmobilise().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("22", "Immobilisations incorporelles", brutIncorp, amortIncorp, brutIncorp.subtract(amortIncorp)));

        BigDecimal brutCorp = soldeDebiteur.apply("23");
        BigDecimal amortCorp = soldeCrediteur.apply("283");
        dto.getActifImmobilise().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("23", "Immobilisations corporelles", brutCorp, amortCorp, brutCorp.subtract(amortCorp)));

        BigDecimal brutFin = soldeDebiteur.apply("24").add(soldeDebiteur.apply("25"));
        BigDecimal provFin = soldeCrediteur.apply("294").add(soldeCrediteur.apply("295"));
        dto.getActifImmobilise().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("24/25", "Immobilisations financières", brutFin, provFin, brutFin.subtract(provFin)));

        BigDecimal ecartActifImmo = soldeDebiteur.apply("27");
        dto.getActifImmobilise().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("27", "Écarts de conversion - Actif (durables)", ecartActifImmo, BigDecimal.ZERO, ecartActifImmo));

        BigDecimal totalActifImmoBrut = brutNonVal.add(brutIncorp).add(brutCorp).add(brutFin).add(ecartActifImmo);
        BigDecimal totalActifImmoAmort = amortNonVal.add(amortIncorp).add(amortCorp).add(provFin);
        dto.getActifImmobilise().setTotalBrut(totalActifImmoBrut);
        dto.getActifImmobilise().setTotalAmortissements(totalActifImmoAmort);
        dto.getActifImmobilise().setTotalNet(totalActifImmoBrut.subtract(totalActifImmoAmort));

        // II. ACTIF CIRCULANT (HORS TRÉSORERIE)
        BigDecimal brutStocks = soldeDebiteur.apply("31");
        BigDecimal provStocks = soldeCrediteur.apply("391");
        dto.getActifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("31", "Stocks (Marchandises, matières, PF)", brutStocks, provStocks, brutStocks.subtract(provStocks)));

        BigDecimal brutCreances = soldeDebiteur.apply("34");
        BigDecimal provCreances = soldeCrediteur.apply("394");
        dto.getActifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("34", "Créances de l'actif circulant (Clients, État débiteur)", brutCreances, provCreances, brutCreances.subtract(provCreances)));

        BigDecimal brutTvp = soldeDebiteur.apply("35");
        BigDecimal provTvp = soldeCrediteur.apply("395");
        dto.getActifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("35", "Titres et valeurs de placement", brutTvp, provTvp, brutTvp.subtract(provTvp)));

        BigDecimal ecartActifCirc = soldeDebiteur.apply("37");
        dto.getActifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("37", "Écarts de conversion - Actif (circulants)", ecartActifCirc, BigDecimal.ZERO, ecartActifCirc));

        BigDecimal totalActifCircBrut = brutStocks.add(brutCreances).add(brutTvp).add(ecartActifCirc);
        BigDecimal totalActifCircAmort = provStocks.add(provCreances).add(provTvp);
        dto.getActifCirculant().setTotalBrut(totalActifCircBrut);
        dto.getActifCirculant().setTotalAmortissements(totalActifCircAmort);
        dto.getActifCirculant().setTotalNet(totalActifCircBrut.subtract(totalActifCircAmort));

        // III. TRÉSORERIE - ACTIF
        BigDecimal cheques = soldeDebiteur.apply("511");
        dto.getTresorerieActif().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("511", "Chèques et valeurs à encaisser", cheques, BigDecimal.ZERO, cheques));

        BigDecimal banques = soldeDebiteur.apply("514");
        dto.getTresorerieActif().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("514", "Banques, TG et CCP", banques, BigDecimal.ZERO, banques));

        BigDecimal caisses = soldeDebiteur.apply("516");
        dto.getTresorerieActif().getLignes().add(new BilanOfficielDTO.LigneBilanActifDTO("516", "Caisses, régies d'avances", caisses, BigDecimal.ZERO, caisses));

        BigDecimal totalTresActifBrut = cheques.add(banques).add(caisses);
        dto.getTresorerieActif().setTotalBrut(totalTresActifBrut);
        dto.getTresorerieActif().setTotalAmortissements(BigDecimal.ZERO);
        dto.getTresorerieActif().setTotalNet(totalTresActifBrut);

        // TOTAUX ACTIF
        dto.setTotalActifBrut(totalActifImmoBrut.add(totalActifCircBrut).add(totalTresActifBrut));
        dto.setTotalActifAmortissements(totalActifImmoAmort.add(totalActifCircAmort));
        dto.setTotalActifNet(dto.getActifImmobilise().getTotalNet().add(dto.getActifCirculant().getTotalNet()).add(dto.getTresorerieActif().getTotalNet()));

        // ==========================================
        // PASSIF
        // ==========================================
        BigDecimal capital = soldeCrediteur.apply("111");
        dto.getFinancementPermanent().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("111", "Capital social ou personnel", capital));

        BigDecimal reserves = soldeCrediteur.apply("114").add(soldeCrediteur.apply("115"));
        dto.getFinancementPermanent().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("114/115", "Réserves (Légale, statutaires)", reserves));

        BigDecimal reportANouveau = soldeCrediteur.apply("116").subtract(soldeDebiteur.apply("1169"));
        dto.getFinancementPermanent().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("116", "Report à nouveau", reportANouveau));

        BigDecimal produits = BigDecimal.ZERO;
        BigDecimal charges = BigDecimal.ZERO;
        for (BalanceCompteDTO b : balance) {
            BigDecimal deb = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
            BigDecimal cred = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
            if (b.getNumeroCompte().startsWith("7")) {
                produits = produits.add(cred.subtract(deb));
            } else if (b.getNumeroCompte().startsWith("6")) {
                charges = charges.add(deb.subtract(cred));
            }
        }
        BigDecimal resultatNet = produits.subtract(charges);
        dto.setResultatNetExercice(resultatNet);
        dto.getFinancementPermanent().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("119", "Résultat net de l'exercice", resultatNet));

        BigDecimal dettesFin = soldeCrediteur.apply("14");
        dto.getFinancementPermanent().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("14", "Dettes de financement", dettesFin));

        BigDecimal provDurables = soldeCrediteur.apply("15");
        dto.getFinancementPermanent().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("15", "Provisions durables pour risques et charges", provDurables));

        BigDecimal totalFinPerm = capital.add(reserves).add(reportANouveau).add(resultatNet).add(dettesFin).add(provDurables);
        dto.getFinancementPermanent().setTotal(totalFinPerm);

        // II. PASSIF CIRCULANT
        BigDecimal fournisseurs = soldeCrediteur.apply("441");
        dto.getPassifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("441", "Fournisseurs et comptes rattachés", fournisseurs));

        BigDecimal clientsCred = soldeCrediteur.apply("442");
        dto.getPassifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("442", "Clients créditeurs, avances et acomptes", clientsCred));

        BigDecimal personnelDettes = soldeCrediteur.apply("443");
        dto.getPassifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("443", "Personnel - rémunérations dues", personnelDettes));

        BigDecimal organismesSoc = soldeCrediteur.apply("444");
        dto.getPassifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("444", "Organismes sociaux", organismesSoc));

        BigDecimal etatDettes = soldeCrediteur.apply("445");
        dto.getPassifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("445", "État - créditeur (TVA due, IS, taxes)", etatDettes));

        BigDecimal autresDettes = soldeCrediteur.apply("448");
        dto.getPassifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("448", "Autres créanciers", autresDettes));

        BigDecimal provCirc = soldeCrediteur.apply("45");
        dto.getPassifCirculant().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("45", "Provisions pour risques et charges (circulant)", provCirc));

        BigDecimal totalPassifCirc = fournisseurs.add(clientsCred).add(personnelDettes).add(organismesSoc).add(etatDettes).add(autresDettes).add(provCirc);
        dto.getPassifCirculant().setTotal(totalPassifCirc);

        // III. TRÉSORERIE - PASSIF
        BigDecimal escomptes = soldeCrediteur.apply("552");
        dto.getTresoreriePassif().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("552", "Crédits d'escompte", escomptes));

        BigDecimal decouvert = soldeCrediteur.apply("554");
        dto.getTresoreriePassif().getLignes().add(new BilanOfficielDTO.LigneBilanPassifDTO("554", "Crédits de trésorerie / Découverts", decouvert));

        BigDecimal totalTresPassif = escomptes.add(decouvert);
        dto.getTresoreriePassif().setTotal(totalTresPassif);

        // TOTAL PASSIF
        BigDecimal totalPassif = totalFinPerm.add(totalPassifCirc).add(totalTresPassif);
        dto.setTotalPassif(totalPassif);

        BigDecimal diff = dto.getTotalActifNet().subtract(totalPassif).abs();
        dto.setEcartEquilibre(diff);
        dto.setEquilibre(diff.compareTo(new BigDecimal("0.05")) <= 0);

        return dto;
    }

    // =========================================================================
    // COMPTE DE PRODUITS ET CHARGES (CPC OFFICIEL PCGM)
    // =========================================================================

    @Transactional(readOnly = true)
    public CpcOfficielDTO getCpcOfficiel(LocalDate dateDebut, LocalDate dateFin) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();

        List<BalanceCompteDTO> balance = getBalance(dDebut, dFin);

        CpcOfficielDTO dto = new CpcOfficielDTO();
        dto.setDateDebut(dDebut);
        dto.setDateFin(dFin);
        dto.setTenantId(tenantId);

        // Fonctions de calcul par préfixe de compte
        // Pour les produits (Classe 7) : Solde créditeur net = Crédit - Débit
        java.util.function.Function<String, BigDecimal> soldeProduit = prefix -> balance.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal deb = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal cred = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return cred.subtract(deb);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pour les charges (Classe 6) : Solde débiteur net = Débit - Crédit
        java.util.function.Function<String, BigDecimal> soldeCharge = prefix -> balance.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal deb = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal cred = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return deb.subtract(cred);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---------------------------------------------------------------------
        // I. PRODUITS D'EXPLOITATION (Rubrique 71)
        // ---------------------------------------------------------------------
        BigDecimal vntMarchandises = soldeProduit.apply("711");
        dto.getProduitsExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("711", "Ventes de marchandises (en l'état)", vntMarchandises));

        BigDecimal vntBiensServices = soldeProduit.apply("712");
        dto.getProduitsExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("712", "Ventes de biens et services produits", vntBiensServices));

        BigDecimal ca = vntMarchandises.add(vntBiensServices);
        dto.setChiffreAffaires(ca);

        BigDecimal varStock = soldeProduit.apply("713");
        dto.getProduitsExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("713", "Variation des stocks de produits (±)", varStock));

        BigDecimal immoProduite = soldeProduit.apply("714");
        dto.getProduitsExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("714", "Immobilisations produites par l'Ese pour elle-même", immoProduite));

        BigDecimal subventionsExpl = soldeProduit.apply("716");
        dto.getProduitsExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("716", "Subventions d'exploitation", subventionsExpl));

        BigDecimal autresProdExpl = soldeProduit.apply("718");
        dto.getProduitsExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("718", "Autres produits d'exploitation", autresProdExpl));

        BigDecimal reprisesExpl = soldeProduit.apply("719");
        dto.getProduitsExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("719", "Reprises d'exploitation ; transferts de charges", reprisesExpl));

        BigDecimal totalI = vntMarchandises.add(vntBiensServices).add(varStock).add(immoProduite).add(subventionsExpl).add(autresProdExpl).add(reprisesExpl);
        dto.getProduitsExploitation().setTotal(totalI);

        // ---------------------------------------------------------------------
        // II. CHARGES D'EXPLOITATION (Rubrique 61)
        // ---------------------------------------------------------------------
        BigDecimal achatsMarchandises = soldeCharge.apply("611");
        dto.getChargesExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("611", "Achats revendus de marchandises", achatsMarchandises));

        BigDecimal matPremieres = soldeCharge.apply("612");
        dto.getChargesExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("612", "Achats consommés de matières et de fournitures", matPremieres));

        BigDecimal autresChargesExt = soldeCharge.apply("613").add(soldeCharge.apply("614"));
        dto.getChargesExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("613/614", "Autres charges externes", autresChargesExt));

        BigDecimal impotsTaxes = soldeCharge.apply("616");
        dto.getChargesExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("616", "Impôts et taxes", impotsTaxes));

        BigDecimal chargesPersonnel = soldeCharge.apply("617");
        dto.getChargesExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("617", "Charges de personnel", chargesPersonnel));

        BigDecimal autresChargesExpl = soldeCharge.apply("618");
        dto.getChargesExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("618", "Autres charges d'exploitation", autresChargesExpl));

        BigDecimal dotationsExpl = soldeCharge.apply("619");
        dto.getChargesExploitation().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("619", "Dotations d'exploitation", dotationsExpl));

        BigDecimal totalII = achatsMarchandises.add(matPremieres).add(autresChargesExt).add(impotsTaxes).add(chargesPersonnel).add(autresChargesExpl).add(dotationsExpl);
        dto.getChargesExploitation().setTotal(totalII);

        // III. RÉSULTAT D'EXPLOITATION (I - II)
        BigDecimal resultatExploitation = totalI.subtract(totalII);
        dto.setResultatExploitation(resultatExploitation);

        // ---------------------------------------------------------------------
        // IV. PRODUITS FINANCIERS (Rubrique 73)
        // ---------------------------------------------------------------------
        BigDecimal titresImmo = soldeProduit.apply("732");
        dto.getProduitsFinanciers().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("732", "Produits des titres de participation et autres t. immo", titresImmo));

        BigDecimal gainsChange = soldeProduit.apply("733");
        dto.getProduitsFinanciers().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("733", "Gains de change", gainsChange));

        BigDecimal interetsProd = soldeProduit.apply("738");
        dto.getProduitsFinanciers().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("738", "Intérêts et autres produits financiers", interetsProd));

        BigDecimal reprisesFin = soldeProduit.apply("739");
        dto.getProduitsFinanciers().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("739", "Reprises financières ; transferts de charges", reprisesFin));

        BigDecimal totalIV = titresImmo.add(gainsChange).add(interetsProd).add(reprisesFin);
        dto.getProduitsFinanciers().setTotal(totalIV);

        // ---------------------------------------------------------------------
        // V. CHARGES FINANCIÈRES (Rubrique 63)
        // ---------------------------------------------------------------------
        BigDecimal chargesInterets = soldeCharge.apply("631");
        dto.getChargesFinancieres().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("631", "Charges d'intérêts", chargesInterets));

        BigDecimal pertesChange = soldeCharge.apply("633");
        dto.getChargesFinancieres().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("633", "Pertes de change", pertesChange));

        BigDecimal autresChargesFin = soldeCharge.apply("638");
        dto.getChargesFinancieres().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("638", "Autres charges financières", autresChargesFin));

        BigDecimal dotationsFin = soldeCharge.apply("639");
        dto.getChargesFinancieres().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("639", "Dotations financières", dotationsFin));

        BigDecimal totalV = chargesInterets.add(pertesChange).add(autresChargesFin).add(dotationsFin);
        dto.getChargesFinancieres().setTotal(totalV);

        // VI. RÉSULTAT FINANCIER (IV - V)
        BigDecimal resultatFinancier = totalIV.subtract(totalV);
        dto.setResultatFinancier(resultatFinancier);

        // VII. RÉSULTAT COURANT (III + VI)
        BigDecimal resultatCourant = resultatExploitation.add(resultatFinancier);
        dto.setResultatCourant(resultatCourant);

        // ---------------------------------------------------------------------
        // VIII. PRODUITS NON COURANTS (Rubrique 75)
        // ---------------------------------------------------------------------
        BigDecimal prodCessions = soldeProduit.apply("751");
        dto.getProduitsNonCourants().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("751", "Produits des cessions d'immobilisations", prodCessions));

        BigDecimal subEquilibre = soldeProduit.apply("756");
        dto.getProduitsNonCourants().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("756", "Subventions d'équilibre", subEquilibre));

        BigDecimal reprisesSubv = soldeProduit.apply("757");
        dto.getProduitsNonCourants().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("757", "Reprises sur subventions d'investissement", reprisesSubv));

        BigDecimal autresProdNonCour = soldeProduit.apply("758");
        dto.getProduitsNonCourants().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("758", "Autres produits non courants", autresProdNonCour));

        BigDecimal reprisesNonCour = soldeProduit.apply("759");
        dto.getProduitsNonCourants().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("759", "Reprises non courantes ; transferts de charges", reprisesNonCour));

        BigDecimal totalVIII = prodCessions.add(subEquilibre).add(reprisesSubv).add(autresProdNonCour).add(reprisesNonCour);
        dto.getProduitsNonCourants().setTotal(totalVIII);

        // ---------------------------------------------------------------------
        // IX. CHARGES NON COURANTES (Rubrique 65)
        // ---------------------------------------------------------------------
        BigDecimal vnaImmo = soldeCharge.apply("651");
        dto.getChargesNonCourantes().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("651", "Valeurs nettes d'amort. des immo. cédées (VNA)", vnaImmo));

        BigDecimal subAccordees = soldeCharge.apply("656");
        dto.getChargesNonCourantes().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("656", "Subventions accordées", subAccordees));

        BigDecimal autresChargesNonCour = soldeCharge.apply("658");
        dto.getChargesNonCourantes().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("658", "Autres charges non courantes", autresChargesNonCour));

        BigDecimal dotationsNonCour = soldeCharge.apply("659");
        dto.getChargesNonCourantes().getLignes().add(new CpcOfficielDTO.LigneCpcDTO("659", "Dotations non courantes aux amort. et provisions", dotationsNonCour));

        BigDecimal totalIX = vnaImmo.add(subAccordees).add(autresChargesNonCour).add(dotationsNonCour);
        dto.getChargesNonCourantes().setTotal(totalIX);

        // X. RÉSULTAT NON COURANT (VIII - IX)
        BigDecimal resultatNonCourant = totalVIII.subtract(totalIX);
        dto.setResultatNonCourant(resultatNonCourant);

        // XI. RÉSULTAT AVANT IMPÔTS (VII + X)
        BigDecimal resultatAvantImpots = resultatCourant.add(resultatNonCourant);
        dto.setResultatAvantImpots(resultatAvantImpots);

        // XII. IMPÔT SUR LES RÉSULTATS (Rubrique 67)
        BigDecimal impotResultat = soldeCharge.apply("67");
        dto.setImpotSurResultats(impotResultat);

        // XIII. RÉSULTAT NET (XI - XII)
        BigDecimal resultatNet = resultatAvantImpots.subtract(impotResultat);
        dto.setResultatNet(resultatNet);

        // Totaux de synthèse
        BigDecimal totalProduits = totalI.add(totalIV).add(totalVIII);
        BigDecimal totalCharges = totalII.add(totalV).add(totalIX).add(impotResultat);
        dto.setTotalProduits(totalProduits);
        dto.setTotalCharges(totalCharges);

        return dto;
    }

    // =========================================================================
    // EXPORT FEC (FICHIER DES ÉCRITURES COMPTABLES) NORMALISÉ DGI 18 COLONNES
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] exporterFecDgi(LocalDate dateDebut, LocalDate dateFin, String separateurParam) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();

        String sep = (separateurParam != null && !separateurParam.isEmpty()) ? separateurParam : "\t";

        List<EcritureComptable> ecritures = ecritureRepository.findByPointDeVenteIdAndDateEcritureBetweenOrderByDateEcritureAsc(
                tenantId, dDebut, dFin);

        DateTimeFormatter dgiDateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        StringBuilder sb = new StringBuilder();
        // BOM UTF-8
        sb.append("\uFEFF");

        // 18 colonnes légales DGI
        String[] headers = {
            "JournalCode", "JournalLib", "EcritureNum", "EcritureDate",
            "CompteNum", "CompteLib", "CompAuxNum", "CompAuxLib",
            "PieceRef", "PieceDate", "EcritureLib", "Debit", "Credit",
            "EcritureLet", "DateLet", "ValidDate", "Montantdevise", "Idevise"
        };
        sb.append(String.join(sep, headers)).append("\r\n");

        for (EcritureComptable e : ecritures) {
            String journalCode = (e.getJournal() != null && e.getJournal().getCode() != null) ? e.getJournal().getCode() : "OD";
            String journalLib = (e.getJournal() != null && e.getJournal().getLibelle() != null) ? e.getJournal().getLibelle().replace(sep, " ") : "Opérations Diverses";
            String ecritureNum = e.getNumeroPiece() != null ? e.getNumeroPiece() : "ECR-" + e.getId();
            String ecritureDate = e.getDateEcriture() != null ? e.getDateEcriture().format(dgiDateFmt) : LocalDate.now().format(dgiDateFmt);
            String pieceRef = e.getReferencePiece() != null ? e.getReferencePiece() : ecritureNum;
            String pieceDate = ecritureDate;
            String validDate = (e.getDateCreation() != null) ? e.getDateCreation().toLocalDate().format(dgiDateFmt) : ecritureDate;
            String ecritureLib = e.getLibelle() != null ? e.getLibelle().replace(sep, " ") : "";

            if (e.getLignes() != null) {
                for (LigneEcriture l : e.getLignes()) {
                    String compteNum = (l.getCompte() != null && l.getCompte().getNumeroCompte() != null) ? l.getCompte().getNumeroCompte() : "";
                    String compteLib = (l.getCompte() != null && l.getCompte().getLibelle() != null) ? l.getCompte().getLibelle().replace(sep, " ") : "";

                    // Auxiliaire Tiers (Clients 3421, Fournisseurs 4411)
                    String compAuxNum = "";
                    String compAuxLib = "";
                    if (compteNum.startsWith("3421") || compteNum.startsWith("4411")) {
                        compAuxNum = (l.getReferenceLigne() != null && !l.getReferenceLigne().isEmpty()) ? l.getReferenceLigne() : pieceRef;
                        compAuxLib = (l.getLibelleLigne() != null && !l.getLibelleLigne().isEmpty()) ? l.getLibelleLigne().replace(sep, " ") : ecritureLib;
                    }

                    String ligneLib = (l.getLibelleLigne() != null && !l.getLibelleLigne().isEmpty())
                            ? l.getLibelleLigne().replace(sep, " ") : ecritureLib;

                    BigDecimal deb = (l.getDebit() != null) ? l.getDebit() : BigDecimal.ZERO;
                    BigDecimal cred = (l.getCredit() != null) ? l.getCredit() : BigDecimal.ZERO;

                    String debitStr = deb.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
                    String creditStr = cred.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();

                    String lettrage = (l.getLettrage() != null) ? l.getLettrage() : "";
                    String dateLet = (l.getLettrage() != null && !l.getLettrage().isEmpty()) ? validDate : "";

                    String montantDevise = "";
                    String iDevise = "MAD";

                    sb.append(journalCode).append(sep)
                      .append(journalLib).append(sep)
                      .append(ecritureNum).append(sep)
                      .append(ecritureDate).append(sep)
                      .append(compteNum).append(sep)
                      .append(compteLib).append(sep)
                      .append(compAuxNum).append(sep)
                      .append(compAuxLib).append(sep)
                      .append(pieceRef).append(sep)
                      .append(pieceDate).append(sep)
                      .append(ligneLib).append(sep)
                      .append(debitStr).append(sep)
                      .append(creditStr).append(sep)
                      .append(lettrage).append(sep)
                      .append(dateLet).append(sep)
                      .append(validDate).append(sep)
                      .append(montantDevise).append(sep)
                      .append(iDevise).append("\r\n");
                }
            }
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // =========================================================================
    // AUDIT ET CONTRÔLE DE CONFORMITÉ FEC MAROCAIN (ART. 145 DU CGI)
    // =========================================================================

    @Transactional(readOnly = true)
    public AuditFecReportDTO auditerConformiteFec(LocalDate dateDebut, LocalDate dateFin) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();

        List<EcritureComptable> ecritures = ecritureRepository.findByPointDeVenteIdAndDateEcritureBetweenOrderByDateEcritureAsc(
                tenantId, dDebut, dFin);

        AuditFecReportDTO audit = new AuditFecReportDTO();
        audit.setDateDebut(dDebut);
        audit.setDateFin(dFin);
        audit.setTotalEcritures(ecritures.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int totalLignes = 0;
        int nbBrouillons = 0;
        int nbComptesSansLibelle = 0;
        int nbAuxiliairesManquants = 0;

        Set<String> numerosPiecesUniques = new HashSet<>();
        int nbDoublonsPieces = 0;

        for (EcritureComptable e : ecritures) {
            if (!Boolean.TRUE.equals(e.getValidee())) {
                nbBrouillons++;
            }

            String numPiece = e.getNumeroPiece() != null ? e.getNumeroPiece().trim() : ("ID-" + e.getId());
            if (numerosPiecesUniques.contains(numPiece)) {
                nbDoublonsPieces++;
            } else {
                numerosPiecesUniques.add(numPiece);
            }

            if (e.getLignes() != null) {
                totalLignes += e.getLignes().size();
                for (LigneEcriture l : e.getLignes()) {
                    BigDecimal deb = (l.getDebit() != null) ? l.getDebit() : BigDecimal.ZERO;
                    BigDecimal cred = (l.getCredit() != null) ? l.getCredit() : BigDecimal.ZERO;
                    totalDebit = totalDebit.add(deb);
                    totalCredit = totalCredit.add(cred);

                    if (l.getCompte() == null || l.getCompte().getLibelle() == null || l.getCompte().getLibelle().trim().isEmpty()) {
                        nbComptesSansLibelle++;
                    }

                    String cNum = (l.getCompte() != null && l.getCompte().getNumeroCompte() != null) ? l.getCompte().getNumeroCompte() : "";
                    if ((cNum.startsWith("3421") || cNum.startsWith("4411")) &&
                            (l.getReferenceLigne() == null || l.getReferenceLigne().trim().isEmpty()) &&
                            (e.getReferencePiece() == null || e.getReferencePiece().trim().isEmpty())) {
                        nbAuxiliairesManquants++;
                    }
                }
            }
        }

        audit.setTotalLignes(totalLignes);
        audit.setTotalDebit(totalDebit);
        audit.setTotalCredit(totalCredit);
        BigDecimal ecart = totalDebit.subtract(totalCredit).abs();
        audit.setEcartEquilibre(ecart);
        boolean estEquilibre = ecart.compareTo(new BigDecimal("0.01")) < 0;
        audit.setEstParfaitementEquilibre(estEquilibre);
        audit.setNbEcrituresBrouillons(nbBrouillons);
        audit.setNbRupturesSequence(nbDoublonsPieces);
        audit.setNbComptesSansLibelle(nbComptesSansLibelle);
        audit.setNbAuxiliairesManquants(nbAuxiliairesManquants);

        int score = 100;

        // Contrôle 1 : Équilibre comptable parfait
        if (!estEquilibre) {
            score -= 35;
            audit.getAnomaliesBloquantes().add("Déséquilibre comptable bloquant : Écart Débit/Crédit de " + ecart + " MAD.");
        } else {
            audit.getPointsDeControleValides().add("Équilibre général rigoureux : Débit = Crédit = " + totalDebit.setScale(2, java.math.RoundingMode.HALF_UP) + " MAD.");
        }

        // Contrôle 2 : Statut des écritures (Intangibilité Art. 145 CGI)
        if (nbBrouillons > 0) {
            score -= 25;
            audit.getAnomaliesBloquantes().add(nbBrouillons + " écriture(s) en statut BROUILLON non validée(s). Les écritures doivent être validées pour un FEC officiel.");
        } else if (ecritures.size() > 0) {
            audit.getPointsDeControleValides().add("Intangibilité vérifiée : 100% des écritures sont validées et verrouillées.");
        }

        // Contrôle 3 : Continuité et unicité des pièces
        if (nbDoublonsPieces > 0) {
            score -= 15;
            audit.getAvertissements().add(nbDoublonsPieces + " numéro(s) de pièces en double ou incohérents détectés.");
        } else if (ecritures.size() > 0) {
            audit.getPointsDeControleValides().add("Séquentialité des pièces respectée sans doublon de numérotation.");
        }

        // Contrôle 4 : Intégrité des comptes
        if (nbComptesSansLibelle > 0) {
            score -= 10;
            audit.getAvertissements().add(nbComptesSansLibelle + " ligne(s) sans compte ou sans libellé de compte.");
        } else if (totalLignes > 0) {
            audit.getPointsDeControleValides().add("Plan de comptes PCGM respecté : Tous les comptes possèdent un libellé normalisé.");
        }

        // Contrôle 5 : Auxiliaires tiers
        if (nbAuxiliairesManquants > 0) {
            score -= 10;
            audit.getAvertissements().add(nbAuxiliairesManquants + " écriture(s) de tiers (3421/4411) sans référence auxiliaire.");
        } else if (totalLignes > 0) {
            audit.getPointsDeControleValides().add("Comptabilité auxiliaire : Références tiers renseignées.");
        }

        if (score < 0) score = 0;
        audit.setScoreConformitePourcentage(score);

        if (!audit.getAnomaliesBloquantes().isEmpty()) {
            audit.setStatutAudit("NON_CONFORME_BLOQUANT");
        } else if (!audit.getAvertissements().isEmpty()) {
            audit.setStatutAudit("AVERTISSEMENTS");
        } else {
            audit.setStatutAudit("CERTIFIE_CONFORME");
        }

        return audit;
    }
}
