package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.SensCompte;
import com.gestion.persistent.enums.TypeJournal;
import com.gestion.persistent.model.*;
import com.gestion.repository.CompteComptableRepository;
import com.gestion.repository.EcritureComptableRepository;
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

    public ComptabiliteService(CompteComptableRepository compteRepository,
                               JournalComptableRepository journalRepository,
                               EcritureComptableRepository ecritureRepository,
                               LigneEcritureRepository ligneRepository) {
        this.compteRepository = compteRepository;
        this.journalRepository = journalRepository;
        this.ecritureRepository = ecritureRepository;
        this.ligneRepository = ligneRepository;
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
        return journaux.stream().map(this::toJournalDto).collect(Collectors.toList());
    }

    public JournalComptableDTO creerJournal(JournalComptableDTO dto) {
        Long tenantId = getTenantId();
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Le code journal est obligatoire (ex: VT, AC, BQ)");
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

    public void initJournauxParDefaut(Long tenantId) {
        List<JournalComptable> defauts = List.of(
            new JournalComptable("VT", "Journal des Ventes", TypeJournal.VENTES, tenantId),
            new JournalComptable("AC", "Journal des Achats", TypeJournal.ACHATS, tenantId),
            new JournalComptable("BQ", "Journal de Banque", TypeJournal.BANQUE, tenantId),
            new JournalComptable("CA", "Journal de Caisse", TypeJournal.CAISSE, tenantId),
            new JournalComptable("OD", "Journal des Opérations Diverses", TypeJournal.OPERATIONS_DIVERSES, tenantId)
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

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journal);
        ecriture.setDateEcriture(dto.getDateEcriture() != null ? dto.getDateEcriture() : LocalDate.now());
        ecriture.setLibelle(dto.getLibelle() != null ? dto.getLibelle() : "Écriture du " + ecriture.getDateEcriture());
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
        BigDecimal tvaDeductible = BigDecimal.ZERO;

        for (LigneEcriture l : lignes) {
            if (l.getCompte() != null && l.getCompte().getNumeroCompte() != null) {
                String num = l.getCompte().getNumeroCompte();
                // TVA collectée / facturée : compte 4455 (Crédit - Débit)
                if (num.startsWith("4455")) {
                    BigDecimal cred = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;
                    BigDecimal deb = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                    tvaCollectee = tvaCollectee.add(cred.subtract(deb));
                }
                // TVA récupérable / déductible : compte 3455 (Débit - Crédit)
                else if (num.startsWith("3455")) {
                    BigDecimal deb = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                    BigDecimal cred = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;
                    tvaDeductible = tvaDeductible.add(deb.subtract(cred));
                }
            }
        }

        return new DeclarationTvaDTO(dDebut, dFin, tvaCollectee, tvaDeductible);
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
}
