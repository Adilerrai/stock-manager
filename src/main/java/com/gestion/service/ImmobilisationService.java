package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.SensCompte;
import com.gestion.persistent.enums.StatutImmobilisation;
import com.gestion.persistent.enums.TypeAmortissement;
import com.gestion.persistent.enums.TypeJournal;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ImmobilisationService {

    private final ImmobilisationRepository immobilisationRepository;
    private final LignePlanAmortissementRepository lignePlanRepository;
    private final CompteComptableRepository compteRepository;
    private final JournalComptableRepository journalRepository;
    private final EcritureComptableRepository ecritureRepository;

    public ImmobilisationService(ImmobilisationRepository immobilisationRepository,
                                 LignePlanAmortissementRepository lignePlanRepository,
                                 CompteComptableRepository compteRepository,
                                 JournalComptableRepository journalRepository,
                                 EcritureComptableRepository ecritureRepository) {
        this.immobilisationRepository = immobilisationRepository;
        this.lignePlanRepository = lignePlanRepository;
        this.compteRepository = compteRepository;
        this.journalRepository = journalRepository;
        this.ecritureRepository = ecritureRepository;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // CRUD & GESTION DES IMMOBILISATIONS
    // =========================================================================

    public ImmobilisationDTO creerImmobilisation(ImmobilisationDTO dto) {
        Long tenantId = getTenantId();

        Immobilisation immo = new Immobilisation();
        immo.setPointDeVenteId(tenantId);
        immo.setDateCreation(LocalDateTime.now());

        // Code actif
        if (dto.getCode() != null && !dto.getCode().trim().isEmpty()) {
            immo.setCode(dto.getCode().trim().toUpperCase());
        } else {
            immo.setCode(genererCodeImmo(tenantId));
        }

        immo.setDesignation(dto.getDesignation() != null ? dto.getDesignation() : "Immobilisation " + immo.getCode());
        immo.setNumeroFacture(dto.getNumeroFacture());
        immo.setFournisseurNom(dto.getFournisseurNom());

        LocalDate dateAcq = dto.getDateAcquisition() != null ? dto.getDateAcquisition() : LocalDate.now();
        LocalDate dateServ = dto.getDateMiseEnService() != null ? dto.getDateMiseEnService() : dateAcq;
        immo.setDateAcquisition(dateAcq);
        immo.setDateMiseEnService(dateServ);

        BigDecimal valAcq = dto.getValeurAcquisition() != null ? dto.getValeurAcquisition() : BigDecimal.ZERO;
        immo.setValeurAcquisition(valAcq);
        immo.setTvaDeductible(dto.getTvaDeductible() != null ? dto.getTvaDeductible() : BigDecimal.ZERO);
        immo.setValeurResiduelle(dto.getValeurResiduelle() != null ? dto.getValeurResiduelle() : BigDecimal.ZERO);

        immo.setDureeAnnees(dto.getDureeAnnees() != null && dto.getDureeAnnees() > 0 ? dto.getDureeAnnees() : 5);
        immo.setTypeAmortissement(dto.getTypeAmortissement() != null ? dto.getTypeAmortissement() : TypeAmortissement.LINEAIRE);
        immo.setStatut(StatutImmobilisation.EN_SERVICE);

        // Résolution des comptes comptables PCGM
        immo.setCompteImmobilisation(resoudreCompte(dto.getCompteImmobilisationId(), dto.getCompteImmobilisationNumero(), "23320000", "Matériel et outillage", 2, SensCompte.DEBIT, tenantId));
        immo.setCompteAmortissement(resoudreCompte(dto.getCompteAmortissementId(), dto.getCompteAmortissementNumero(), "28332000", "Amortissements du matériel et outillage", 2, SensCompte.CREDIT, tenantId));
        immo.setCompteDotation(resoudreCompte(dto.getCompteDotationId(), dto.getCompteDotationNumero(), "61933000", "D.E.A. du matériel et outillage", 6, SensCompte.DEBIT, tenantId));
        immo.setCompteProduitCession(findOrCreateCompte("75130000", "Produits de cessions des immobilisations corporelles", 7, SensCompte.CREDIT, tenantId));
        immo.setCompteVna(findOrCreateCompte("65130000", "V.N.A. des immobilisations corporelles cédées", 6, SensCompte.DEBIT, tenantId));

        // Coefficients et taux fiscaux marocains
        configurerTauxEtCoefficients(immo);

        // Initialisation de la VNC
        immo.setCumulAmortissements(BigDecimal.ZERO);
        immo.setValeurNetteComptable(valAcq);

        // Calcul et génération du plan d'amortissement
        List<LignePlanAmortissement> plan = calculerPlanAmortissement(immo);
        immo.getLignesPlanAmortissement().clear();
        for (LignePlanAmortissement ligne : plan) {
            immo.getLignesPlanAmortissement().add(ligne);
            ligne.setImmobilisation(immo);
        }

        Immobilisation saved = immobilisationRepository.save(immo);
        return toImmobilisationDto(saved);
    }

    @Transactional(readOnly = true)
    public ImmobilisationDTO getImmobilisation(Long id) {
        Long tenantId = getTenantId();
        Immobilisation immo = immobilisationRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Immobilisation introuvable ID: " + id));
        return toImmobilisationDto(immo);
    }

    @Transactional(readOnly = true)
    public List<ImmobilisationDTO> getToutesImmobilisations() {
        Long tenantId = getTenantId();
        return immobilisationRepository.findByPointDeVenteIdOrderByCodeAsc(tenantId).stream()
                .map(this::toImmobilisationDto)
                .collect(Collectors.toList());
    }

    public void supprimerImmobilisation(Long id) {
        Long tenantId = getTenantId();
        Immobilisation immo = immobilisationRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Immobilisation introuvable ID: " + id));

        boolean hasComptabilisee = immo.getLignesPlanAmortissement().stream().anyMatch(LignePlanAmortissement::getComptabilisee);
        if (hasComptabilisee) {
            throw new IllegalStateException("Impossible de supprimer une immobilisation dont les dotations ont déjà été comptabilisées.");
        }
        immobilisationRepository.delete(immo);
    }

    // =========================================================================
    // CALCUL DU PLAN D'AMORTISSEMENT (LINÉAIRE & DÉGRESSIF MAROCAIN)
    // =========================================================================

    public List<LignePlanAmortissement> calculerPlanAmortissement(Immobilisation immo) {
        if (immo.getTypeAmortissement() == TypeAmortissement.NON_AMORTISSABLE) {
            return Collections.emptyList();
        }

        if (immo.getTypeAmortissement() == TypeAmortissement.DEGRESSIF_MAROCAIN) {
            return calculerPlanDegressifMarocain(immo);
        } else {
            return calculerPlanLineaire(immo);
        }
    }

    private List<LignePlanAmortissement> calculerPlanLineaire(Immobilisation immo) {
        List<LignePlanAmortissement> lignes = new ArrayList<>();
        BigDecimal base = immo.getValeurAcquisition().subtract(immo.getValeurResiduelle());
        if (base.compareTo(BigDecimal.ZERO) <= 0) return lignes;

        int duree = immo.getDureeAnnees();
        BigDecimal taux = BigDecimal.ONE.divide(new BigDecimal(duree), 6, RoundingMode.HALF_UP);
        LocalDate dateServ = immo.getDateMiseEnService();
        int anneeDebut = dateServ.getYear();
        int mois1 = 12 - dateServ.getMonthValue() + 1; // Prorata temporis marocain (mois inclus)

        BigDecimal cumul = BigDecimal.ZERO;
        int anneeCourante = anneeDebut;

        // Année 1 avec prorata
        BigDecimal dot1 = base.multiply(taux)
                .multiply(new BigDecimal(mois1))
                .divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
        cumul = cumul.add(dot1);
        BigDecimal vnc1 = base.subtract(cumul);

        lignes.add(creerLigne(immo, anneeCourante, mois1, base, taux, dot1, cumul, vnc1, false));

        // Années intermédiaires
        int anneesPleines = (mois1 == 12) ? duree - 1 : duree - 1;
        for (int i = 0; i < anneesPleines; i++) {
            anneeCourante++;
            BigDecimal dot = base.multiply(taux).setScale(2, RoundingMode.HALF_UP);
            cumul = cumul.add(dot);
            BigDecimal vnc = base.subtract(cumul);
            lignes.add(creerLigne(immo, anneeCourante, 12, base, taux, dot, cumul, vnc, false));
        }

        // Dernière année complémentaire (si prorata en année 1)
        if (mois1 < 12) {
            anneeCourante++;
            int moisFin = 12 - mois1;
            BigDecimal dotFin = base.subtract(cumul); // Solde résiduel pour atteindre exactement 0
            if (dotFin.compareTo(BigDecimal.ZERO) > 0) {
                cumul = cumul.add(dotFin);
                lignes.add(creerLigne(immo, anneeCourante, moisFin, base, taux, dotFin, cumul, BigDecimal.ZERO, false));
            }
        }

        return lignes;
    }

    private List<LignePlanAmortissement> calculerPlanDegressifMarocain(Immobilisation immo) {
        List<LignePlanAmortissement> lignes = new ArrayList<>();
        BigDecimal valeurOrigine = immo.getValeurAcquisition().subtract(immo.getValeurResiduelle());
        if (valeurOrigine.compareTo(BigDecimal.ZERO) <= 0) return lignes;

        int duree = immo.getDureeAnnees();
        BigDecimal coeff = immo.getCoefficientDegressif();
        BigDecimal tauxLineaireInitial = BigDecimal.ONE.divide(new BigDecimal(duree), 6, RoundingMode.HALF_UP);
        BigDecimal tauxDegressif = tauxLineaireInitial.multiply(coeff);

        LocalDate dateServ = immo.getDateMiseEnService();
        int anneeCourante = dateServ.getYear();
        int mois1 = 12 - dateServ.getMonthValue() + 1;

        BigDecimal vnaDebut = valeurOrigine;
        BigDecimal cumul = BigDecimal.ZERO;
        boolean basculeLineaire = false;

        // Année 1 : Prorata temporis dégressif
        BigDecimal dot1 = vnaDebut.multiply(tauxDegressif)
                .multiply(new BigDecimal(mois1))
                .divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
        cumul = cumul.add(dot1);
        BigDecimal vnaFin1 = valeurOrigine.subtract(cumul);

        lignes.add(creerLigne(immo, anneeCourante, mois1, vnaDebut, tauxDegressif, dot1, cumul, vnaFin1, false));
        vnaDebut = vnaFin1;

        // Années suivantes
        int totalAnnees = (mois1 == 12) ? duree : duree + 1;
        for (int i = 2; i <= totalAnnees; i++) {
            anneeCourante++;
            int anneesRestantes = totalAnnees - i + 1;
            BigDecimal tauxLineaireResiduel = BigDecimal.ONE.divide(new BigDecimal(anneesRestantes), 6, RoundingMode.HALF_UP);

            BigDecimal tauxApplique;
            BigDecimal dotation;

            // Règle fiscale marocaine : bascule sur le linéaire dès que Taux Dégressif < Taux Linéaire Résiduel
            if (basculeLineaire || tauxDegressif.compareTo(tauxLineaireResiduel) < 0) {
                basculeLineaire = true;
                tauxApplique = tauxLineaireResiduel;
                if (anneesRestantes == 1) {
                    dotation = vnaDebut; // Dernière année : solde total
                } else {
                    dotation = vnaDebut.divide(new BigDecimal(anneesRestantes), 2, RoundingMode.HALF_UP);
                }
            } else {
                tauxApplique = tauxDegressif;
                dotation = vnaDebut.multiply(tauxDegressif).setScale(2, RoundingMode.HALF_UP);
            }

            cumul = cumul.add(dotation);
            BigDecimal vnaFin = valeurOrigine.subtract(cumul);
            if (vnaFin.compareTo(BigDecimal.ZERO) < 0) {
                dotation = dotation.add(vnaFin);
                cumul = valeurOrigine;
                vnaFin = BigDecimal.ZERO;
            }

            lignes.add(creerLigne(immo, anneeCourante, 12, vnaDebut, tauxApplique, dotation, cumul, vnaFin, basculeLineaire));
            vnaDebut = vnaFin;

            if (vnaDebut.compareTo(BigDecimal.ZERO) <= 0) break;
        }

        return lignes;
    }

    private LignePlanAmortissement creerLigne(Immobilisation immo, int annee, int mois, BigDecimal base,
                                              BigDecimal taux, BigDecimal dotation, BigDecimal cumul,
                                              BigDecimal vnc, boolean bascule) {
        LignePlanAmortissement l = new LignePlanAmortissement();
        l.setImmobilisation(immo);
        l.setAnnee(annee);
        l.setMoisAmortis(mois);
        l.setBaseCalcul(base);
        l.setTauxApplique(taux);
        l.setDotation(dotation);
        l.setCumulAmortissement(cumul);
        l.setValeurNetteFin(vnc);
        l.setModeLineaireBascule(bascule);
        l.setComptabilisee(false);
        l.setPointDeVenteId(immo.getPointDeVenteId());
        return l;
    }

    private void configurerTauxEtCoefficients(Immobilisation immo) {
        int duree = immo.getDureeAnnees();
        if (duree <= 0) duree = 5;

        BigDecimal txLin = BigDecimal.ONE.divide(new BigDecimal(duree), 4, RoundingMode.HALF_UP);
        immo.setTauxAmortissement(txLin);

        if (immo.getTypeAmortissement() == TypeAmortissement.DEGRESSIF_MAROCAIN) {
            // Art. 10 CGI Maroc : Coefficients légaux
            BigDecimal coeff;
            if (duree <= 4) {
                coeff = new BigDecimal("1.50");
            } else if (duree <= 6) {
                coeff = new BigDecimal("2.00");
            } else {
                coeff = new BigDecimal("3.00");
            }
            immo.setCoefficientDegressif(coeff);
            immo.setTauxAmortissement(txLin.multiply(coeff));
        } else {
            immo.setCoefficientDegressif(BigDecimal.ONE);
        }
    }

    // =========================================================================
    // GÉNÉRATION AUTOMATIQUE DES DOTATIONS D'INVENTAIRE (619x / 28xx)
    // =========================================================================

    public EcritureComptableDTO genererDotationsInventaire(int annee) {
        Long tenantId = getTenantId();

        List<LignePlanAmortissement> lignes = lignePlanRepository.findNonComptabiliseesParAnnee(tenantId, annee);
        if (lignes.isEmpty()) {
            throw new IllegalStateException("Aucune dotation non comptabilisée à générer pour l'exercice " + annee);
        }

        // Trouver ou créer le journal OD (Opérations Diverses)
        JournalComptable journalOd = journalRepository.findByCodeAndPointDeVenteId("OD", tenantId)
                .orElseGet(() -> {
                    JournalComptable j = new JournalComptable("OD", "Opérations Diverses", TypeJournal.OPERATIONS_DIVERSES, tenantId);
                    return journalRepository.save(j);
                });

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journalOd);
        ecriture.setDateEcriture(LocalDate.of(annee, 12, 31));
        ecriture.setLibelle("Dotations d'inventaire aux amortissements exercice " + annee);
        ecriture.setReferencePiece("DOT-" + annee);
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setValidee(true);

        String prefix = "OD-" + annee + "-";
        Long count = ecritureRepository.countByPrefixAndTenant(prefix, tenantId);
        ecriture.setNumeroPiece(String.format("%s%05d", prefix, (count != null ? count : 0) + 1));

        // Regrouper par compte dotation (619) et compte amortissement (28)
        Map<CompteComptable, BigDecimal> totalParCompteDotation = new HashMap<>();
        Map<CompteComptable, BigDecimal> totalParCompteAmortissement = new HashMap<>();

        for (LignePlanAmortissement ligne : lignes) {
            Immobilisation immo = ligne.getImmobilisation();
            if (immo.getStatut() == StatutImmobilisation.MIS_AU_REBUT) continue;

            BigDecimal dot = ligne.getDotation();
            if (dot.compareTo(BigDecimal.ZERO) <= 0) continue;

            totalParCompteDotation.merge(immo.getCompteDotation(), dot, BigDecimal::add);
            totalParCompteAmortissement.merge(immo.getCompteAmortissement(), dot, BigDecimal::add);
        }

        if (totalParCompteDotation.isEmpty()) {
            throw new IllegalStateException("Le montant total des dotations à comptabiliser est nul pour " + annee);
        }

        // Lignes DÉBIT : 619x (Charges de dotations d'exploitation)
        for (Map.Entry<CompteComptable, BigDecimal> entry : totalParCompteDotation.entrySet()) {
            LigneEcriture ligneDebit = new LigneEcriture(
                    entry.getKey(),
                    entry.getValue(),
                    BigDecimal.ZERO,
                    "Dotation aux amortissements " + entry.getKey().getLibelle(),
                    tenantId
            );
            ecriture.addLigne(ligneDebit);
        }

        // Lignes CRÉDIT : 28xx (Amortissements des immobilisations)
        for (Map.Entry<CompteComptable, BigDecimal> entry : totalParCompteAmortissement.entrySet()) {
            LigneEcriture ligneCredit = new LigneEcriture(
                    entry.getKey(),
                    entry.getValue(),
                    entry.getValue(),
                    "Amortissement annuel " + entry.getKey().getLibelle(),
                    tenantId
            );
            // Corriger sens : Débit = 0, Crédit = montant
            ligneCredit.setDebit(BigDecimal.ZERO);
            ecriture.addLigne(ligneCredit);
        }

        if (!ecriture.isEquilibree()) {
            throw new IllegalStateException(String.format(
                    "Écriture de dotation déséquilibrée ! Débit=%s, Crédit=%s",
                    ecriture.getTotalDebit(), ecriture.getTotalCredit()
            ));
        }

        EcritureComptable ecritureSaved = ecritureRepository.save(ecriture);

        // Mettre à jour le statut des lignes et des immobilisations
        for (LignePlanAmortissement ligne : lignes) {
            ligne.setComptabilisee(true);
            ligne.setDateComptabilisation(ecritureSaved.getDateEcriture());
            ligne.setEcritureId(ecritureSaved.getId());
            lignePlanRepository.save(ligne);

            Immobilisation immo = ligne.getImmobilisation();
            immo.setCumulAmortissements(ligne.getCumulAmortissement());
            immo.setValeurNetteComptable(ligne.getValeurNetteFin());
            if (immo.getValeurNetteComptable().compareTo(BigDecimal.ZERO) <= 0) {
                immo.setStatut(StatutImmobilisation.TOTALEMENT_AMORTI);
            }
            immobilisationRepository.save(immo);
        }

        return toEcritureDto(ecritureSaved);
    }

    // =========================================================================
    // ENREGISTREMENT D'UNE CESSION (VNA 6513 / 28xx / 23xx)
    // =========================================================================

    public ImmobilisationDTO enregistrerCession(Long id, LocalDate dateCession, BigDecimal prixCession) {
        Long tenantId = getTenantId();
        Immobilisation immo = immobilisationRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Immobilisation introuvable ID: " + id));

        LocalDate dCess = dateCession != null ? dateCession : LocalDate.now();
        immo.setDateCession(dCess);
        immo.setPrixCession(prixCession != null ? prixCession : BigDecimal.ZERO);
        immo.setStatut(StatutImmobilisation.CEDE);

        BigDecimal valeurBrute = immo.getValeurAcquisition();
        BigDecimal amortCumul = immo.getCumulAmortissements();
        BigDecimal vna = valeurBrute.subtract(amortCumul);
        if (vna.compareTo(BigDecimal.ZERO) < 0) vna = BigDecimal.ZERO;

        // Générer l'écriture comptable de sortie d'actif au journal OD
        JournalComptable journalOd = journalRepository.findByCodeAndPointDeVenteId("OD", tenantId)
                .orElseGet(() -> {
                    JournalComptable j = new JournalComptable("OD", "Opérations Diverses", TypeJournal.OPERATIONS_DIVERSES, tenantId);
                    return journalRepository.save(j);
                });

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journalOd);
        ecriture.setDateEcriture(dCess);
        ecriture.setLibelle("Sortie d'actif après cession : " + immo.getCode() + " - " + immo.getDesignation());
        ecriture.setReferencePiece("CESS-" + immo.getCode());
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setValidee(true);

        String prefix = "OD-" + dCess.getYear() + "-";
        Long count = ecritureRepository.countByPrefixAndTenant(prefix, tenantId);
        ecriture.setNumeroPiece(String.format("%s%05d", prefix, (count != null ? count : 0) + 1));

        // Débit 28xx (Cumul des amortissements)
        if (amortCumul.compareTo(BigDecimal.ZERO) > 0) {
            ecriture.addLigne(new LigneEcriture(
                    immo.getCompteAmortissement(),
                    amortCumul,
                    BigDecimal.ZERO,
                    "Annulation amortissements cumulés " + immo.getCode(),
                    tenantId
            ));
        }

        // Débit 6513 (VNA de l'immobilisation cédée)
        if (vna.compareTo(BigDecimal.ZERO) > 0) {
            ecriture.addLigne(new LigneEcriture(
                    immo.getCompteVna(),
                    vna,
                    BigDecimal.ZERO,
                    "VNA immobilisation cédée " + immo.getCode(),
                    tenantId
            ));
        }

        // Crédit 23xx (Valeur brute d'origine de l'immobilisation)
        ecriture.addLigne(new LigneEcriture(
                immo.getCompteImmobilisation(),
                BigDecimal.ZERO,
                valeurBrute,
                "Sortie brute de bilan " + immo.getCode(),
                tenantId
        ));

        if (ecriture.isEquilibree()) {
            ecritureRepository.save(ecriture);
        }

        immo.setValeurNetteComptable(BigDecimal.ZERO);
        Immobilisation saved = immobilisationRepository.save(immo);
        return toImmobilisationDto(saved);
    }

    // =========================================================================
    // ÉTATS DE LIASSE FISCALE MAROCAINE (T10, T11, T12/T23)
    // =========================================================================

    @Transactional(readOnly = true)
    public LiasseTableauT10DTO getTableauT10(int annee) {
        Long tenantId = getTenantId();
        List<Immobilisation> immos = immobilisationRepository.findActifsJusquAAnnee(tenantId, annee);

        LiasseTableauT10DTO dto = new LiasseTableauT10DTO();
        dto.setAnnee(annee);

        Map<String, LiasseTableauT10DTO.LigneImmobilisationA3DTO> lignesParPoste = new LinkedHashMap<>();
        initialiserPostesT10(lignesParPoste);

        for (Immobilisation immo : immos) {
            String num = immo.getCompteImmobilisation().getNumeroCompte();
            String posteCle = determinerPosteImmo(num);

            LiasseTableauT10DTO.LigneImmobilisationA3DTO ligne = lignesParPoste.get(posteCle);
            if (ligne == null) continue;

            boolean acquisAvant = immo.getDateAcquisition().getYear() < annee;
            boolean acquisAnnee = immo.getDateAcquisition().getYear() == annee;
            boolean cedeAnnee = immo.getDateCession() != null && immo.getDateCession().getYear() == annee;
            boolean cedeAvant = immo.getDateCession() != null && immo.getDateCession().getYear() < annee;

            if (cedeAvant) continue; // Plus dans le bilan

            BigDecimal valAcq = immo.getValeurAcquisition();

            if (acquisAvant) {
                ligne.setMontantBrutDebut(ligne.getMontantBrutDebut().add(valAcq));
            }
            if (acquisAnnee) {
                ligne.setAcquisitions(ligne.getAcquisitions().add(valAcq));
            }
            if (cedeAnnee) {
                ligne.setCessions(ligne.getCessions().add(valAcq));
            }

            BigDecimal brutFin = ligne.getMontantBrutDebut().add(ligne.getAcquisitions()).subtract(ligne.getCessions());
            ligne.setMontantBrutFin(brutFin);
        }

        dto.setLignes(new ArrayList<>(lignesParPoste.values()));

        // Totaux
        dto.setTotalBrutDebut(dto.getLignes().stream().map(LiasseTableauT10DTO.LigneImmobilisationA3DTO::getMontantBrutDebut).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalAcquisitions(dto.getLignes().stream().map(LiasseTableauT10DTO.LigneImmobilisationA3DTO::getAcquisitions).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalCessions(dto.getLignes().stream().map(LiasseTableauT10DTO.LigneImmobilisationA3DTO::getCessions).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalBrutFin(dto.getLignes().stream().map(LiasseTableauT10DTO.LigneImmobilisationA3DTO::getMontantBrutFin).reduce(BigDecimal.ZERO, BigDecimal::add));

        return dto;
    }

    @Transactional(readOnly = true)
    public LiasseTableauT11DTO getTableauT11(int annee) {
        Long tenantId = getTenantId();
        List<Immobilisation> immos = immobilisationRepository.findActifsJusquAAnnee(tenantId, annee);

        LiasseTableauT11DTO dto = new LiasseTableauT11DTO();
        dto.setAnnee(annee);

        Map<String, LiasseTableauT11DTO.LigneAmortissementA4DTO> lignesParPoste = new LinkedHashMap<>();
        initialiserPostesT11(lignesParPoste);

        for (Immobilisation immo : immos) {
            String num = immo.getCompteAmortissement().getNumeroCompte();
            String posteCle = determinerPosteAmort(num);

            LiasseTableauT11DTO.LigneAmortissementA4DTO ligne = lignesParPoste.get(posteCle);
            if (ligne == null) continue;

            boolean cedeAvant = immo.getDateCession() != null && immo.getDateCession().getYear() < annee;
            if (cedeAvant) continue;

            boolean cedeAnnee = immo.getDateCession() != null && immo.getDateCession().getYear() == annee;

            // Dotations de l'exercice N
            BigDecimal dotAnnee = immo.getLignesPlanAmortissement().stream()
                    .filter(l -> l.getAnnee() == annee)
                    .map(LignePlanAmortissement::getDotation)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Cumul amortissements au 31/12 N-1
            BigDecimal cumulDebut = immo.getLignesPlanAmortissement().stream()
                    .filter(l -> l.getAnnee() < annee)
                    .map(LignePlanAmortissement::getDotation)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ligne.setCumulDebut(ligne.getCumulDebut().add(cumulDebut));
            ligne.setDotationsExercice(ligne.getDotationsExercice().add(dotAnnee));

            if (cedeAnnee) {
                BigDecimal amortSortie = cumulDebut.add(dotAnnee);
                ligne.setAmortissementsSorties(ligne.getAmortissementsSorties().add(amortSortie));
            }

            BigDecimal fin = ligne.getCumulDebut().add(ligne.getDotationsExercice()).subtract(ligne.getAmortissementsSorties());
            ligne.setCumulFin(fin);
        }

        dto.setLignes(new ArrayList<>(lignesParPoste.values()));

        // Totaux
        dto.setTotalCumulDebut(dto.getLignes().stream().map(LiasseTableauT11DTO.LigneAmortissementA4DTO::getCumulDebut).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalDotations(dto.getLignes().stream().map(LiasseTableauT11DTO.LigneAmortissementA4DTO::getDotationsExercice).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalAmortissementsSorties(dto.getLignes().stream().map(LiasseTableauT11DTO.LigneAmortissementA4DTO::getAmortissementsSorties).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalCumulFin(dto.getLignes().stream().map(LiasseTableauT11DTO.LigneAmortissementA4DTO::getCumulFin).reduce(BigDecimal.ZERO, BigDecimal::add));

        return dto;
    }

    @Transactional(readOnly = true)
    public LiasseTableauT12PlusMoinsValuesDTO getTableauT12PlusMoinsValues(int annee) {
        Long tenantId = getTenantId();
        List<Immobilisation> cessions = immobilisationRepository.findByPointDeVenteIdAndStatut(tenantId, StatutImmobilisation.CEDE).stream()
                .filter(i -> i.getDateCession() != null && i.getDateCession().getYear() == annee)
                .collect(Collectors.toList());

        LiasseTableauT12PlusMoinsValuesDTO dto = new LiasseTableauT12PlusMoinsValuesDTO();
        dto.setAnnee(annee);

        for (Immobilisation immo : cessions) {
            BigDecimal brute = immo.getValeurAcquisition();
            BigDecimal cumul = immo.getCumulAmortissements();
            BigDecimal vna = brute.subtract(cumul).max(BigDecimal.ZERO);
            BigDecimal prix = immo.getPrixCession();

            LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO ligne = new LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO(
                    immo.getDateCession(),
                    immo.getCompteImmobilisation().getNumeroCompte(),
                    immo.getDesignation(),
                    brute,
                    cumul,
                    vna,
                    prix
            );
            dto.getCessions().add(ligne);
        }

        dto.setTotalValeurBrute(dto.getCessions().stream().map(LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO::getValeurBrute).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalAmortissementsCumules(dto.getCessions().stream().map(LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO::getAmortissementsCumules).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalVna(dto.getCessions().stream().map(LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO::getValeurNetteAmortissements).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalPrixCession(dto.getCessions().stream().map(LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO::getPrixCession).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalPlusValues(dto.getCessions().stream().map(LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO::getPlusValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalMoinsValues(dto.getCessions().stream().map(LiasseTableauT12PlusMoinsValuesDTO.LigneCessionImmoDTO::getMoinsValue).reduce(BigDecimal.ZERO, BigDecimal::add));

        return dto;
    }

    // =========================================================================
    // UTILITAIRES PRIVÉS
    // =========================================================================

    private String genererCodeImmo(Long tenantId) {
        String prefix = "IMM-" + LocalDate.now().getYear() + "-";
        Long count = immobilisationRepository.countByPrefixAndTenant(prefix, tenantId);
        return String.format("%s%04d", prefix, (count != null ? count : 0) + 1);
    }

    private CompteComptable resoudreCompte(Long compteId, String numero, String defaultNumero, String defaultLibelle, int classe, SensCompte sens, Long tenantId) {
        if (compteId != null) {
            return compteRepository.findById(compteId)
                    .orElseGet(() -> findOrCreateCompte(defaultNumero, defaultLibelle, classe, sens, tenantId));
        }
        if (numero != null && !numero.trim().isEmpty()) {
            return compteRepository.findByNumeroCompteAndPointDeVenteId(numero.trim(), tenantId)
                    .orElseGet(() -> findOrCreateCompte(numero.trim(), defaultLibelle, classe, sens, tenantId));
        }
        return findOrCreateCompte(defaultNumero, defaultLibelle, classe, sens, tenantId);
    }

    public CompteComptable findOrCreateCompte(String numero, String libelle, int classe, SensCompte sens, Long tenantId) {
        return compteRepository.findByNumeroCompteAndPointDeVenteId(numero, tenantId)
                .orElseGet(() -> {
                    CompteComptable c = new CompteComptable(numero, libelle, classe, sens, tenantId);
                    return compteRepository.save(c);
                });
    }

    private String determinerPosteImmo(String num) {
        if (num.startsWith("21")) return "21";
        if (num.startsWith("22")) return "22";
        if (num.startsWith("231")) return "231";
        if (num.startsWith("232")) return "232";
        if (num.startsWith("233")) return "233";
        if (num.startsWith("234")) return "234";
        if (num.startsWith("235")) return "235";
        return "238";
    }

    private String determinerPosteAmort(String num) {
        if (num.startsWith("281")) return "281";
        if (num.startsWith("282")) return "282";
        if (num.startsWith("2832")) return "2832";
        if (num.startsWith("2833")) return "2833";
        if (num.startsWith("2834")) return "2834";
        if (num.startsWith("2835")) return "2835";
        return "2838";
    }

    private void initialiserPostesT10(Map<String, LiasseTableauT10DTO.LigneImmobilisationA3DTO> map) {
        map.put("21", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("21", "Immobilisation en non-valeurs", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("22", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("22", "Immobilisations incorporelles", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("231", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("231", "Terrains", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("232", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("232", "Constructions", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("233", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("233", "Installations techniques, matériel et outillage", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("234", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("234", "Matériel de transport", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("235", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("235", "Mobilier, matériel de bureau et aménagement", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("238", new LiasseTableauT10DTO.LigneImmobilisationA3DTO("238", "Autres immobilisations corporelles", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private void initialiserPostesT11(Map<String, LiasseTableauT11DTO.LigneAmortissementA4DTO> map) {
        map.put("281", new LiasseTableauT11DTO.LigneAmortissementA4DTO("281", "Amortissements des non-valeurs", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("282", new LiasseTableauT11DTO.LigneAmortissementA4DTO("282", "Amortissements des immos incorporelles", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("2832", new LiasseTableauT11DTO.LigneAmortissementA4DTO("2832", "Amortissements des constructions", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("2833", new LiasseTableauT11DTO.LigneAmortissementA4DTO("2833", "Amortissements du matériel et outillage", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("2834", new LiasseTableauT11DTO.LigneAmortissementA4DTO("2834", "Amortissements du matériel de transport", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("2835", new LiasseTableauT11DTO.LigneAmortissementA4DTO("2835", "Amortissements du mobilier et matériel de bureau", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        map.put("2838", new LiasseTableauT11DTO.LigneAmortissementA4DTO("2838", "Amortissements des autres immos corporelles", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private ImmobilisationDTO toImmobilisationDto(Immobilisation immo) {
        ImmobilisationDTO dto = new ImmobilisationDTO();
        dto.setId(immo.getId());
        dto.setCode(immo.getCode());
        dto.setDesignation(immo.getDesignation());
        dto.setNumeroFacture(immo.getNumeroFacture());
        dto.setFournisseurNom(immo.getFournisseurNom());
        dto.setDateAcquisition(immo.getDateAcquisition());
        dto.setDateMiseEnService(immo.getDateMiseEnService());
        dto.setValeurAcquisition(immo.getValeurAcquisition());
        dto.setTvaDeductible(immo.getTvaDeductible());
        dto.setValeurResiduelle(immo.getValeurResiduelle());
        dto.setDureeAnnees(immo.getDureeAnnees());
        dto.setTypeAmortissement(immo.getTypeAmortissement());
        dto.setTauxAmortissement(immo.getTauxAmortissement());
        dto.setCoefficientDegressif(immo.getCoefficientDegressif());
        dto.setStatut(immo.getStatut());
        dto.setDateCession(immo.getDateCession());
        dto.setPrixCession(immo.getPrixCession());
        dto.setCumulAmortissements(immo.getCumulAmortissements());
        dto.setValeurNetteComptable(immo.getValeurNetteComptable());
        dto.setPointDeVenteId(immo.getPointDeVenteId());
        dto.setDateCreation(immo.getDateCreation());

        if (immo.getCompteImmobilisation() != null) {
            dto.setCompteImmobilisationId(immo.getCompteImmobilisation().getId());
            dto.setCompteImmobilisationNumero(immo.getCompteImmobilisation().getNumeroCompte());
            dto.setCompteImmobilisationLibelle(immo.getCompteImmobilisation().getLibelle());
        }
        if (immo.getCompteAmortissement() != null) {
            dto.setCompteAmortissementId(immo.getCompteAmortissement().getId());
            dto.setCompteAmortissementNumero(immo.getCompteAmortissement().getNumeroCompte());
            dto.setCompteAmortissementLibelle(immo.getCompteAmortissement().getLibelle());
        }
        if (immo.getCompteDotation() != null) {
            dto.setCompteDotationId(immo.getCompteDotation().getId());
            dto.setCompteDotationNumero(immo.getCompteDotation().getNumeroCompte());
            dto.setCompteDotationLibelle(immo.getCompteDotation().getLibelle());
        }

        if (immo.getLignesPlanAmortissement() != null) {
            dto.setLignesPlanAmortissement(immo.getLignesPlanAmortissement().stream().map(l -> {
                LignePlanAmortissementDTO lDto = new LignePlanAmortissementDTO();
                lDto.setId(l.getId());
                lDto.setAnnee(l.getAnnee());
                lDto.setMoisAmortis(l.getMoisAmortis());
                lDto.setBaseCalcul(l.getBaseCalcul());
                lDto.setTauxApplique(l.getTauxApplique());
                lDto.setDotation(l.getDotation());
                lDto.setCumulAmortissement(l.getCumulAmortissement());
                lDto.setValeurNetteFin(l.getValeurNetteFin());
                lDto.setModeLineaireBascule(l.getModeLineaireBascule());
                lDto.setComptabilisee(l.getComptabilisee());
                lDto.setDateComptabilisation(l.getDateComptabilisation());
                lDto.setEcritureId(l.getEcritureId());
                return lDto;
            }).collect(Collectors.toList()));
        }

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
        return dto;
    }
}
