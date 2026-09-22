package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.StatutRapprochement;
import com.gestion.persistent.enums.TypeCompteFinancier;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RapprochementBancaireService {

    private final ReleveBancaireRepository releveRepository;
    private final LigneReleveBancaireRepository ligneReleveRepository;
    private final CompteFinancierRepository compteFinancierRepository;
    private final MouvementTresorerieRepository mouvementTresorerieRepository;
    private final OpenOcrService openOcrService;
    private final TransactionBancaireService transactionBancaireService;
    private final RapprochementService rapprochementService;

    public RapprochementBancaireService(ReleveBancaireRepository releveRepository,
                                        LigneReleveBancaireRepository ligneReleveRepository,
                                        CompteFinancierRepository compteFinancierRepository,
                                        MouvementTresorerieRepository mouvementTresorerieRepository,
                                        OpenOcrService openOcrService,
                                        TransactionBancaireService transactionBancaireService,
                                        RapprochementService rapprochementService) {
        this.releveRepository = releveRepository;
        this.ligneReleveRepository = ligneReleveRepository;
        this.compteFinancierRepository = compteFinancierRepository;
        this.mouvementTresorerieRepository = mouvementTresorerieRepository;
        this.openOcrService = openOcrService;
        this.transactionBancaireService = transactionBancaireService;
        this.rapprochementService = rapprochementService;
    }

    private Long getTenantId() {
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    // =========================================================================
    // IMPORT RELEVÉ BANCAIRE PAR OCR (PDF / JPG / PNG)
    // =========================================================================

    public List<LigneReleveBancaireDTO> previewOcr(byte[] content, String fileName, String contentType) {
        String extractedText = openOcrService.extraireTexte(content, fileName, contentType);
        return transactionBancaireService.extraireTransactions(extractedText);
    }

    public ReleveBancaireDTO importerReleveOcr(Long compteId, String fileName, String contentType, byte[] content) {
        Long tenantId = getTenantId();
        CompteFinancier compte = compteFinancierRepository.findById(compteId)
                .orElseThrow(() -> new IllegalArgumentException("Compte financier introuvable : " + compteId));

        String extractedText = openOcrService.extraireTexte(content, fileName, contentType);
        List<LigneReleveBancaireDTO> dtos = transactionBancaireService.extraireTransactions(extractedText);

        if (dtos.isEmpty()) {
            throw new IllegalArgumentException("Aucune opération bancaire n'a pu être extraite du document par l'OCR. Veuillez vérifier le fichier.");
        }

        ReleveBancaire releve = new ReleveBancaire();
        releve.setCompteFinancier(compte);
        releve.setPointDeVenteId(tenantId);
        releve.setDateImport(LocalDateTime.now());
        releve.setStatut("EN_COURS");
        releve.setReferenceReleve("OCR-" + (compte.getCode() != null ? compte.getCode() : "BQ") + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + (System.currentTimeMillis() % 1000));

        LocalDate minDate = null;
        LocalDate maxDate = null;
        BigDecimal cumulDebit = BigDecimal.ZERO;
        BigDecimal cumulCredit = BigDecimal.ZERO;

        for (LigneReleveBancaireDTO dto : dtos) {
            LigneReleveBancaire ligne = new LigneReleveBancaire();
            ligne.setDateOperation(dto.getDateOperation() != null ? dto.getDateOperation() : LocalDate.now());
            ligne.setDateValeur(dto.getDateValeur() != null ? dto.getDateValeur() : ligne.getDateOperation());
            
            String libelle = dto.getLibelle() != null && !dto.getLibelle().isBlank() ? dto.getLibelle().trim() : "Opération bancaire";
            if (libelle.length() > 490) libelle = libelle.substring(0, 490);
            ligne.setLibelle(libelle);

            String ref = dto.getReference() != null ? dto.getReference().trim() : null;
            if (ref != null && ref.length() > 90) ref = ref.substring(0, 90);
            ligne.setReference(ref);

            ligne.setDebit(dto.getDebit() != null ? dto.getDebit() : BigDecimal.ZERO);
            ligne.setCredit(dto.getCredit() != null ? dto.getCredit() : BigDecimal.ZERO);
            ligne.setStatut(StatutRapprochement.NON_RAPPROCHE);
            ligne.setPointDeVenteId(tenantId);

            releve.addLigne(ligne);

            cumulDebit = cumulDebit.add(ligne.getDebit());
            cumulCredit = cumulCredit.add(ligne.getCredit());

            if (minDate == null || ligne.getDateOperation().isBefore(minDate)) minDate = ligne.getDateOperation();
            if (maxDate == null || ligne.getDateOperation().isAfter(maxDate)) maxDate = ligne.getDateOperation();
        }

        releve.setDateDebut(minDate != null ? minDate : LocalDate.now());
        releve.setDateFin(maxDate != null ? maxDate : LocalDate.now());

        BigDecimal soldeInit = compte.getSoldeActuel() != null ? compte.getSoldeActuel() : BigDecimal.ZERO;
        releve.setSoldeInitial(soldeInit);
        releve.setSoldeFinal(soldeInit.add(cumulCredit).subtract(cumulDebit));

        ReleveBancaire saved = releveRepository.save(releve);
        return toReleveDto(saved, true);
    }

    public RapprochementComparatif5141DTO getComparatif5141(Long compteId, LocalDate dateDebut, LocalDate dateFin) {
        return rapprochementService.getComparatif5141(compteId, dateDebut, dateFin);
    }

    public RapprochementComparatif5141DTO getComparatif5141(Long compteId, LocalDate dateArrete) {
        return rapprochementService.getComparatif5141(compteId, null, dateArrete);
    }

    public ItemComparatifRapprochementDTO creerEcriturePourLigne(CreerEcritureReleveRequest req) {
        return rapprochementService.creerEcriturePourLigne(req);
    }

    public Map<String, Object> autoRapprocher5141(Long releveId) {
        return rapprochementService.autoRapprocher5141(releveId);
    }


    // =========================================================================
    // IMPORT RELEVÉ BANCAIRE (CSV)
    // =========================================================================

    public ReleveBancaireDTO importerReleveCsv(Long compteId, String fileName, byte[] content) {
        Long tenantId = getTenantId();
        CompteFinancier compte = compteFinancierRepository.findById(compteId)
                .orElseThrow(() -> new IllegalArgumentException("Compte financier introuvable : " + compteId));

        ReleveBancaire releve = new ReleveBancaire();
        releve.setCompteFinancier(compte);
        releve.setPointDeVenteId(tenantId);
        releve.setDateImport(LocalDateTime.now());
        releve.setReferenceReleve("REL-" + compte.getCode() + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")));

        List<LigneReleveBancaire> lignes = new ArrayList<>();
        LocalDate minDate = null;
        LocalDate maxDate = null;
        BigDecimal cumulDebit = BigDecimal.ZERO;
        BigDecimal cumulCredit = BigDecimal.ZERO;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            String separator = ";";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Enlever le BOM éventuel
                if (firstLine && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                if (firstLine) {
                    if (line.contains(";")) separator = ";";
                    else if (line.contains(",")) separator = ",";
                    else if (line.contains("\t")) separator = "\t";
                    firstLine = false;
                    // Si la ligne ressemble à un en-tête (contient date ou libelle), on saute
                    if (line.toLowerCase().contains("date") || line.toLowerCase().contains("libell") || line.toLowerCase().contains("montant")) {
                        continue;
                    }
                }

                String[] cols = line.split(separator);
                if (cols.length < 3) continue;

                LocalDate dateOp = parseDate(cols[0].trim());
                if (dateOp == null) continue;

                String libelle = cols.length > 1 ? cols[1].trim().replaceAll("^\"|\"$", "") : "Opération bancaire";
                BigDecimal debit = BigDecimal.ZERO;
                BigDecimal credit = BigDecimal.ZERO;
                String ref = "";

                if (cols.length >= 4) {
                    // Format Date;Libelle;Debit;Credit
                    debit = parseMontant(cols[2]);
                    credit = parseMontant(cols[3]);
                    if (cols.length >= 5) ref = cols[4].trim();
                } else {
                    // Format Date;Libelle;Montant (+/-)
                    BigDecimal mnt = parseMontant(cols[2]);
                    if (mnt.compareTo(BigDecimal.ZERO) < 0) {
                        debit = mnt.abs();
                    } else {
                        credit = mnt;
                    }
                }

                if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                LigneReleveBancaire ligneReleve = new LigneReleveBancaire();
                ligneReleve.setDateOperation(dateOp);
                ligneReleve.setDateValeur(dateOp);
                ligneReleve.setLibelle(libelle);
                ligneReleve.setDebit(debit);
                ligneReleve.setCredit(credit);
                ligneReleve.setReference(ref);
                ligneReleve.setStatut(StatutRapprochement.NON_RAPPROCHE);
                ligneReleve.setPointDeVenteId(tenantId);

                releve.addLigne(ligneReleve);

                cumulDebit = cumulDebit.add(debit);
                cumulCredit = cumulCredit.add(credit);

                if (minDate == null || dateOp.isBefore(minDate)) minDate = dateOp;
                if (maxDate == null || dateOp.isAfter(maxDate)) maxDate = dateOp;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier CSV : " + e.getMessage(), e);
        }

        if (releve.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Le fichier ne contient aucune ligne d'opération valide.");
        }

        releve.setDateDebut(minDate != null ? minDate : LocalDate.now());
        releve.setDateFin(maxDate != null ? maxDate : LocalDate.now());

        // Solde initial = solde actuel du compte moins l'impact du relevé
        BigDecimal soldeInit = compte.getSoldeActuel() != null ? compte.getSoldeActuel() : BigDecimal.ZERO;
        releve.setSoldeInitial(soldeInit);
        releve.setSoldeFinal(soldeInit.add(cumulCredit).subtract(cumulDebit));

        ReleveBancaire saved = releveRepository.save(releve);
        return toReleveDto(saved, true);
    }

    private LocalDate parseDate(String val) {
        val = val.replaceAll("^\"|\"$", "").trim();
        String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "d/M/yyyy"};
        for (String f : formats) {
            try {
                return LocalDate.parse(val, DateTimeFormatter.ofPattern(f));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal parseMontant(String val) {
        if (val == null) return BigDecimal.ZERO;
        val = val.replaceAll("^\"|\"$", "").trim().replace(" ", "").replace(",", ".");
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // =========================================================================
    // CONSULTATION DES RELEVÉS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ReleveBancaireDTO> getReleves(Long compteId) {
        Long tenantId = getTenantId();
        List<ReleveBancaire> list;
        if (compteId != null) {
            list = releveRepository.findByCompteFinancierIdAndPointDeVenteIdOrderByDateDebutDesc(compteId, tenantId);
        } else {
            list = releveRepository.findByPointDeVenteIdOrderByDateDebutDesc(tenantId);
        }
        return list.stream().map(r -> toReleveDto(r, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleveBancaireDTO getReleveDetail(Long releveId) {
        Long tenantId = getTenantId();
        ReleveBancaire r = releveRepository.findByIdAndPointDeVenteId(releveId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Relevé introuvable : " + releveId));
        return toReleveDto(r, true);
    }

    // =========================================================================
    // ÉTAT DE RAPPROCHEMENT
    // =========================================================================

    @Transactional(readOnly = true)
    public RapprochementEtatDTO getRapprochementEtat(Long compteId, LocalDate dateArrete) {
        Long tenantId = getTenantId();
        CompteFinancier compte = compteFinancierRepository.findById(compteId)
                .orElseThrow(() -> new IllegalArgumentException("Compte financier introuvable : " + compteId));

        LocalDate date = dateArrete != null ? dateArrete : LocalDate.now();

        RapprochementEtatDTO etat = new RapprochementEtatDTO();
        etat.setCompteId(compte.getId());
        etat.setCompteNom(compte.getNom());
        etat.setNumeroRib(compte.getNumeroCompteRib());
        etat.setBanqueNom(compte.getBanque() != null ? compte.getBanque().getNom() : (compte.getNomBanque() != null ? compte.getNomBanque() : "Banque"));
        etat.setDateArrete(date);

        // Solde comptable actuel
        BigDecimal soldeComptable = compte.getSoldeActuel() != null ? compte.getSoldeActuel() : BigDecimal.ZERO;
        etat.setSoldeComptable(soldeComptable);

        // Dernier relevé bancaire pour ce compte
        Optional<ReleveBancaire> dernierReleve = releveRepository.findFirstByCompteFinancierIdAndPointDeVenteIdOrderByDateFinDesc(compteId, tenantId);
        BigDecimal soldeReleve = dernierReleve.map(ReleveBancaire::getSoldeFinal).orElse(soldeComptable);
        etat.setSoldeReleve(soldeReleve);
        etat.setEcart(soldeReleve.subtract(soldeComptable));

        // Lignes du relevé bancaire non rapprochées
        List<LigneReleveBancaire> lignesNonRapprochees = ligneReleveRepository.findNonRapprocheesAvantDate(compteId, date, tenantId);
        BigDecimal debitsSuspens = BigDecimal.ZERO;
        BigDecimal creditsSuspens = BigDecimal.ZERO;
        for (LigneReleveBancaire l : lignesNonRapprochees) {
            debitsSuspens = debitsSuspens.add(l.getDebit());
            creditsSuspens = creditsSuspens.add(l.getCredit());
        }
        etat.setTotalDebitsReleveEnSuspens(debitsSuspens);
        etat.setTotalCreditsReleveEnSuspens(creditsSuspens);
        etat.setLignesReleveEnAttente(lignesNonRapprochees.stream().map(this::toLigneDto).collect(Collectors.toList()));

        // Mouvements de trésorerie de ce compte
        List<MouvementTresorerie> mouvements = mouvementTresorerieRepository.findByCompteIdAndPointDeVenteIdOrderByDateMouvementDesc(compteId, tenantId);
        // Filtrer les mouvements qui ne sont pas encore liés à une ligne de relevé
        Set<Long> mouvementsLiesIds = lignesNonRapprochees.stream()
                .filter(l -> l.getMouvementTresorerie() != null)
                .map(l -> l.getMouvementTresorerie().getId())
                .collect(Collectors.toSet());

        List<MouvementTresorerieDTO> mouvsEnAttente = mouvements.stream()
                .filter(m -> !mouvementsLiesIds.contains(m.getId()))
                .map(this::toMouvementDto)
                .collect(Collectors.toList());

        etat.setMouvementsEnAttente(mouvsEnAttente);
        return etat;
    }

    // =========================================================================
    // POINTAGE / RAPPROCHEMENT MANUEL ET AUTO
    // =========================================================================

    public LigneReleveBancaireDTO rapprocher(RapprochementPointageRequest req) {
        Long tenantId = getTenantId();
        LigneReleveBancaire ligne = ligneReleveRepository.findById(req.getLigneReleveId())
                .orElseThrow(() -> new IllegalArgumentException("Ligne relevé introuvable : " + req.getLigneReleveId()));

        if (req.getMouvementTresorerieId() != null) {
            MouvementTresorerie m = mouvementTresorerieRepository.findById(req.getMouvementTresorerieId())
                    .orElseThrow(() -> new IllegalArgumentException("Mouvement trésorerie introuvable : " + req.getMouvementTresorerieId()));
            ligne.setMouvementTresorerie(m);
        }
        if (req.getLigneEcritureId() != null) {
            ligne.setLigneEcritureId(req.getLigneEcritureId());
        }

        ligne.setStatut(StatutRapprochement.RAPPROCHE);
        ligne.setDateRapprochement(LocalDateTime.now());
        return toLigneDto(ligneReleveRepository.save(ligne));
    }

    public LigneReleveBancaireDTO derapprocher(Long ligneReleveId) {
        LigneReleveBancaire ligne = ligneReleveRepository.findById(ligneReleveId)
                .orElseThrow(() -> new IllegalArgumentException("Ligne relevé introuvable : " + ligneReleveId));

        ligne.setStatut(StatutRapprochement.NON_RAPPROCHE);
        ligne.setMouvementTresorerie(null);
        ligne.setLigneEcritureId(null);
        ligne.setDateRapprochement(null);
        return toLigneDto(ligneReleveRepository.save(ligne));
    }

    public Map<String, Object> autoRapprochement(Long releveId) {
        Long tenantId = getTenantId();
        ReleveBancaire releve = releveRepository.findByIdAndPointDeVenteId(releveId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Relevé introuvable : " + releveId));

        Long compteId = releve.getCompteFinancier().getId();
        List<LigneReleveBancaire> lignesNonRapprochees = releve.getLignes().stream()
                .filter(l -> l.getStatut() == StatutRapprochement.NON_RAPPROCHE)
                .collect(Collectors.toList());

        List<MouvementTresorerie> mouvements = mouvementTresorerieRepository.findByCompteIdAndPointDeVenteIdOrderByDateMouvementDesc(compteId, tenantId);

        int reconcilies = 0;
        Set<Long> mouvementsUtilises = new HashSet<>();

        for (LigneReleveBancaire ligne : lignesNonRapprochees) {
            BigDecimal montantLigne = ligne.getCredit().compareTo(BigDecimal.ZERO) > 0 ? ligne.getCredit() : ligne.getDebit();
            LocalDate dateLigne = ligne.getDateOperation();

            for (MouvementTresorerie m : mouvements) {
                if (mouvementsUtilises.contains(m.getId())) continue;

                BigDecimal mntMouvement = m.getMontant() != null ? m.getMontant() : BigDecimal.ZERO;
                if (mntMouvement.compareTo(montantLigne) == 0) {
                    LocalDate dateM = m.getDateMouvement() != null ? m.getDateMouvement().toLocalDate() : null;
                    long deltaJours = (dateM != null && dateLigne != null) ? Math.abs(ChronoUnit.DAYS.between(dateLigne, dateM)) : 999;

                    if (deltaJours <= 7) {
                        ligne.setMouvementTresorerie(m);
                        ligne.setStatut(StatutRapprochement.RAPPROCHE);
                        ligne.setDateRapprochement(LocalDateTime.now());
                        ligneReleveRepository.save(ligne);
                        mouvementsUtilises.add(m.getId());
                        reconcilies++;
                        break;
                    }
                }
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("reconcilies", reconcilies);
        res.put("totalLignes", releve.getLignes().size());
        return res;
    }

    public void supprimerReleve(Long releveId) {
        Long tenantId = getTenantId();
        ReleveBancaire releve = releveRepository.findByIdAndPointDeVenteId(releveId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Relevé introuvable : " + releveId));
        releveRepository.delete(releve);
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================

    private ReleveBancaireDTO toReleveDto(ReleveBancaire r, boolean includeLignes) {
        ReleveBancaireDTO dto = new ReleveBancaireDTO();
        dto.setId(r.getId());
        dto.setReferenceReleve(r.getReferenceReleve());
        if (r.getCompteFinancier() != null) {
            dto.setCompteFinancierId(r.getCompteFinancier().getId());
            dto.setCompteFinancierNom(r.getCompteFinancier().getNom());
            dto.setBanqueNom(r.getCompteFinancier().getNomBanque());
        }
        dto.setDateDebut(r.getDateDebut());
        dto.setDateFin(r.getDateFin());
        dto.setSoldeInitial(r.getSoldeInitial());
        dto.setSoldeFinal(r.getSoldeFinal());
        dto.setDateImport(r.getDateImport());
        dto.setStatut(r.getStatut());

        if (r.getLignes() != null) {
            dto.setTotalLignes(r.getLignes().size());
            long rapp = r.getLignes().stream().filter(l -> l.getStatut() == StatutRapprochement.RAPPROCHE).count();
            dto.setLignesRapprochees((int) rapp);
            if (includeLignes) {
                dto.setLignes(r.getLignes().stream().map(this::toLigneDto).collect(Collectors.toList()));
            }
        }
        return dto;
    }

    private LigneReleveBancaireDTO toLigneDto(LigneReleveBancaire l) {
        LigneReleveBancaireDTO dto = new LigneReleveBancaireDTO();
        dto.setId(l.getId());
        if (l.getReleveBancaire() != null) {
            dto.setReleveBancaireId(l.getReleveBancaire().getId());
        }
        dto.setDateOperation(l.getDateOperation());
        dto.setDateValeur(l.getDateValeur());
        dto.setLibelle(l.getLibelle());
        dto.setDebit(l.getDebit());
        dto.setCredit(l.getCredit());
        dto.setReference(l.getReference());
        dto.setStatut(l.getStatut());
        if (l.getMouvementTresorerie() != null) {
            dto.setMouvementTresorerieId(l.getMouvementTresorerie().getId());
            dto.setMouvementReference(l.getMouvementTresorerie().getReference());
        }
        dto.setLigneEcritureId(l.getLigneEcritureId());
        dto.setDateRapprochement(l.getDateRapprochement());
        return dto;
    }

    private MouvementTresorerieDTO toMouvementDto(MouvementTresorerie m) {
        MouvementTresorerieDTO dto = new MouvementTresorerieDTO();
        dto.setId(m.getId());
        dto.setReference(m.getReference());
        dto.setTypeMouvement(m.getTypeMouvement());
        if (m.getCompteSource() != null) {
            dto.setCompteSourceId(m.getCompteSource().getId());
            dto.setCompteSourceNom(m.getCompteSource().getNom());
        }
        if (m.getCompteDestination() != null) {
            dto.setCompteDestinationId(m.getCompteDestination().getId());
            dto.setCompteDestinationNom(m.getCompteDestination().getNom());
        }
        dto.setMontant(m.getMontant());
        dto.setDateMouvement(m.getDateMouvement());
        dto.setMotif(m.getMotif());
        dto.setJustificatifReference(m.getJustificatifReference());
        return dto;
    }
}
