package com.gestion.service;

import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.*;
import com.gestion.persistent.model.AllocationCharge;
import com.gestion.persistent.model.ChargeAchat;
import com.gestion.persistent.model.Commande;
import com.gestion.persistent.model.LigneCommande;
import com.gestion.repository.AllocationChargeRepository;
import com.gestion.repository.ChargeAchatRepository;
import com.gestion.repository.CommandeRepository;
import com.gestion.repository.LigneCommandeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CoutRevientService {

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final ChargeAchatRepository chargeAchatRepository;
    private final AllocationChargeRepository allocationChargeRepository;
    private final RepartitionChargeService repartitionChargeService;

    public CoutRevientService(CommandeRepository commandeRepository,
                              LigneCommandeRepository ligneCommandeRepository,
                              ChargeAchatRepository chargeAchatRepository,
                              AllocationChargeRepository allocationChargeRepository,
                              RepartitionChargeService repartitionChargeService) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.chargeAchatRepository = chargeAchatRepository;
        this.allocationChargeRepository = allocationChargeRepository;
        this.repartitionChargeService = repartitionChargeService;
    }

    /**
     * Recalcule et persiste le coût de revient d'une commande existante.
     */
    @Transactional
    public RecapCoutRevientDTO recalculerEtPersister(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", commandeId));

        List<LigneCommande> lignes = ligneCommandeRepository.findByCommande_Id(commandeId);
        List<ChargeAchat> charges = chargeAchatRepository.findByCommande_Id(commandeId);

        // 1. Déterminer le taux de change effectif (DUM en priorité, sinon estimé commande, sinon 1)
        BigDecimal tauxChange = determinerTauxChange(commande);
        String sourceTaux = (commande.getTauxChangeDouane() != null && commande.getTauxChangeDouane().compareTo(BigDecimal.ZERO) > 0)
                ? "DUM" : "ESTIME_COMMANDE";

        // 2. Mettre à jour les montants MAD des charges (montant réel si dispo, sinon estimé)
        BigDecimal totalChargesEstimees = BigDecimal.ZERO;
        BigDecimal totalChargesReelles = BigDecimal.ZERO;
        BigDecimal totalChargesIncorporables = BigDecimal.ZERO;
        BigDecimal tvaImport = BigDecimal.ZERO;
        BigDecimal chargesNonIncorporables = BigDecimal.ZERO;
        boolean aUneReelle = false;
        boolean aUneEstimee = false;

        for (ChargeAchat charge : charges) {
            BigDecimal tauxCharge = (charge.getTauxChange() != null && charge.getTauxChange().compareTo(BigDecimal.ZERO) > 0)
                    ? charge.getTauxChange() : BigDecimal.ONE;

            BigDecimal montantRetenuDevise;
            if (charge.getMontantReelDevise() != null) {
                montantRetenuDevise = charge.getMontantReelDevise();
                charge.setStatut(StatutCharge.REELLE);
                aUneReelle = true;
                totalChargesReelles = totalChargesReelles.add(montantRetenuDevise.multiply(tauxCharge));
            } else {
                montantRetenuDevise = charge.getMontantEstimeDevise() != null ? charge.getMontantEstimeDevise() : BigDecimal.ZERO;
                charge.setStatut(StatutCharge.ESTIMEE);
                aUneEstimee = true;
                totalChargesEstimees = totalChargesEstimees.add(montantRetenuDevise.multiply(tauxCharge));
            }

            BigDecimal montantMad = montantRetenuDevise.multiply(tauxCharge).setScale(2, RoundingMode.HALF_UP);
            charge.setMontantRetenuMad(montantMad);

            if (charge.getTypeCharge() == TypeCharge.TVA_IMPORT) {
                tvaImport = tvaImport.add(montantMad);
            } else if (Boolean.TRUE.equals(charge.getIncorporable())) {
                totalChargesIncorporables = totalChargesIncorporables.add(montantMad);
            } else {
                chargesNonIncorporables = chargesNonIncorporables.add(montantMad);
            }
        }

        // 3. Répartir les charges incorporables et enregistrer les allocations
        Map<Long, BigDecimal> fraisAllouesParLigne = new HashMap<>();
        for (LigneCommande l : lignes) {
            fraisAllouesParLigne.put(l.getId(), BigDecimal.ZERO);
        }

        for (ChargeAchat charge : charges) {
            // Nettoyer anciennes allocations
            allocationChargeRepository.deleteByCharge_Id(charge.getId());

            if (Boolean.TRUE.equals(charge.getIncorporable()) && charge.getTypeCharge() != TypeCharge.TVA_IMPORT) {
                List<AllocationCharge> allocs = repartitionChargeService.repartirCharge(charge, lignes);
                for (AllocationCharge a : allocs) {
                    allocationChargeRepository.save(a);
                    Long lid = a.getLigneCommande().getId();
                    fraisAllouesParLigne.put(lid, fraisAllouesParLigne.getOrDefault(lid, BigDecimal.ZERO).add(a.getMontantMad()));
                }
            }
        }

        // 4. Mettre à jour les lignes de commande
        BigDecimal totalMarchandiseDevise = BigDecimal.ZERO;
        BigDecimal totalMarchandiseMad = BigDecimal.ZERO;
        BigDecimal totalCoutRevientLignes = BigDecimal.ZERO;

        for (LigneCommande l : lignes) {
            BigDecimal puDevise = l.getPrixUnitaire() != null ? l.getPrixUnitaire() : BigDecimal.ZERO;
            BigDecimal qte = l.getQuantiteCommandee() != null ? BigDecimal.valueOf(l.getQuantiteCommandee()) : BigDecimal.ONE;
            BigDecimal puMad = puDevise.multiply(tauxChange).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalLigneMad = puMad.multiply(qte).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalLigneDevise = puDevise.multiply(qte).setScale(2, RoundingMode.HALF_UP);

            BigDecimal fraisLigne = fraisAllouesParLigne.getOrDefault(l.getId(), BigDecimal.ZERO);
            BigDecimal coutRevientTotal = totalLigneMad.add(fraisLigne);
            BigDecimal coutRevientUnitaire = qte.compareTo(BigDecimal.ZERO) > 0
                    ? coutRevientTotal.divide(qte, 4, RoundingMode.HALF_UP)
                    : coutRevientTotal;

            l.setPrixUnitaireMad(puMad);
            l.setFraisAllouesMad(fraisLigne);
            l.setCoutRevientUnitaireMad(coutRevientUnitaire);
            l.setCoutRevientTotalMad(coutRevientTotal);

            ligneCommandeRepository.save(l);

            totalMarchandiseDevise = totalMarchandiseDevise.add(totalLigneDevise);
            totalMarchandiseMad = totalMarchandiseMad.add(totalLigneMad);
            totalCoutRevientLignes = totalCoutRevientLignes.add(coutRevientTotal);
        }

        // 5. Statut des coûts
        StatutCouts statutCouts;
        if (charges.isEmpty()) {
            statutCouts = StatutCouts.ESTIME;
        } else if (aUneReelle && !aUneEstimee) {
            statutCouts = StatutCouts.DEFINITIF;
        } else if (aUneReelle) {
            statutCouts = StatutCouts.PARTIEL;
        } else {
            statutCouts = StatutCouts.ESTIME;
        }

        // 6. Coefficient d'approche global
        BigDecimal coefficient = totalMarchandiseMad.compareTo(BigDecimal.ZERO) > 0
                ? totalCoutRevientLignes.divide(totalMarchandiseMad, 4, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        // 7. Base douane CIF estimée
        BigDecimal totalAcheminementVersCif = calculerAcheminementVersCif(commande.getIncoterm(), charges);
        BigDecimal valeurDouaneCif = totalMarchandiseMad.add(totalAcheminementVersCif);

        // 8. Sauvegarder commande
        commande.setMontantMarchandiseDevise(totalMarchandiseDevise);
        commande.setMontantMarchandiseMad(totalMarchandiseMad);
        commande.setValeurDouaneMad(valeurDouaneCif);
        commande.setTotalChargesEstimeesMad(totalChargesEstimees);
        commande.setTotalChargesReellesMad(totalChargesReelles);
        commande.setTotalChargesMad(totalChargesIncorporables);
        commande.setCoutRevientTotalMad(totalCoutRevientLignes);
        commande.setCoefficientApproche(coefficient);
        commande.setStatutCouts(statutCouts);

        commandeRepository.save(commande);

        return construireRecap(commande, lignes, charges, tauxChange, sourceTaux, tvaImport, chargesNonIncorporables);
    }

    /**
     * Simulation en direct pour le formulaire front-end (sans écriture en base).
     */
    public RecapCoutRevientDTO simulerCoutRevient(SimulationCoutRevientRequest req) {
        BigDecimal tauxChange = (req.getTauxChange() != null && req.getTauxChange().compareTo(BigDecimal.ZERO) > 0)
                ? req.getTauxChange() : BigDecimal.ONE;

        // Préparer lignes virtuelles
        List<LigneCommande> lignesVirtuelles = new ArrayList<>();
        long idLigne = 1L;
        for (LigneCommandeDTO dto : req.getLignes()) {
            LigneCommande lc = new LigneCommande();
            lc.setId(dto.getId() != null ? dto.getId() : idLigne++);
            lc.setQuantiteCommandee(dto.getQuantiteCommandee() != null ? dto.getQuantiteCommandee() : 1);
            lc.setPrixUnitaire(dto.getPrixUnitaire() != null ? dto.getPrixUnitaire() : BigDecimal.ZERO);
            lc.setMontantHT(dto.getMontantHT() != null ? dto.getMontantHT() : lc.getPrixUnitaire().multiply(BigDecimal.valueOf(lc.getQuantiteCommandee())));
            lc.setPoidsTotalKg(dto.getPoidsTotalKg());
            lc.setVolumeTotalM3(dto.getVolumeTotalM3());
            lc.setCodeSh(dto.getCodeSh());
            lc.setTauxDroitDouane(dto.getTauxDroitDouane());
            lignesVirtuelles.add(lc);
        }

        // Préparer charges virtuelles
        List<ChargeAchat> chargesVirtuelles = new ArrayList<>();
        BigDecimal totalChargesEstimees = BigDecimal.ZERO;
        BigDecimal totalChargesReelles = BigDecimal.ZERO;
        BigDecimal totalChargesIncorporables = BigDecimal.ZERO;
        BigDecimal tvaImport = BigDecimal.ZERO;
        BigDecimal chargesNonIncorporables = BigDecimal.ZERO;

        for (ChargeAchatDTO cdto : req.getCharges()) {
            ChargeAchat c = new ChargeAchat();
            c.setTypeCharge(cdto.getTypeCharge() != null ? cdto.getTypeCharge() : TypeCharge.AUTRE);
            c.setCleRepartition(cdto.getCleRepartition() != null ? cdto.getCleRepartition() : CleRepartition.VALEUR);
            c.setIncorporable(cdto.getIncorporable() != null ? cdto.getIncorporable() : true);
            c.setDevise(cdto.getDevise() != null ? cdto.getDevise() : "MAD");
            c.setTauxChange(cdto.getTauxChange() != null ? cdto.getTauxChange() : BigDecimal.ONE);

            BigDecimal montantDevise = cdto.getMontantReelDevise() != null ? cdto.getMontantReelDevise()
                    : (cdto.getMontantEstimeDevise() != null ? cdto.getMontantEstimeDevise() : BigDecimal.ZERO);
            BigDecimal montantMad = montantDevise.multiply(c.getTauxChange()).setScale(2, RoundingMode.HALF_UP);
            c.setMontantRetenuMad(montantMad);

            if (cdto.getLigneCommandeId() != null) {
                lignesVirtuelles.stream()
                        .filter(l -> l.getId().equals(cdto.getLigneCommandeId()))
                        .findFirst()
                        .ifPresent(c::setLigneCommande);
            }

            chargesVirtuelles.add(c);

            if (cdto.getMontantReelDevise() != null) {
                totalChargesReelles = totalChargesReelles.add(montantMad);
            } else {
                totalChargesEstimees = totalChargesEstimees.add(montantMad);
            }

            if (c.getTypeCharge() == TypeCharge.TVA_IMPORT) {
                tvaImport = tvaImport.add(montantMad);
            } else if (Boolean.TRUE.equals(c.getIncorporable())) {
                totalChargesIncorporables = totalChargesIncorporables.add(montantMad);
            } else {
                chargesNonIncorporables = chargesNonIncorporables.add(montantMad);
            }
        }

        // Répartition en mémoire
        Map<Long, BigDecimal> fraisParLigne = new HashMap<>();
        for (LigneCommande l : lignesVirtuelles) {
            fraisParLigne.put(l.getId(), BigDecimal.ZERO);
        }

        for (ChargeAchat c : chargesVirtuelles) {
            if (Boolean.TRUE.equals(c.getIncorporable()) && c.getTypeCharge() != TypeCharge.TVA_IMPORT) {
                List<AllocationCharge> allocs = repartitionChargeService.repartirCharge(c, lignesVirtuelles);
                for (AllocationCharge a : allocs) {
                    Long lid = a.getLigneCommande().getId();
                    fraisParLigne.put(lid, fraisParLigne.getOrDefault(lid, BigDecimal.ZERO).add(a.getMontantMad()));
                }
            }
        }

        // Calculs des lignes
        BigDecimal totalMarchandiseDevise = BigDecimal.ZERO;
        BigDecimal totalMarchandiseMad = BigDecimal.ZERO;
        BigDecimal totalCoutRevient = BigDecimal.ZERO;
        List<LigneCoutRevientDTO> lignesDto = new ArrayList<>();

        for (int i = 0; i < lignesVirtuelles.size(); i++) {
            LigneCommande l = lignesVirtuelles.get(i);
            LigneCommandeDTO sourceDto = req.getLignes().get(i);

            BigDecimal puDevise = l.getPrixUnitaire() != null ? l.getPrixUnitaire() : BigDecimal.ZERO;
            BigDecimal qte = BigDecimal.valueOf(l.getQuantiteCommandee());
            BigDecimal puMad = puDevise.multiply(tauxChange).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalLigneMad = puMad.multiply(qte).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalLigneDevise = puDevise.multiply(qte).setScale(2, RoundingMode.HALF_UP);

            BigDecimal fraisLigne = fraisParLigne.getOrDefault(l.getId(), BigDecimal.ZERO);
            BigDecimal coutRevientTotal = totalLigneMad.add(fraisLigne);
            BigDecimal coutRevientUnitaire = qte.compareTo(BigDecimal.ZERO) > 0
                    ? coutRevientTotal.divide(qte, 4, RoundingMode.HALF_UP)
                    : coutRevientTotal;
            BigDecimal coeffLigne = totalLigneMad.compareTo(BigDecimal.ZERO) > 0
                    ? coutRevientTotal.divide(totalLigneMad, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ONE;

            LigneCoutRevientDTO ldto = new LigneCoutRevientDTO();
            ldto.setLigneCommandeId(l.getId());
            ldto.setProduitId(sourceDto.getProduitId());
            ldto.setReference(sourceDto.getProduitReference());
            ldto.setDesignation(sourceDto.getDesignation() != null ? sourceDto.getDesignation() : sourceDto.getProduitDescription());
            ldto.setQuantite(l.getQuantiteCommandee());
            ldto.setDevise(req.getDevise());
            ldto.setPrixUnitaireDevise(puDevise);
            ldto.setMontantTotalDevise(totalLigneDevise);
            ldto.setPrixUnitaireMad(puMad);
            ldto.setMontantTotalMad(totalLigneMad);
            ldto.setPoidsTotalKg(l.getPoidsTotalKg());
            ldto.setVolumeTotalM3(l.getVolumeTotalM3());
            ldto.setCodeSh(l.getCodeSh());
            ldto.setTauxDroitDouane(l.getTauxDroitDouane());
            ldto.setTotalFraisAllouesMad(fraisLigne);
            ldto.setFraisParUniteMad(qte.compareTo(BigDecimal.ZERO) > 0 ? fraisLigne.divide(qte, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            ldto.setCoutRevientUnitaireMad(coutRevientUnitaire);
            ldto.setCoutRevientTotalMad(coutRevientTotal);
            ldto.setCoefficientLigne(coeffLigne);

            lignesDto.add(ldto);

            totalMarchandiseDevise = totalMarchandiseDevise.add(totalLigneDevise);
            totalMarchandiseMad = totalMarchandiseMad.add(totalLigneMad);
            totalCoutRevient = totalCoutRevient.add(coutRevientTotal);
        }

        // Assemblage du récap
        RecapCoutRevientDTO recap = new RecapCoutRevientDTO();
        recap.setTypeAchat(req.getTypeAchat());
        recap.setIncoterm(req.getIncoterm());
        recap.setDevise(req.getDevise());
        recap.setTauxChangeApplique(tauxChange);
        recap.setSourceTauxChange("SIMULATION");
        recap.setStatutCouts(StatutCouts.ESTIME);
        recap.setMontantMarchandiseDevise(totalMarchandiseDevise);
        recap.setMontantMarchandiseMad(totalMarchandiseMad);

        ventilerChargesDansRecap(recap, chargesVirtuelles);

        recap.setTotalChargesEstimeesMad(totalChargesEstimees);
        recap.setTotalChargesReellesMad(totalChargesReelles);
        recap.setTotalChargesIncorporablesMad(totalChargesIncorporables);
        recap.setCoutRevientTotalMad(totalCoutRevient);
        recap.setCoefficientApproche(totalMarchandiseMad.compareTo(BigDecimal.ZERO) > 0
                ? totalCoutRevient.divide(totalMarchandiseMad, 4, RoundingMode.HALF_UP)
                : BigDecimal.ONE);
        recap.setTvaImportRecuperableMad(tvaImport);
        recap.setChargesNonIncorporablesMad(chargesNonIncorporables);
        recap.setLignes(lignesDto);

        return recap;
    }

    private BigDecimal determinerTauxChange(Commande c) {
        if (c.getTauxChangeDouane() != null && c.getTauxChangeDouane().compareTo(BigDecimal.ZERO) > 0) {
            return c.getTauxChangeDouane();
        }
        if (c.getTauxChangeCommande() != null && c.getTauxChangeCommande().compareTo(BigDecimal.ZERO) > 0) {
            return c.getTauxChangeCommande();
        }
        return BigDecimal.ONE;
    }

    private BigDecimal calculerAcheminementVersCif(Incoterm incoterm, List<ChargeAchat> charges) {
        if (incoterm == null) return BigDecimal.ZERO;
        BigDecimal somme = BigDecimal.ZERO;
        for (ChargeAchat c : charges) {
            if (!Boolean.TRUE.equals(c.getIncorporable())) continue;
            TypeCharge tc = c.getTypeCharge();
            if (tc == TypeCharge.TRANSPORT_ORIGINE && !incoterm.isInclutTransportOrigine()) {
                somme = somme.add(c.getMontantRetenuMad());
            } else if (tc == TypeCharge.FRET_INTERNATIONAL && !incoterm.isInclutFret()) {
                somme = somme.add(c.getMontantRetenuMad());
            } else if (tc == TypeCharge.ASSURANCE && !incoterm.isInclutAssurance()) {
                somme = somme.add(c.getMontantRetenuMad());
            }
        }
        return somme;
    }

    private void ventilerChargesDansRecap(RecapCoutRevientDTO recap, List<ChargeAchat> charges) {
        for (ChargeAchat c : charges) {
            BigDecimal m = c.getMontantRetenuMad() != null ? c.getMontantRetenuMad() : BigDecimal.ZERO;
            switch (c.getTypeCharge()) {
                case TRANSPORT_ORIGINE:
                    recap.setTransportOrigineMad(recap.getTransportOrigineMad().add(m));
                    break;
                case FRET_INTERNATIONAL:
                    recap.setFretInternationalMad(recap.getFretInternationalMad().add(m));
                    break;
                case ASSURANCE:
                    recap.setAssuranceMad(recap.getAssuranceMad().add(m));
                    break;
                case DROITS_DOUANE:
                    recap.setDroitsDouaneMad(recap.getDroitsDouaneMad().add(m));
                    break;
                case TRANSITAIRE:
                    recap.setTransitaireMad(recap.getTransitaireMad().add(m));
                    break;
                case MAGASINAGE:
                    recap.setMagasinageMad(recap.getMagasinageMad().add(m));
                    break;
                case PENALITE:
                    recap.setPenalitesMad(recap.getPenalitesMad().add(m));
                    break;
                case TRANSPORT_LOCAL:
                    recap.setTransportLocalMad(recap.getTransportLocalMad().add(m));
                    break;
                case MANUTENTION:
                    recap.setManutentionMad(recap.getManutentionMad().add(m));
                    break;
                case MAIN_OEUVRE:
                    recap.setMainOeuvreMad(recap.getMainOeuvreMad().add(m));
                    break;
                case FRAIS_BANCAIRES:
                    recap.setFraisBancairesMad(recap.getFraisBancairesMad().add(m));
                    break;
                case AUTRE:
                default:
                    recap.setAutresChargesMad(recap.getAutresChargesMad().add(m));
                    break;
            }
        }

        recap.setTotalCfrMad(recap.getMontantMarchandiseMad().add(recap.getTransportOrigineMad()).add(recap.getFretInternationalMad()));
        recap.setValeurCifMad(recap.getTotalCfrMad().add(recap.getAssuranceMad()));
        recap.setTotalDouaneMad(recap.getDroitsDouaneMad().add(recap.getTransitaireMad()));
        recap.setTotalChargesLocalesMad(recap.getMagasinageMad()
                .add(recap.getPenalitesMad())
                .add(recap.getTransportLocalMad())
                .add(recap.getManutentionMad())
                .add(recap.getMainOeuvreMad())
                .add(recap.getFraisBancairesMad())
                .add(recap.getAutresChargesMad()));
    }

    private RecapCoutRevientDTO construireRecap(Commande c, List<LigneCommande> lignes, List<ChargeAchat> charges,
                                                BigDecimal tauxChange, String sourceTaux, BigDecimal tvaImport, BigDecimal nonIncorp) {
        RecapCoutRevientDTO recap = new RecapCoutRevientDTO();
        recap.setCommandeId(c.getId());
        recap.setNumeroCommande(c.getNumeroCommande());
        recap.setTypeAchat(c.getTypeAchat());
        recap.setIncoterm(c.getIncoterm());
        recap.setDevise(c.getDevise());
        recap.setTauxChangeApplique(tauxChange);
        recap.setSourceTauxChange(sourceTaux);
        recap.setStatutCouts(c.getStatutCouts());
        recap.setMontantMarchandiseDevise(c.getMontantMarchandiseDevise());
        recap.setMontantMarchandiseMad(c.getMontantMarchandiseMad());

        ventilerChargesDansRecap(recap, charges);

        recap.setTotalChargesEstimeesMad(c.getTotalChargesEstimeesMad());
        recap.setTotalChargesReellesMad(c.getTotalChargesReellesMad());
        recap.setTotalChargesIncorporablesMad(c.getTotalChargesMad());
        recap.setCoutRevientTotalMad(c.getCoutRevientTotalMad());
        recap.setCoefficientApproche(c.getCoefficientApproche());
        recap.setTvaImportRecuperableMad(tvaImport);
        recap.setChargesNonIncorporablesMad(nonIncorp);

        List<LigneCoutRevientDTO> ldtos = new ArrayList<>();
        for (LigneCommande l : lignes) {
            LigneCoutRevientDTO dto = new LigneCoutRevientDTO();
            dto.setLigneCommandeId(l.getId());
            if (l.getProduit() != null) {
                dto.setProduitId(l.getProduit().getId());
                dto.setReference(l.getProduit().getReference());
                dto.setDesignation(l.getProduit().getDesignation() != null ? l.getProduit().getDesignation() : l.getProduit().getDescription());
            }
            dto.setQuantite(l.getQuantiteCommandee());
            dto.setDevise(c.getDevise());
            dto.setPrixUnitaireDevise(l.getPrixUnitaire());
            dto.setMontantTotalDevise(l.getPrixUnitaire() != null && l.getQuantiteCommandee() != null
                    ? l.getPrixUnitaire().multiply(BigDecimal.valueOf(l.getQuantiteCommandee())) : BigDecimal.ZERO);
            dto.setPrixUnitaireMad(l.getPrixUnitaireMad());
            dto.setMontantTotalMad(l.getPrixUnitaireMad() != null && l.getQuantiteCommandee() != null
                    ? l.getPrixUnitaireMad().multiply(BigDecimal.valueOf(l.getQuantiteCommandee())) : BigDecimal.ZERO);
            dto.setPoidsTotalKg(l.getPoidsTotalKg());
            dto.setVolumeTotalM3(l.getVolumeTotalM3());
            dto.setCodeSh(l.getCodeSh());
            dto.setTauxDroitDouane(l.getTauxDroitDouane());
            dto.setTotalFraisAllouesMad(l.getFraisAllouesMad());
            dto.setFraisParUniteMad((l.getFraisAllouesMad() != null && l.getQuantiteCommandee() != null && l.getQuantiteCommandee() > 0)
                    ? l.getFraisAllouesMad().divide(BigDecimal.valueOf(l.getQuantiteCommandee()), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            dto.setCoutRevientUnitaireMad(l.getCoutRevientUnitaireMad());
            dto.setCoutRevientTotalMad(l.getCoutRevientTotalMad());
            if (dto.getMontantTotalMad() != null && dto.getMontantTotalMad().compareTo(BigDecimal.ZERO) > 0 && dto.getCoutRevientTotalMad() != null) {
                dto.setCoefficientLigne(dto.getCoutRevientTotalMad().divide(dto.getMontantTotalMad(), 4, RoundingMode.HALF_UP));
            } else {
                dto.setCoefficientLigne(BigDecimal.ONE);
            }
            ldtos.add(dto);
        }
        recap.setLignes(ldtos);

        return recap;
    }
}
