package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
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

    public ComptabiliteService(CompteComptableRepository compteRepository,
                               JournalComptableRepository journalRepository,
                               EcritureComptableRepository ecritureRepository,
                               LigneEcritureRepository ligneRepository,
                               ExerciceComptableRepository exerciceRepository) {
        this.compteRepository = compteRepository;
        this.journalRepository = journalRepository;
        this.ecritureRepository = ecritureRepository;
        this.ligneRepository = ligneRepository;
        this.exerciceRepository = exerciceRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    // =========================================================================
    // PLAN COMPTABLE
    // =========================================================================

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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
        return toEcritureDto(saved);
    }

    public EcritureComptableDTO validerEcriture(Long id) {
        EcritureComptable ecriture = ecritureRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Écriture introuvable: " + id));

        if (!ecriture.isEquilibree()) {
            throw new IllegalStateException("Impossible de valider une écriture déséquilibrée");
        }

        ecriture.setValidee(true);
        return toEcritureDto(ecritureRepository.save(ecriture));
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
        if (dto.getNumeroCompte() != null) {
            return compteRepository.findByNumeroCompteAndPointDeVenteId(dto.getNumeroCompte().trim(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable numéro: " + dto.getNumeroCompte()));
        }
        throw new IllegalArgumentException("Compte comptable non renseigné sur la ligne d'écriture");
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

        return toEcritureDto(ecritureRepository.save(ecriture));
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

        return toEcritureDto(ecritureRepository.save(ecriture));
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

        return toEcritureDto(ecritureRepository.save(ecriture));
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
        ecriture.setLibelle("Décaissement Fournisseur " + reglement.getModePaiement() + " (" + ref + ")");
        ecriture.setReferencePiece(ref);
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setNumeroPiece(genererNumeroPiece(journal, ecriture.getDateEcriture(), tenantId));
        ecriture.setValidee(true);

        // Débit Fournisseur
        ecriture.addLigne(new LigneEcriture(compteFournisseur, reglement.getMontant(), BigDecimal.ZERO, "Règlement dette fournisseur", tenantId));
        // Crédit Banque ou Caisse
        ecriture.addLigne(new LigneEcriture(compteTresorerie, BigDecimal.ZERO, reglement.getMontant(), "Décaissement trésorerie " + ref, tenantId));

        return toEcritureDto(ecritureRepository.save(ecriture));
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
            throw new IllegalArgumentException("Certaines lignes d'écritures sont introuvables.");
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
        return nouveauCode;
    }

    public void annulerLettrage(String codeLettrage) {
        Long tenantId = getTenantId();
        if (codeLettrage == null || codeLettrage.trim().isEmpty()) {
            throw new IllegalArgumentException("Code de lettrage manquant");
        }
        List<LigneEcriture> lignes = ligneRepository.findByLettrageAndPointDeVenteId(codeLettrage.trim().toUpperCase(), tenantId);
        for (LigneEcriture l : lignes) {
            l.setLettrage(null);
        }
        ligneRepository.saveAll(lignes);
    }

    public Map<String, Object> autoLettrage(String prefixCompte) {
        Long tenantId = getTenantId();
        String prefix = (prefixCompte != null && !prefixCompte.trim().isEmpty()) ? prefixCompte.trim() : "3421";
        List<LigneEcriture> nonLettrees = ligneRepository.findLignesNonLettrees(tenantId, prefix, LocalDate.of(2000, 1, 1), LocalDate.now().plusYears(1));

        List<LigneEcriture> debits = nonLettrees.stream().filter(l -> l.getDebit().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
        List<LigneEcriture> credits = nonLettrees.stream().filter(l -> l.getCredit().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());

        int countLettrees = 0;
        Set<Long> creditsUtilises = new HashSet<>();

        for (LigneEcriture deb : debits) {
            for (LigneEcriture cred : credits) {
                if (creditsUtilises.contains(cred.getId())) continue;

                if (deb.getDebit().compareTo(cred.getCredit()) == 0) {
                    boolean samePiece = deb.getReferenceLigne() != null && cred.getReferenceLigne() != null &&
                            deb.getReferenceLigne().equalsIgnoreCase(cred.getReferenceLigne());
                    boolean sameCompte = deb.getCompte() != null && cred.getCompte() != null &&
                            deb.getCompte().getId().equals(cred.getCompte().getId());

                    if (sameCompte || samePiece) {
                        String code = genererCodeLettrageSuivant(tenantId);
                        deb.setLettrage(code);
                        cred.setLettrage(code);
                        ligneRepository.save(deb);
                        ligneRepository.save(cred);
                        creditsUtilises.add(cred.getId());
                        countLettrees += 2;
                        break;
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("lignesLettrees", countLettrees);
        result.put("codesAttribues", countLettrees / 2);
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
}
