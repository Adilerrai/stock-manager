package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.DemandeRegularisationDTO;
import com.gestion.persistent.dto.RegularisationResultDTO;
import com.gestion.persistent.model.CompteComptable;
import com.gestion.persistent.model.EcritureComptable;
import com.gestion.persistent.model.JournalComptable;
import com.gestion.persistent.model.LigneEcriture;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.CompteComptableRepository;
import com.gestion.repository.EcritureComptableRepository;
import com.gestion.repository.ExerciceComptableRepository;
import com.gestion.repository.JournalComptableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class InventaireComptableService {

    private final EcritureComptableRepository ecritureRepository;
    private final JournalComptableRepository journalRepository;
    private final CompteComptableRepository compteRepository;
    private final ExerciceComptableRepository exerciceRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    public InventaireComptableService(EcritureComptableRepository ecritureRepository,
                                     JournalComptableRepository journalRepository,
                                     CompteComptableRepository compteRepository,
                                     ExerciceComptableRepository exerciceRepository,
                                     ClientRepository clientRepository,
                                     AuditService auditService) {
        this.ecritureRepository = ecritureRepository;
        this.journalRepository = journalRepository;
        this.compteRepository = compteRepository;
        this.exerciceRepository = exerciceRepository;
        this.clientRepository = clientRepository;
        this.auditService = auditService;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // 1. CHARGES CONSTATÉES D'AVANCE (CCA - Compte 3491)
    // =========================================================================

    public RegularisationResultDTO creerCCA(DemandeRegularisationDTO dto) {
        Long tenantId = getTenantId();
        validerMontant(dto.getMontant());

        LocalDate dateCloture = dto.getDateEcriture() != null 
                ? dto.getDateEcriture() 
                : LocalDate.of(LocalDate.now().getYear(), 12, 31);

        verifierExerciceNonCloture(dateCloture, tenantId);

        JournalComptable journalOd = getJournalOD(tenantId);
        CompteComptable compteCca = getOrCreateCompte("34910000", "Charges constatées d'avance", 3, tenantId);
        
        String compteChargeNumero = (dto.getCompteChargeProduit() != null && !dto.getCompteChargeProduit().isBlank())
                ? dto.getCompteChargeProduit().trim()
                : "61311000";
        CompteComptable compteCharge = getOrCreateCompte(compteChargeNumero, "Charge à régulariser (CCA)", 6, tenantId);

        String libelle = (dto.getLibelle() != null && !dto.getLibelle().isBlank())
                ? dto.getLibelle()
                : "Régularisation CCA au " + dateCloture;

        // 1. Écriture de régularisation CCA au 31/12 (Débit 3491 / Crédit 6xxx)
        EcritureComptable ecrCCA = new EcritureComptable();
        ecrCCA.setJournal(journalOd);
        ecrCCA.setDateEcriture(dateCloture);
        ecrCCA.setLibelle(libelle);
        ecrCCA.setReferencePiece("REGUL-CCA-" + dateCloture.getYear());
        ecrCCA.setPointDeVenteId(tenantId);
        ecrCCA.setValidee(true);
        ecrCCA.setNumeroPiece(genererNumeroPiece(journalOd, dateCloture, tenantId));

        // Débit 3491
        LigneEcriture ligneDebit = new LigneEcriture();
        ligneDebit.setCompte(compteCca);
        ligneDebit.setDebit(dto.getMontant());
        ligneDebit.setCredit(BigDecimal.ZERO);
        ligneDebit.setLibelleLigne("CCA: " + libelle);
        ligneDebit.setPointDeVenteId(tenantId);
        ecrCCA.addLigne(ligneDebit);

        // Crédit 6xxx
        LigneEcriture ligneCredit = new LigneEcriture();
        ligneCredit.setCompte(compteCharge);
        ligneCredit.setDebit(BigDecimal.ZERO);
        ligneCredit.setCredit(dto.getMontant());
        ligneCredit.setLibelleLigne("Annulation quote-part charge N: " + libelle);
        ligneCredit.setPointDeVenteId(tenantId);
        ecrCCA.addLigne(ligneCredit);

        ecrCCA.recalculerTotaux();
        EcritureComptable savedCCA = ecritureRepository.save(ecrCCA);
        auditService.logCreation("INVENTAIRE_CCA", savedCCA.getId(), "Création CCA: " + libelle + " (" + dto.getMontant() + " MAD)");

        RegularisationResultDTO resultat = new RegularisationResultDTO();
        resultat.setType("CCA");
        resultat.setMontant(dto.getMontant());
        resultat.setIdEcritureInventaire(savedCCA.getId());
        resultat.setNumeroPieceInventaire(savedCCA.getNumeroPiece());
        resultat.setDateInventaire(savedCCA.getDateEcriture());

        // 2. Extourne automatique au 01/01/N+1 (Débit 6xxx / Crédit 3491)
        if (Boolean.TRUE.equals(dto.getExtourneAutomatique())) {
            LocalDate dateExtourne = dto.getDateExtourne() != null 
                    ? dto.getDateExtourne() 
                    : dateCloture.plusDays(1); // 01/01 N+1

            EcritureComptable ecrExtourne = new EcritureComptable();
            ecrExtourne.setJournal(journalOd);
            ecrExtourne.setDateEcriture(dateExtourne);
            ecrExtourne.setLibelle("Extourne " + libelle);
            ecrExtourne.setReferencePiece("EXT-CCA-" + dateExtourne.getYear());
            ecrExtourne.setPointDeVenteId(tenantId);
            ecrExtourne.setValidee(true);
            ecrExtourne.setNumeroPiece(genererNumeroPiece(journalOd, dateExtourne, tenantId));

            LigneEcriture ligneExtDebit = new LigneEcriture();
            ligneExtDebit.setCompte(compteCharge);
            ligneExtDebit.setDebit(dto.getMontant());
            ligneExtDebit.setCredit(BigDecimal.ZERO);
            ligneExtDebit.setLibelleLigne("Extourne CCA: " + libelle);
            ligneExtDebit.setPointDeVenteId(tenantId);
            ecrExtourne.addLigne(ligneExtDebit);

            LigneEcriture ligneExtCredit = new LigneEcriture();
            ligneExtCredit.setCompte(compteCca);
            ligneExtCredit.setDebit(BigDecimal.ZERO);
            ligneExtCredit.setCredit(dto.getMontant());
            ligneExtCredit.setLibelleLigne("Soldage compte CCA: " + libelle);
            ligneExtCredit.setPointDeVenteId(tenantId);
            ecrExtourne.addLigne(ligneExtCredit);

            ecrExtourne.recalculerTotaux();
            EcritureComptable savedExtourne = ecritureRepository.save(ecrExtourne);
            auditService.logCreation("EXTOURNE_CCA", savedExtourne.getId(), "Extourne CCA générée au " + dateExtourne);

            resultat.setIdEcritureExtourne(savedExtourne.getId());
            resultat.setNumeroPieceExtourne(savedExtourne.getNumeroPiece());
            resultat.setDateExtourne(savedExtourne.getDateEcriture());
            resultat.setMessage("Régularisation CCA enregistrée avec extourne automatique au " + dateExtourne);
        } else {
            resultat.setMessage("Régularisation CCA enregistrée avec succès.");
        }

        return resultat;
    }

    // =========================================================================
    // 2. PRODUITS CONSTATÉS D'AVANCE (PCA - Compte 4491)
    // =========================================================================

    public RegularisationResultDTO creerPCA(DemandeRegularisationDTO dto) {
        Long tenantId = getTenantId();
        validerMontant(dto.getMontant());

        LocalDate dateCloture = dto.getDateEcriture() != null 
                ? dto.getDateEcriture() 
                : LocalDate.of(LocalDate.now().getYear(), 12, 31);

        verifierExerciceNonCloture(dateCloture, tenantId);

        JournalComptable journalOd = getJournalOD(tenantId);
        CompteComptable comptePca = getOrCreateCompte("44910000", "Produits constatés d'avance", 4, tenantId);
        
        String compteProduitNumero = (dto.getCompteChargeProduit() != null && !dto.getCompteChargeProduit().isBlank())
                ? dto.getCompteChargeProduit().trim()
                : "71110000";
        CompteComptable compteProduit = getOrCreateCompte(compteProduitNumero, "Produit à régulariser (PCA)", 7, tenantId);

        String libelle = (dto.getLibelle() != null && !dto.getLibelle().isBlank())
                ? dto.getLibelle()
                : "Régularisation PCA au " + dateCloture;

        // 1. Écriture de régularisation PCA au 31/12 (Débit 7xxx / Crédit 4491)
        EcritureComptable ecrPCA = new EcritureComptable();
        ecrPCA.setJournal(journalOd);
        ecrPCA.setDateEcriture(dateCloture);
        ecrPCA.setLibelle(libelle);
        ecrPCA.setReferencePiece("REGUL-PCA-" + dateCloture.getYear());
        ecrPCA.setPointDeVenteId(tenantId);
        ecrPCA.setValidee(true);
        ecrPCA.setNumeroPiece(genererNumeroPiece(journalOd, dateCloture, tenantId));

        // Débit 7xxx
        LigneEcriture ligneDebit = new LigneEcriture();
        ligneDebit.setCompte(compteProduit);
        ligneDebit.setDebit(dto.getMontant());
        ligneDebit.setCredit(BigDecimal.ZERO);
        ligneDebit.setLibelleLigne("Annulation quote-part produit N: " + libelle);
        ligneDebit.setPointDeVenteId(tenantId);
        ecrPCA.addLigne(ligneDebit);

        // Crédit 4491
        LigneEcriture ligneCredit = new LigneEcriture();
        ligneCredit.setCompte(comptePca);
        ligneCredit.setDebit(BigDecimal.ZERO);
        ligneCredit.setCredit(dto.getMontant());
        ligneCredit.setLibelleLigne("PCA: " + libelle);
        ligneCredit.setPointDeVenteId(tenantId);
        ecrPCA.addLigne(ligneCredit);

        ecrPCA.recalculerTotaux();
        EcritureComptable savedPCA = ecritureRepository.save(ecrPCA);
        auditService.logCreation("INVENTAIRE_PCA", savedPCA.getId(), "Création PCA: " + libelle + " (" + dto.getMontant() + " MAD)");

        RegularisationResultDTO resultat = new RegularisationResultDTO();
        resultat.setType("PCA");
        resultat.setMontant(dto.getMontant());
        resultat.setIdEcritureInventaire(savedPCA.getId());
        resultat.setNumeroPieceInventaire(savedPCA.getNumeroPiece());
        resultat.setDateInventaire(savedPCA.getDateEcriture());

        // 2. Extourne automatique au 01/01/N+1 (Débit 4491 / Crédit 7xxx)
        if (Boolean.TRUE.equals(dto.getExtourneAutomatique())) {
            LocalDate dateExtourne = dto.getDateExtourne() != null 
                    ? dto.getDateExtourne() 
                    : dateCloture.plusDays(1);

            EcritureComptable ecrExtourne = new EcritureComptable();
            ecrExtourne.setJournal(journalOd);
            ecrExtourne.setDateEcriture(dateExtourne);
            ecrExtourne.setLibelle("Extourne " + libelle);
            ecrExtourne.setReferencePiece("EXT-PCA-" + dateExtourne.getYear());
            ecrExtourne.setPointDeVenteId(tenantId);
            ecrExtourne.setValidee(true);
            ecrExtourne.setNumeroPiece(genererNumeroPiece(journalOd, dateExtourne, tenantId));

            LigneEcriture ligneExtDebit = new LigneEcriture();
            ligneExtDebit.setCompte(comptePca);
            ligneExtDebit.setDebit(dto.getMontant());
            ligneExtDebit.setCredit(BigDecimal.ZERO);
            ligneExtDebit.setLibelleLigne("Soldage compte PCA: " + libelle);
            ligneExtDebit.setPointDeVenteId(tenantId);
            ecrExtourne.addLigne(ligneExtDebit);

            LigneEcriture ligneExtCredit = new LigneEcriture();
            ligneExtCredit.setCompte(compteProduit);
            ligneExtCredit.setDebit(BigDecimal.ZERO);
            ligneExtCredit.setCredit(dto.getMontant());
            ligneExtCredit.setLibelleLigne("Extourne PCA: " + libelle);
            ligneExtCredit.setPointDeVenteId(tenantId);
            ecrExtourne.addLigne(ligneExtCredit);

            ecrExtourne.recalculerTotaux();
            EcritureComptable savedExtourne = ecritureRepository.save(ecrExtourne);
            auditService.logCreation("EXTOURNE_PCA", savedExtourne.getId(), "Extourne PCA générée au " + dateExtourne);

            resultat.setIdEcritureExtourne(savedExtourne.getId());
            resultat.setNumeroPieceExtourne(savedExtourne.getNumeroPiece());
            resultat.setDateExtourne(savedExtourne.getDateEcriture());
            resultat.setMessage("Régularisation PCA enregistrée avec extourne automatique au " + dateExtourne);
        } else {
            resultat.setMessage("Régularisation PCA enregistrée avec succès.");
        }

        return resultat;
    }

    // =========================================================================
    // 3. PROVISIONS POUR CLIENTS DOUTEUX (61964 / 3942)
    // =========================================================================

    public RegularisationResultDTO creerProvisionClientDouteux(
            Long clientId, 
            BigDecimal montantCreance, 
            BigDecimal pourcentageProvision, 
            LocalDate dateInventaire, 
            String motif) {

        Long tenantId = getTenantId();
        validerMontant(montantCreance);

        if (pourcentageProvision == null || pourcentageProvision.compareTo(BigDecimal.ZERO) <= 0 || pourcentageProvision.compareTo(BigDecimal.valueOf(100)) > 0) {
            pourcentageProvision = BigDecimal.valueOf(100);
        }

        BigDecimal montantProvision = montantCreance.multiply(pourcentageProvision).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        LocalDate dateCloture = dateInventaire != null ? dateInventaire : LocalDate.of(LocalDate.now().getYear(), 12, 31);
        verifierExerciceNonCloture(dateCloture, tenantId);

        JournalComptable journalOd = getJournalOD(tenantId);
        CompteComptable compteDotation = getOrCreateCompte("61964000", "D.E.A. pour dépréciation des créances de l'actif circulant", 6, tenantId);
        CompteComptable compteProvision = getOrCreateCompte("39420000", "Provisions pour dépréciation des clients et comptes rattachés", 3, tenantId);

        String nomClient = "Client #" + clientId;
        if (clientId != null) {
            nomClient = clientRepository.findById(clientId).map(c -> c.getNomComplet() != null ? c.getNomComplet() : c.getNom()).orElse(nomClient);
        }

        String libelle = "Provision pour dépréciation " + nomClient + " (" + pourcentageProvision + "% de " + montantCreance + " MAD)";
        if (motif != null && !motif.isBlank()) {
            libelle += " - " + motif;
        }

        EcritureComptable ecr = new EcritureComptable();
        ecr.setJournal(journalOd);
        ecr.setDateEcriture(dateCloture);
        ecr.setLibelle(libelle);
        ecr.setReferencePiece("PROV-CLI-" + dateCloture.getYear());
        ecr.setPointDeVenteId(tenantId);
        ecr.setValidee(true);
        ecr.setNumeroPiece(genererNumeroPiece(journalOd, dateCloture, tenantId));

        // Débit 61964
        LigneEcriture lDebit = new LigneEcriture();
        lDebit.setCompte(compteDotation);
        lDebit.setDebit(montantProvision);
        lDebit.setCredit(BigDecimal.ZERO);
        lDebit.setLibelleLigne(libelle);
        lDebit.setPointDeVenteId(tenantId);
        ecr.addLigne(lDebit);

        // Crédit 3942
        LigneEcriture lCredit = new LigneEcriture();
        lCredit.setCompte(compteProvision);
        lCredit.setDebit(BigDecimal.ZERO);
        lCredit.setCredit(montantProvision);
        lCredit.setLibelleLigne(libelle);
        lCredit.setPointDeVenteId(tenantId);
        ecr.addLigne(lCredit);

        ecr.recalculerTotaux();
        EcritureComptable saved = ecritureRepository.save(ecr);
        auditService.logCreation("PROVISION_CLIENT", saved.getId(), "Création provision client douteux: " + libelle);

        RegularisationResultDTO res = new RegularisationResultDTO();
        res.setType("PROVISION_CLIENT");
        res.setMontant(montantProvision);
        res.setIdEcritureInventaire(saved.getId());
        res.setNumeroPieceInventaire(saved.getNumeroPiece());
        res.setDateInventaire(saved.getDateEcriture());
        res.setMessage("Dotation aux provisions clients comptabilisée avec succès pour un montant de " + montantProvision + " MAD.");
        return res;
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private void validerMontant(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de la régularisation doit être strictement supérieur à zéro.");
        }
    }

    private void verifierExerciceNonCloture(LocalDate date, Long tenantId) {
        if (exerciceRepository.isDateInExerciceCloture(date, tenantId)) {
            throw new IllegalStateException("Impossible d'enregistrer la régularisation : l'exercice pour le " + date + " est clôturé.");
        }
    }

    private JournalComptable getJournalOD(Long tenantId) {
        return journalRepository.findByCodeAndPointDeVenteId("OD", tenantId)
                .orElseGet(() -> {
                    JournalComptable j = new JournalComptable();
                    j.setCode("OD");
                    j.setLibelle("Opérations Diverses");
                    j.setTypeJournal(com.gestion.persistent.enums.TypeJournal.OPERATIONS_DIVERSES);
                    j.setActif(true);
                    j.setPointDeVenteId(tenantId);
                    return journalRepository.save(j);
                });
    }

    private CompteComptable getOrCreateCompte(String numero, String libelle, int classe, Long tenantId) {
        return compteRepository.findByNumeroCompteAndPointDeVenteId(numero, tenantId)
                .orElseGet(() -> {
                    CompteComptable c = new CompteComptable();
                    c.setNumeroCompte(numero);
                    c.setLibelle(libelle);
                    c.setClasse(classe);
                    c.setActif(true);
                    c.setPointDeVenteId(tenantId);
                    return compteRepository.save(c);
                });
    }

    private String genererNumeroPiece(JournalComptable journal, LocalDate date, Long tenantId) {
        int annee = date.getYear();
        long count = ecritureRepository.countByPointDeVenteIdAndJournal(tenantId, journal);
        return String.format("%s-%d-%05d", journal.getCode(), annee, count + 1);
    }
}
