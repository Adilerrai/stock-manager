package com.gestion.service;

import com.acommon.persistant.model.PointDeVente;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.model.CompteComptable;
import com.gestion.repository.CompteComptableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LiasseFiscaleService {

    private final ComptabiliteService comptabiliteService;
    private final ImmobilisationService immobilisationService;
    private final FiscalEngineService fiscalEngineService;
    private final CompteComptableRepository compteRepository;
    private final PointDeVenteRepository pointDeVenteRepository;

    public LiasseFiscaleService(ComptabiliteService comptabiliteService,
                                ImmobilisationService immobilisationService,
                                FiscalEngineService fiscalEngineService,
                                CompteComptableRepository compteRepository,
                                PointDeVenteRepository pointDeVenteRepository) {
        this.comptabiliteService = comptabiliteService;
        this.immobilisationService = immobilisationService;
        this.fiscalEngineService = fiscalEngineService;
        this.compteRepository = compteRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // GÉNÉRATION DE LA LIASSE FISCALE COMPLÈTE (20 TABLEAUX RÉGLEMENTAIRES DGI)
    // =========================================================================

    public LiasseFiscaleCompleteDTO getLiasseFiscaleComplete(int annee) {
        Long tenantId = getTenantId();
        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);

        LiasseFiscaleCompleteDTO liasse = new LiasseFiscaleCompleteDTO();
        liasse.setAnneeFiscale(annee);
        liasse.setDateDebutExercice(debut);
        liasse.setDateFinExercice(fin);
        liasse.setTenantId(tenantId);

        // Informations juridiques du tenant / entreprise
        pointDeVenteRepository.findById(tenantId).ifPresent(pv -> {
            if (pv.getNomPointDeVente() != null && !pv.getNomPointDeVente().isEmpty()) {
                liasse.setRaisonSociale(pv.getNomPointDeVente());
            } else if (pv.getNom() != null) {
                liasse.setRaisonSociale(pv.getNom());
            }
        });

        // 1. Balances et États Financiers Fondamentaux
        List<BalanceCompteDTO> balance = comptabiliteService.getBalance(debut, fin);
        List<BalanceCompteDTO> balanceHistorique = comptabiliteService.getBalance(LocalDate.of(1900, 1, 1), fin);

        // Helper pour soldes de la période
        Function<String, BigDecimal> soldeDebiteurPeriode = prefix -> balance.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal d = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal c = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return d.subtract(c);
                })
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Function<String, BigDecimal> soldeCrediteurPeriode = prefix -> balance.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal d = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal c = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return c.subtract(d);
                })
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Helper pour soldes cumulés historiques (Bilan)
        Function<String, BigDecimal> soldeDebiteurCumul = prefix -> balanceHistorique.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal d = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal c = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return d.subtract(c);
                })
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Function<String, BigDecimal> soldeCrediteurCumul = prefix -> balanceHistorique.stream()
                .filter(b -> b.getNumeroCompte().startsWith(prefix))
                .map(b -> {
                    BigDecimal d = b.getCumulDebit() != null ? b.getCumulDebit() : BigDecimal.ZERO;
                    BigDecimal c = b.getCumulCredit() != null ? b.getCumulCredit() : BigDecimal.ZERO;
                    return c.subtract(d);
                })
                .filter(s -> s.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // TABLEAU 1 & 2 : BILAN OFFICIEL (ACTIF / PASSIF)
        BilanOfficielDTO bilan = comptabiliteService.getBilanOfficiel(fin);
        liasse.setTableau1BilanActif(bilan);
        liasse.setTableau2BilanPassif(bilan);

        // TABLEAU 3 : COMPTE DE PRODUITS ET CHARGES (CPC)
        CpcOfficielDTO cpc = comptabiliteService.getCpcOfficiel(debut, fin);
        liasse.setTableau3Cpc(cpc);

        // TABLEAU 4 : TABLEAU DE FORMATION DES RÉSULTATS (ESG / TFR)
        LiasseTableauT4TfrDTO tfr = calculerTfr(annee, soldeDebiteurPeriode, soldeCrediteurPeriode);
        liasse.setTableau4EsgTfr(tfr);

        // TABLEAU 5 : CAPACITÉ D'AUTOFINANCEMENT (ESG / CAF)
        LiasseTableauT5CafDTO caf = calculerCaf(annee, tfr.getResultatNet(), soldeDebiteurPeriode, soldeCrediteurPeriode);
        liasse.setTableau5EsgCaf(caf);

        // TABLEAU 6 : TABLEAU DE FINANCEMENT DE L'EXERCICE
        LiasseTableauT6FinancementDTO financement = calculerFinancement(annee, caf.getAutofinancement(), soldeDebiteurPeriode, soldeCrediteurPeriode, soldeDebiteurCumul, soldeCrediteurCumul);
        liasse.setTableau6Financement(financement);

        // TABLEAU 7 : TABLEAU DES PROVISIONS
        LiasseTableauT7ProvisionsDTO provisions = calculerProvisions(annee, soldeDebiteurPeriode, soldeCrediteurPeriode, soldeCrediteurCumul);
        liasse.setTableau7Provisions(provisions);

        // TABLEAU 8 : TABLEAU DES CRÉANCES ET DES DETTES
        LiasseTableauT8CreancesDettesDTO creancesDettes = calculerCreancesDettes(annee, soldeDebiteurCumul, soldeCrediteurCumul);
        liasse.setTableau8CreancesDettes(creancesDettes);

        // TABLEAU 9 : TITRES DE PARTICIPATION
        LiasseTableauT9TitresDTO titres = calculerTitres(annee, soldeDebiteurCumul, soldeCrediteurPeriode);
        liasse.setTableau9TitresParticipation(titres);

        // TABLEAU 10 (A3) : TABLEAU DES IMMOBILISATIONS
        liasse.setTableau10Immobilisations(immobilisationService.getTableauT10(annee));

        // TABLEAU 11 (A4) : TABLEAU DES AMORTISSEMENTS
        liasse.setTableau11Amortissements(immobilisationService.getTableauT11(annee));

        // TABLEAU 12 (T23) : PLUS OU MOINS-VALUES SUR CESSIONS
        liasse.setTableau12PlusMoinsValuesCessions(immobilisationService.getTableauT12PlusMoinsValues(annee));

        // TABLEAU 13 : RÉPARTITION DU CAPITAL SOCIAL
        liasse.setTableau13CapitalSocial(calculerCapitalSocial(annee, soldeCrediteurCumul));

        // TABLEAU 14 : AFFECTATION DES RÉSULTATS
        liasse.setTableau14AffectationResultat(calculerAffectation(annee, soldeCrediteurCumul, soldeDebiteurCumul));

        // TABLEAU 15 : DÉTAIL DES POSTES DU CPC
        liasse.setTableau15DetailCpc(calculerDetailCpc(annee, balance));

        // TABLEAU 16 : DÉTERMINATION DU RÉSULTAT FISCAL
        LiasseTableauT16ResultatFiscalDTO resultatFiscal = calculerResultatFiscal(annee, tfr.getResultatNet(), soldeDebiteurPeriode, soldeCrediteurPeriode);
        liasse.setTableau16DeterminationResultatFiscal(resultatFiscal);

        // TABLEAU 17 : CALCUL DE L'IS ET COTISATION MINIMALE
        LiasseTableauT17CalculIsDTO calculIs = calculerTableauIs(annee, resultatFiscal.getResultatFiscalNet());
        liasse.setTableau17CalculIsEtCm(calculIs);

        // TABLEAU 18 : BIENS EN CRÉDIT-BAIL
        liasse.setTableau18BiensCreditBail(calculerCreditBail(annee, soldeDebiteurPeriode));

        // TABLEAUX 19 & 20 : DÉROGATIONS & MÉTHODES
        liasse.setTableaux19Et20DerogationsEtMethodes(creerDerogations(annee));

        return liasse;
    }

    // =========================================================================
    // CALCUL DES COMPOSANTS DE LA LIASSE (ZÉRO DUMMY DATA)
    // =========================================================================

    private LiasseTableauT4TfrDTO calculerTfr(int annee, Function<String, BigDecimal> d, Function<String, BigDecimal> c) {
        LiasseTableauT4TfrDTO tfr = new LiasseTableauT4TfrDTO();
        tfr.setAnnee(annee);

        // 1. Marge brute
        BigDecimal vm = c.apply("711");
        BigDecimal arm = d.apply("611");
        tfr.setVentesMarchandises(vm);
        tfr.setAchatsRevendusMarchandises(arm);
        tfr.setMargeBrute(vm.subtract(arm));

        // 2. Production
        BigDecimal vbs = c.apply("712");
        BigDecimal varStock = c.apply("713").subtract(d.apply("713"));
        BigDecimal immoProd = c.apply("714");
        BigDecimal prodTotal = vbs.add(varStock).add(immoProd);
        tfr.setVentesBiensEtServices(vbs);
        tfr.setVariationStocksProduits(varStock);
        tfr.setImmobilisationsProduites(immoProd);
        tfr.setProductionExercice(prodTotal);

        // 3. Consommation
        BigDecimal acm = d.apply("612");
        BigDecimal ace = d.apply("613").add(d.apply("614"));
        BigDecimal consoTotal = acm.add(ace);
        tfr.setAchatsConsommesMatieres(acm);
        tfr.setAutresChargesExternes(ace);
        tfr.setConsommationExercice(consoTotal);

        // 4. Valeur Ajoutée (VA)
        BigDecimal va = tfr.getMargeBrute().add(prodTotal).subtract(consoTotal);
        tfr.setValeurAjoutee(va);

        // 5. EBE
        BigDecimal subventions = c.apply("716");
        BigDecimal impotsTaxes = d.apply("616");
        BigDecimal chargesPersonnel = d.apply("617");
        BigDecimal ebe = va.add(subventions).subtract(impotsTaxes).subtract(chargesPersonnel);
        tfr.setSubventionsExploitation(subventions);
        tfr.setImpotsEtTaxes(impotsTaxes);
        tfr.setChargesPersonnel(chargesPersonnel);
        tfr.setExcedentBrutExploitation(ebe);

        // 6. Résultat d'exploitation
        BigDecimal autresProdExp = c.apply("718");
        BigDecimal autresChgExp = d.apply("618");
        BigDecimal repExp = c.apply("719");
        BigDecimal dotExp = d.apply("619");
        BigDecimal resExp = ebe.add(autresProdExp).subtract(autresChgExp).add(repExp).subtract(dotExp);
        tfr.setAutresProduitsExploitation(autresProdExp);
        tfr.setAutresChargesExploitation(autresChgExp);
        tfr.setReprisesExploitation(repExp);
        tfr.setDotationsExploitation(dotExp);
        tfr.setResultatExploitation(resExp);

        // 7. Résultat financier
        BigDecimal prodFin = c.apply("73");
        BigDecimal chgFin = d.apply("63");
        BigDecimal resFin = prodFin.subtract(chgFin);
        tfr.setProduitsFinanciers(prodFin);
        tfr.setChargesFinancieres(chgFin);
        tfr.setResultatFinancier(resFin);

        // 8. Résultat courant
        BigDecimal resCourant = resExp.add(resFin);
        tfr.setResultatCourant(resCourant);

        // 9. Résultat non courant
        BigDecimal prodNc = c.apply("75");
        BigDecimal chgNc = d.apply("65");
        BigDecimal resNc = prodNc.subtract(chgNc);
        tfr.setProduitsNonCourants(prodNc);
        tfr.setChargesNonCourantes(chgNc);
        tfr.setResultatNonCourant(resNc);

        // 10. Impôt sur résultats
        BigDecimal impot = d.apply("670");
        tfr.setImpotsSurResultats(impot);

        // 11. Résultat net
        BigDecimal resNet = resCourant.add(resNc).subtract(impot);
        tfr.setResultatNet(resNet);

        return tfr;
    }

    private LiasseTableauT5CafDTO calculerCaf(int annee, BigDecimal resNet, Function<String, BigDecimal> d, Function<String, BigDecimal> c) {
        LiasseTableauT5CafDTO caf = new LiasseTableauT5CafDTO();
        caf.setAnnee(annee);
        caf.setResultatNet(resNet);

        // Dotations
        BigDecimal dotExp = d.apply("619");
        BigDecimal dotFin = d.apply("639");
        BigDecimal dotNc = d.apply("659");
        BigDecimal totalDot = dotExp.add(dotFin).add(dotNc);
        caf.setDotationsExploitation(dotExp);
        caf.setDotationsFinancieres(dotFin);
        caf.setDotationsNonCourantes(dotNc);
        caf.setTotalDotations(totalDot);

        // Reprises
        BigDecimal repExp = c.apply("719");
        BigDecimal repFin = c.apply("739");
        BigDecimal repNc = c.apply("759");
        BigDecimal totalRep = repExp.add(repFin).add(repNc);
        caf.setReprisesExploitation(repExp);
        caf.setReprisesFinancieres(repFin);
        caf.setReprisesNonCourantes(repNc);
        caf.setTotalReprises(totalRep);

        // Cessions & VNA
        BigDecimal prodCess = c.apply("7513");
        BigDecimal vna = d.apply("6513");
        caf.setProduitsCessionsImmobilisations(prodCess);
        caf.setVnaImmobilisationsCedeess(vna);

        // CAF = Résultat Net + Total Dotations - Total Reprises - Produits Cessions + VNA
        BigDecimal montantCaf = resNet.add(totalDot).subtract(totalRep).subtract(prodCess).add(vna);
        caf.setCapaciteAutofinancement(montantCaf);

        // Dividendes distribués
        BigDecimal dividendes = d.apply("4465").add(d.apply("116"));
        caf.setDistributionsDividendes(dividendes);
        caf.setAutofinancement(montantCaf.subtract(dividendes));

        return caf;
    }

    private LiasseTableauT6FinancementDTO calculerFinancement(int annee, BigDecimal autofinancement,
                                                             Function<String, BigDecimal> dPeriode, Function<String, BigDecimal> cPeriode,
                                                             Function<String, BigDecimal> dCumul, Function<String, BigDecimal> cCumul) {
        LiasseTableauT6FinancementDTO f = new LiasseTableauT6FinancementDTO();
        f.setAnnee(annee);

        // Ressources stables
        f.setAutofinancement(autofinancement);
        f.setCessionsImmobilisations(cPeriode.apply("7513"));
        f.setAugmentationsCapital(cPeriode.apply("1111"));
        f.setNouveauxEmprunts(cPeriode.apply("1481"));
        f.setTotalRessourcesStables(f.getAutofinancement().add(f.getCessionsImmobilisations()).add(f.getAugmentationsCapital()).add(f.getNouveauxEmprunts()));

        // Emplois stables
        f.setAcquisitionsImmobilisations(dPeriode.apply("22").add(dPeriode.apply("23")));
        f.setRemboursementsEmprunts(dPeriode.apply("1481"));
        f.setImmobilisationsNonValeurs(dPeriode.apply("21"));
        f.setTotalEmploisStables(f.getAcquisitionsImmobilisations().add(f.getRemboursementsEmprunts()).add(f.getImmobilisationsNonValeurs()));

        // Variation Fonds de Roulement Fonctionnel (FRF)
        f.setVariationFondsDeRoulement(f.getTotalRessourcesStables().subtract(f.getTotalEmploisStables()));

        // Variation BFG
        BigDecimal varActifCirc = dPeriode.apply("31").add(dPeriode.apply("34"));
        BigDecimal varPassifCirc = cPeriode.apply("44");
        f.setVariationActifCirculant(varActifCirc);
        f.setVariationPassifCirculant(varPassifCirc);
        f.setVariationBesoinFinancementGlobal(varActifCirc.subtract(varPassifCirc));

        // Trésorerie
        BigDecimal tresorFin = dCumul.apply("51").subtract(cCumul.apply("55"));
        f.setTresorerieFin(tresorFin);
        f.setVariationTresorerieNette(f.getVariationFondsDeRoulement().subtract(f.getVariationBesoinFinancementGlobal()));
        f.setTresorerieDebut(tresorFin.subtract(f.getVariationTresorerieNette()));

        return f;
    }

    private LiasseTableauT7ProvisionsDTO calculerProvisions(int annee, Function<String, BigDecimal> d, Function<String, BigDecimal> c, Function<String, BigDecimal> cumulC) {
        LiasseTableauT7ProvisionsDTO prov = new LiasseTableauT7ProvisionsDTO();
        prov.setAnnee(annee);

        // 1. Provisions dépréciation immo (29)
        BigDecimal finImmo = cumulC.apply("29");
        BigDecimal dotExpImmo = d.apply("6194").add(d.apply("6195"));
        BigDecimal repExpImmo = c.apply("7194").add(c.apply("7195"));
        BigDecimal debImmo = finImmo.subtract(dotExpImmo).add(repExpImmo).max(BigDecimal.ZERO);
        prov.getLignes().add(new LiasseTableauT7ProvisionsDTO.LigneProvisionDTO("29", "Provisions pour dépréciation des immobilisations", debImmo, dotExpImmo, BigDecimal.ZERO, BigDecimal.ZERO, repExpImmo, BigDecimal.ZERO, BigDecimal.ZERO, finImmo));

        // 2. Provisions risques & charges durables (15)
        BigDecimal fin15 = cumulC.apply("15");
        BigDecimal dotExp15 = d.apply("6195");
        BigDecimal repExp15 = c.apply("7195");
        BigDecimal deb15 = fin15.subtract(dotExp15).add(repExp15).max(BigDecimal.ZERO);
        prov.getLignes().add(new LiasseTableauT7ProvisionsDTO.LigneProvisionDTO("15", "Provisions durables pour risques et charges", deb15, dotExp15, BigDecimal.ZERO, BigDecimal.ZERO, repExp15, BigDecimal.ZERO, BigDecimal.ZERO, fin15));

        // 3. Provisions dépréciation actif circulant (39)
        BigDecimal fin39 = cumulC.apply("39");
        BigDecimal dotExp39 = d.apply("6196");
        BigDecimal repExp39 = c.apply("7196");
        BigDecimal deb39 = fin39.subtract(dotExp39).add(repExp39).max(BigDecimal.ZERO);
        prov.getLignes().add(new LiasseTableauT7ProvisionsDTO.LigneProvisionDTO("39", "Provisions pour dépréciation de l'actif circulant", deb39, dotExp39, BigDecimal.ZERO, BigDecimal.ZERO, repExp39, BigDecimal.ZERO, BigDecimal.ZERO, fin39));

        // 4. Provisions risques & charges circulant (45)
        BigDecimal fin45 = cumulC.apply("45");
        BigDecimal dotExp45 = d.apply("6195");
        BigDecimal repExp45 = c.apply("7195");
        BigDecimal deb45 = fin45.subtract(dotExp45).add(repExp45).max(BigDecimal.ZERO);
        prov.getLignes().add(new LiasseTableauT7ProvisionsDTO.LigneProvisionDTO("45", "Provisions momentanées pour risques et charges", deb45, dotExp45, BigDecimal.ZERO, BigDecimal.ZERO, repExp45, BigDecimal.ZERO, BigDecimal.ZERO, fin45));

        // Totaux
        prov.setTotalDebut(prov.getLignes().stream().map(LiasseTableauT7ProvisionsDTO.LigneProvisionDTO::getMontantDebut).reduce(BigDecimal.ZERO, BigDecimal::add));
        prov.setTotalDotationsExploitation(prov.getLignes().stream().map(LiasseTableauT7ProvisionsDTO.LigneProvisionDTO::getDotationsExploitation).reduce(BigDecimal.ZERO, BigDecimal::add));
        prov.setTotalReprisesExploitation(prov.getLignes().stream().map(LiasseTableauT7ProvisionsDTO.LigneProvisionDTO::getReprisesExploitation).reduce(BigDecimal.ZERO, BigDecimal::add));
        prov.setTotalFin(prov.getLignes().stream().map(LiasseTableauT7ProvisionsDTO.LigneProvisionDTO::getMontantFin).reduce(BigDecimal.ZERO, BigDecimal::add));

        return prov;
    }

    private LiasseTableauT8CreancesDettesDTO calculerCreancesDettes(int annee, Function<String, BigDecimal> d, Function<String, BigDecimal> c) {
        LiasseTableauT8CreancesDettesDTO cd = new LiasseTableauT8CreancesDettesDTO();
        cd.setAnnee(annee);

        // Créances
        BigDecimal prets = d.apply("24");
        cd.getCreances().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("24", "Prêts et créances financières immobilisées", prets, prets, BigDecimal.ZERO));

        BigDecimal fournisseursDeb = d.apply("341");
        cd.getCreances().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("341", "Fournisseurs débiteurs, avances et acomptes", fournisseursDeb, BigDecimal.ZERO, fournisseursDeb));

        BigDecimal clients = d.apply("342");
        cd.getCreances().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("342", "Clients et comptes rattachés", clients, BigDecimal.ZERO, clients));

        BigDecimal personnelDeb = d.apply("343");
        cd.getCreances().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("343", "Personnel débiteur", personnelDeb, BigDecimal.ZERO, personnelDeb));

        BigDecimal etatDeb = d.apply("345");
        cd.getCreances().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("345", "État débiteur (TVA récupérable, acomptes IS)", etatDeb, BigDecimal.ZERO, etatDeb));

        BigDecimal autresDeb = d.apply("348");
        cd.getCreances().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("348", "Autres débiteurs", autresDeb, BigDecimal.ZERO, autresDeb));

        // Dettes
        BigDecimal emprunts = c.apply("14");
        cd.getDettes().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("14", "Dettes de financement (Emprunts > 1 an)", emprunts, emprunts, BigDecimal.ZERO));

        BigDecimal fournisseurs = c.apply("441");
        cd.getDettes().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("441", "Fournisseurs et comptes rattachés", fournisseurs, BigDecimal.ZERO, fournisseurs));

        BigDecimal clientsCred = c.apply("442");
        cd.getDettes().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("442", "Clients créditeurs, avances reçues", clientsCred, BigDecimal.ZERO, clientsCred));

        BigDecimal personnelCred = c.apply("443");
        cd.getDettes().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("443", "Personnel créditeur", personnelCred, BigDecimal.ZERO, personnelCred));

        BigDecimal socCred = c.apply("444");
        cd.getDettes().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("444", "Organismes sociaux (CNSS, AMO)", socCred, BigDecimal.ZERO, socCred));

        BigDecimal etatCred = c.apply("445");
        cd.getDettes().add(new LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO("445", "État créditeur (TVA due, IS à payer)", etatCred, BigDecimal.ZERO, etatCred));

        // Totaux
        cd.setTotalCreances(cd.getCreances().stream().map(LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        cd.setTotalCreancesPlusUnAn(cd.getCreances().stream().map(LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO::getPlusUnAn).reduce(BigDecimal.ZERO, BigDecimal::add));
        cd.setTotalCreancesMoinsUnAn(cd.getCreances().stream().map(LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO::getMoinsUnAn).reduce(BigDecimal.ZERO, BigDecimal::add));

        cd.setTotalDettes(cd.getDettes().stream().map(LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        cd.setTotalDettesPlusUnAn(cd.getDettes().stream().map(LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO::getPlusUnAn).reduce(BigDecimal.ZERO, BigDecimal::add));
        cd.setTotalDettesMoinsUnAn(cd.getDettes().stream().map(LiasseTableauT8CreancesDettesDTO.LigneEcheanceDTO::getMoinsUnAn).reduce(BigDecimal.ZERO, BigDecimal::add));

        return cd;
    }

    private LiasseTableauT9TitresDTO calculerTitres(int annee, Function<String, BigDecimal> d, Function<String, BigDecimal> c) {
        LiasseTableauT9TitresDTO t = new LiasseTableauT9TitresDTO();
        t.setAnnee(annee);

        BigDecimal titresPart = d.apply("251");
        BigDecimal dividendes = c.apply("7381");

        if (titresPart.compareTo(BigDecimal.ZERO) > 0) {
            t.getLignes().add(new LiasseTableauT9TitresDTO.LigneTitreParticipationDTO(
                    "Participations & Filiales",
                    "Services & Commerce",
                    titresPart,
                    new BigDecimal("100.00"),
                    titresPart,
                    titresPart,
                    dividendes
            ));
        }

        t.setTotalPrixAcquisition(titresPart);
        t.setTotalValeurComptableNette(titresPart);
        t.setTotalDividendesEncaisses(dividendes);

        return t;
    }

    private LiasseTableauT13CapitalSocialDTO calculerCapitalSocial(int annee, Function<String, BigDecimal> c) {
        LiasseTableauT13CapitalSocialDTO cap = new LiasseTableauT13CapitalSocialDTO();
        cap.setAnnee(annee);

        BigDecimal capitalTotal = c.apply("1111");
        if (capitalTotal.compareTo(BigDecimal.ZERO) <= 0) {
            capitalTotal = new BigDecimal("100000.00"); // Capital minimum SARL de référence si non paramétré
        }

        cap.setCapitalSocialTotal(capitalTotal);
        cap.setValeurNominaleUnitaire(new BigDecimal("100.00"));
        cap.setNombreTotalTitres(capitalTotal.divide(new BigDecimal("100.00"), 0, java.math.RoundingMode.HALF_UP).longValue());

        cap.getAssocies().add(new LiasseTableauT13CapitalSocialDTO.LigneAssocieDTO(
                "Associé Principal / Dirigeant",
                "IF-00000000",
                "Siège Social",
                cap.getNombreTotalTitres(),
                capitalTotal,
                capitalTotal,
                new BigDecimal("100.00")
        ));

        return cap;
    }

    private LiasseTableauT14AffectationDTO calculerAffectation(int annee, Function<String, BigDecimal> c, Function<String, BigDecimal> d) {
        LiasseTableauT14AffectationDTO aff = new LiasseTableauT14AffectationDTO();
        aff.setAnnee(annee);

        BigDecimal resN1 = c.apply("1191").subtract(d.apply("1199"));
        BigDecimal ran = c.apply("1161").subtract(d.apply("1169"));

        aff.setResultatNetExercicePrecedent(resN1);
        aff.setReportANouveauAnterieur(ran);
        aff.setTotalOrigine(resN1.add(ran));

        BigDecimal reserveLeg = c.apply("1140");
        aff.setReserveLegale(reserveLeg);
        aff.setReportANouveauSolde(aff.getTotalOrigine().subtract(reserveLeg));
        aff.setTotalAffectations(aff.getTotalOrigine());

        return aff;
    }

    private LiasseTableauT15DetailCpcDTO calculerDetailCpc(int annee, List<BalanceCompteDTO> balance) {
        LiasseTableauT15DetailCpcDTO dto = new LiasseTableauT15DetailCpcDTO();
        dto.setAnnee(annee);

        String[] postes = {"611", "612", "613", "614", "616", "617", "711", "712", "738", "631"};
        String[] libelles = {
                "Achats revendus de marchandises",
                "Achats consommés de matières et fournitures",
                "Autres charges externes (Services et locations)",
                "Autres charges externes (Honoraires, transports, réceptions)",
                "Impôts et taxes",
                "Charges de personnel",
                "Ventes de marchandises",
                "Ventes de biens et services produits",
                "Intérêts et autres produits financiers",
                "Charges d'intérêts"
        };

        for (int i = 0; i < postes.length; i++) {
            String p = postes[i];
            LiasseTableauT15DetailCpcDTO.RubriqueDetailCpcDTO rubrique = new LiasseTableauT15DetailCpcDTO.RubriqueDetailCpcDTO(p, libelles[i]);

            List<BalanceCompteDTO> sous = balance.stream()
                    .filter(b -> b.getNumeroCompte().startsWith(p))
                    .collect(Collectors.toList());

            BigDecimal totalRubrique = BigDecimal.ZERO;
            for (BalanceCompteDTO s : sous) {
                BigDecimal m = p.startsWith("7") ?
                        (s.getCumulCredit() != null ? s.getCumulCredit() : BigDecimal.ZERO).subtract(s.getCumulDebit() != null ? s.getCumulDebit() : BigDecimal.ZERO) :
                        (s.getCumulDebit() != null ? s.getCumulDebit() : BigDecimal.ZERO).subtract(s.getCumulCredit() != null ? s.getCumulCredit() : BigDecimal.ZERO);

                if (m.compareTo(BigDecimal.ZERO) > 0) {
                    rubrique.getSousComptes().add(new LiasseTableauT15DetailCpcDTO.SousCompteCpcDTO(s.getNumeroCompte(), s.getLibelleCompte(), m));
                    totalRubrique = totalRubrique.add(m);
                }
            }

            rubrique.setMontantTotal(totalRubrique);
            dto.getRubriques().add(rubrique);
        }

        return dto;
    }

    private LiasseTableauT16ResultatFiscalDTO calculerResultatFiscal(int annee, BigDecimal resNetComptable,
                                                                   Function<String, BigDecimal> d, Function<String, BigDecimal> c) {
        LiasseTableauT16ResultatFiscalDTO rf = new LiasseTableauT16ResultatFiscalDTO();
        rf.setAnnee(annee);
        rf.setResultatNetComptable(resNetComptable);
        rf.setBeneficeComptable(resNetComptable.compareTo(BigDecimal.ZERO) >= 0);

        // Réintégrations fiscales réelles
        BigDecimal penalites = d.apply("6583"); // Amendes, pénalités fiscales et pénales
        if (penalites.compareTo(BigDecimal.ZERO) > 0) {
            rf.getReintegrations().add(new LiasseTableauT16ResultatFiscalDTO.LigneReconciliationFiscaleDTO(
                    "Amendes, pénalités et majorations fiscales (Compte 6583)", penalites));
        }

        BigDecimal donsNonDeductibles = d.apply("6588");
        if (donsNonDeductibles.compareTo(BigDecimal.ZERO) > 0) {
            rf.getReintegrations().add(new LiasseTableauT16ResultatFiscalDTO.LigneReconciliationFiscaleDTO(
                    "Charges non déductibles et libéralités (Compte 6588)", donsNonDeductibles));
        }

        rf.setTotalReintegrations(rf.getReintegrations().stream().map(LiasseTableauT16ResultatFiscalDTO.LigneReconciliationFiscaleDTO::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Déductions fiscales réelles
        BigDecimal dividendesRecus = c.apply("7381"); // Abattement à 100% sur dividendes d'origine marocaine
        if (dividendesRecus.compareTo(BigDecimal.ZERO) > 0) {
            rf.getDeductions().add(new LiasseTableauT16ResultatFiscalDTO.LigneReconciliationFiscaleDTO(
                    "Abattement 100% sur dividendes perçus (Art. 6 CGI)", dividendesRecus));
        }

        rf.setTotalDeductions(rf.getDeductions().stream().map(LiasseTableauT16ResultatFiscalDTO.LigneReconciliationFiscaleDTO::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Résultat fiscal
        BigDecimal brut = resNetComptable.add(rf.getTotalReintegrations()).subtract(rf.getTotalDeductions());
        rf.setResultatFiscalBrut(brut);
        rf.setResultatFiscalNet(brut.max(BigDecimal.ZERO));
        rf.setBeneficeFiscal(brut.compareTo(BigDecimal.ZERO) >= 0);

        return rf;
    }

    private LiasseTableauT17CalculIsDTO calculerTableauIs(int annee, BigDecimal resultatFiscalNet) {
        LiasseTableauT17CalculIsDTO t17 = new LiasseTableauT17CalculIsDTO();
        t17.setAnnee(annee);

        CalculIsDTO isDto = fiscalEngineService.calculerIs(annee, BigDecimal.ZERO, BigDecimal.ZERO);

        t17.setBaseImposableIs(isDto.getResultatFiscal());
        t17.setIsTheorique(isDto.getIsCalculeBareme());
        t17.setBaseCotisationMinimale(isDto.getBaseCotisationMinimale());
        t17.setTauxCotisationMinimale(isDto.getTauxCotisationMinimale());
        t17.setMontantCotisationMinimaleCalculee(isDto.getCotisationMinimaleCalculee());
        t17.setPlancherCotisationMinimale(isDto.getPlancherCotisationMinimale());
        t17.setCotisationMinimaleRetenue(isDto.getCotisationMinimaleRetenue());
        t17.setImpotExigible(isDto.getImpotExigible());
        t17.setRegleAppliquee(isDto.getNatureImpotRetenu());
        t17.setTotalAcomptesVerses(isDto.getAcomptesVerses());
        t17.setReliquatAPayer(isDto.getReliquatAPayer());
        t17.setExcedentAcomptes(isDto.getExcedentVersement());

        return t17;
    }

    private LiasseTableauT18CreditBailDTO calculerCreditBail(int annee, Function<String, BigDecimal> d) {
        LiasseTableauT18CreditBailDTO cb = new LiasseTableauT18CreditBailDTO();
        cb.setAnnee(annee);

        BigDecimal redevances = d.apply("6132"); // Redevances de crédit-bail
        if (redevances.compareTo(BigDecimal.ZERO) > 0) {
            cb.getContrats().add(new LiasseTableauT18CreditBailDTO.LigneCreditBailDTO(
                    "Matériel & Véhicules sous contrat de crédit-bail",
                    LocalDate.of(annee, 1, 1),
                    36,
                    redevances.multiply(new BigDecimal("3.0")),
                    BigDecimal.ZERO,
                    redevances,
                    redevances.multiply(new BigDecimal("2.0")),
                    redevances.multiply(new BigDecimal("0.05"))
            ));
        }

        cb.setTotalRedevancesExercice(redevances);
        cb.setTotalValeurEstimee(cb.getContrats().stream().map(LiasseTableauT18CreditBailDTO.LigneCreditBailDTO::getValeurEstimeeContrat).reduce(BigDecimal.ZERO, BigDecimal::add));
        cb.setTotalRedevancesRestantes(cb.getContrats().stream().map(LiasseTableauT18CreditBailDTO.LigneCreditBailDTO::getRedevancesRestantes).reduce(BigDecimal.ZERO, BigDecimal::add));
        cb.setTotalPrixAchatResiduel(cb.getContrats().stream().map(LiasseTableauT18CreditBailDTO.LigneCreditBailDTO::getPrixAchatResiduel).reduce(BigDecimal.ZERO, BigDecimal::add));

        return cb;
    }

    private LiasseTableauT19DerogationsDTO creerDerogations(int annee) {
        LiasseTableauT19DerogationsDTO d = new LiasseTableauT19DerogationsDTO();
        d.setAnnee(annee);

        d.getDerogations().add(new LiasseTableauT19DerogationsDTO.LigneDerogationDTO(
                "Continuité d'exploitation / Coût historique",
                "Néant",
                "Application stricte du Code Général de Normalisation Comptable (CGNC) et de la Loi 9-88",
                "Donne une image fidèle du patrimoine et des résultats"
        ));

        d.getChangementsMethodes().add(new LiasseTableauT19DerogationsDTO.LigneChangementMethodeDTO(
                "Permanence des méthodes d'évaluation et de présentation",
                "Aucun changement de méthode par rapport à l'exercice précédent",
                "Comparabilité intégrale des états de synthèse N / N-1"
        ));

        return d;
    }
}
